package dev.lyricfall.media;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

/** Turns cover bytes into a small ARGB pixel grid. Java's ImageIO first, Minecraft's own decoder as fallback. */
final class CoverDecoder {
    private CoverDecoder() {}

    static int[] decode(byte[] bytes, int size) {
        try {
            BufferedImage img = javax.imageio.ImageIO.read(new ByteArrayInputStream(bytes));
            if (img != null) return sample(img, size);
        } catch (Throwable ignored) {
            // no java.desktop in this runtime, or an odd format
        }
        try {
            return dev.lyricfall.compat.NativeDecode.decode(bytes, size);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int[] sample(BufferedImage img, int size) {
        int w = img.getWidth(), h = img.getHeight();
        if (w <= 0 || h <= 0) return null;
        int[] out = new int[size * size];
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++)
                out[y * size + x] = img.getRGB(Math.min(w - 1, x * w / size), Math.min(h - 1, y * h / size));
        return out;
    }

}
