package dev.lyricfall.hud;

/**
 * Where the sung words are and how bright they should shine. Written by the renderer every frame, read by the
 * LambDynamicLights integration (possibly from another thread), so everything is volatile and plain.
 */
public final class LyricLight {
    private LyricLight() {}

    private static volatile double x, y, z;
    private static volatile int level;
    private static float smooth;

    public static double x() { return x; }
    public static double y() { return y; }
    public static double z() { return z; }
    public static int level() { return level; }

    /** pos: world position of the word being sung right now, or NaN when nothing is sung. */
    static void frame(double px, double py, double pz, int target, float dt) {
        boolean singing = !Double.isNaN(px);
        if (singing) {
            x = px;
            y = py;
            z = pz;
        }
        float want = singing ? target : 0f;
        // lights up quickly with the word, fades out over about a second in pauses
        float rate = want > smooth ? 9f : 1.6f;
        smooth += (want - smooth) * (1f - (float) Math.exp(-dt * rate));
        level = Math.round(smooth);
    }

    static void off() {
        smooth = 0f;
        level = 0;
    }
}
