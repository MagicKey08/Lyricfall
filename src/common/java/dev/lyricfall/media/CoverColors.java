package dev.lyricfall.media;

import dev.lyricfall.LyricfallMod;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Colors taken from the album cover, like Apple Music and Spotify do. The cover is sampled, pixels are grouped by
 * hue (weighted by how colorful and bright they are), the strongest hue becomes the main color and a clearly
 * different one the second color. Both are then pushed into a lightness range that stays readable over the sky,
 * caves, snow and the nether. Black and white covers get a cool silver palette instead of muddy grey.
 *
 * Colors fade over about a second when the song changes, so the text never jumps.
 */
public final class CoverColors {
    private CoverColors() {}

    /** Main and second color (RGB), plus whether they really came from a cover. */
    public record Palette(int primary, int secondary, boolean fromCover) {}

    private static final Palette MONO = new Palette(0xF2F4FF, 0xB9C3E6, true);
    private static final int BINS = 24;
    private static final long FADE_NS = 1_100_000_000L;

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "lyricfall-cover");
        t.setDaemon(true);
        return t;
    });

    private static volatile Palette target;
    private static volatile int lastHash;
    private static Palette fading;
    private static int fromP, fromS;
    private static long changedAt;

    /** Called with the raw cover bytes (JPEG or PNG) whenever Windows reports a new cover. */
    public static void submit(byte[] image) {
        if (image == null || image.length == 0) return;
        int hash = java.util.Arrays.hashCode(image);
        if (hash == lastHash) return;
        lastHash = hash;
        WORKER.execute(() -> {
            try {
                int[] argb = CoverDecoder.decode(image, 64);
                if (argb == null) return;
                target = extract(argb);
            } catch (Throwable e) {
                LyricfallMod.LOGGER.warn("Cover colors failed: {}", e.toString());
            }
        });
    }

    /** Current palette with a smooth fade, or null while no cover was ever seen. Render thread only. */
    public static Palette current() {
        Palette t = target;
        if (t == null) return null;
        long now = System.nanoTime();
        if (fading == null) {
            fading = t;
            fromP = t.primary();
            fromS = t.secondary();
            changedAt = now - FADE_NS;
        } else if (t != fading) {
            Palette cur = blend(now);
            fromP = cur.primary();
            fromS = cur.secondary();
            fading = t;
            changedAt = now;
        }
        return blend(now);
    }

    private static Palette blend(long now) {
        float k = Math.min(1f, (now - changedAt) / (float) FADE_NS);
        k = k * k * (3f - 2f * k);
        return new Palette(lerp(fromP, fading.primary(), k), lerp(fromS, fading.secondary(), k), fading.fromCover());
    }

    static Palette extract(int[] argb) {
        double[] w = new double[BINS], r = new double[BINS], g = new double[BINS], b = new double[BINS];
        double total = 0, colorful = 0;
        for (int p : argb) {
            if ((p >>> 24) < 128) continue;
            int pr = (p >> 16) & 0xFF, pg = (p >> 8) & 0xFF, pb = p & 0xFF;
            float[] hsv = hsv(pr, pg, pb);
            total++;
            if (hsv[1] < 0.22f || hsv[2] < 0.2f) continue;
            double weight = hsv[1] * hsv[1] * (0.35 + hsv[2]);
            int bin = Math.min(BINS - 1, (int) (hsv[0] * BINS));
            w[bin] += weight;
            r[bin] += pr * weight;
            g[bin] += pg * weight;
            b[bin] += pb * weight;
            colorful += weight;
        }
        if (total == 0 || colorful / total < 0.035) return MONO;

        int best = -1;
        double bestScore = 0;
        for (int i = 0; i < BINS; i++) {
            double score = w[i] + 0.5 * (w[(i + BINS - 1) % BINS] + w[(i + 1) % BINS]);
            if (score > bestScore) { bestScore = score; best = i; }
        }
        int second = -1;
        double secondScore = 0;
        for (int i = 0; i < BINS; i++) {
            int dist = Math.min(Math.abs(i - best), BINS - Math.abs(i - best));
            if (dist < 3) continue;
            double score = w[i] + 0.5 * (w[(i + BINS - 1) % BINS] + w[(i + 1) % BINS]);
            if (score > secondScore) { secondScore = score; second = i; }
        }

        int primary = readable(mean(best, w, r, g, b), 0.70f);
        int sec;
        if (second >= 0 && secondScore >= bestScore * 0.12) {
            sec = readable(mean(second, w, r, g, b), 0.74f);
        } else {
            // one color covers: an analogous neighbour hue looks calmer than a random complement
            float[] hsl = hsl(primary);
            sec = fromHsl((hsl[0] + 0.09f) % 1f, Math.max(0.45f, hsl[1] * 0.85f), 0.78f);
        }
        return new Palette(primary, sec, true);
    }

    private static int mean(int bin, double[] w, double[] r, double[] g, double[] b) {
        double ww = 0, rr = 0, gg = 0, bb = 0;
        for (int d = -1; d <= 1; d++) {
            int i = (bin + d + BINS) % BINS;
            double k = d == 0 ? 1.0 : 0.5;
            ww += w[i] * k; rr += r[i] * k; gg += g[i] * k; bb += b[i] * k;
        }
        if (ww <= 0) return 0xFFFFFF;
        return ((int) Math.round(rr / ww) << 16) | ((int) Math.round(gg / ww) << 8) | (int) Math.round(bb / ww);
    }

    /** Pushes a color into a lightness and saturation range that reads well on any Minecraft background. */
    static int readable(int rgb, float lightness) {
        float[] hsl = hsl(rgb);
        float s = Math.max(0.5f, Math.min(0.9f, hsl[1] * 1.1f));
        // blue and purple look darker than yellow at the same lightness, give them a bit more
        float hueBoost = hsl[0] > 0.55f && hsl[0] < 0.8f ? 0.05f : 0f;
        float l = Math.max(lightness - 0.02f, Math.min(0.82f, lightness + hueBoost));
        return fromHsl(hsl[0], s, l);
    }

    private static float[] hsv(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf)), min = Math.min(rf, Math.min(gf, bf));
        float d = max - min, h = 0;
        if (d > 0) {
            if (max == rf) h = ((gf - bf) / d) % 6f;
            else if (max == gf) h = (bf - rf) / d + 2f;
            else h = (rf - gf) / d + 4f;
            h /= 6f;
            if (h < 0) h += 1f;
        }
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }

    static float[] hsl(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float l = (max + min) / 2f, h = 0, s = 0, d = max - min;
        if (d > 0) {
            s = l > 0.5f ? d / (2f - max - min) : d / (max + min);
            if (max == r) h = ((g - b) / d + (g < b ? 6f : 0f)) / 6f;
            else if (max == g) h = ((b - r) / d + 2f) / 6f;
            else h = ((r - g) / d + 4f) / 6f;
        }
        return new float[]{h, s, l};
    }

    static int fromHsl(float h, float s, float l) {
        float q = l < 0.5f ? l * (1 + s) : l + s - l * s, p = 2 * l - q;
        int r = Math.round(hue(p, q, h + 1f / 3f) * 255f);
        int g = Math.round(hue(p, q, h) * 255f);
        int b = Math.round(hue(p, q, h - 1f / 3f) * 255f);
        return (r << 16) | (g << 8) | b;
    }

    private static float hue(float p, float q, float t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1f / 6f) return p + (q - p) * 6 * t;
        if (t < 0.5f) return q;
        if (t < 2f / 3f) return p + (q - p) * (2f / 3f - t) * 6;
        return p;
    }

    private static int lerp(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }
}
