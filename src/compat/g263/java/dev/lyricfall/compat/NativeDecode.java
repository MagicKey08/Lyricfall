package dev.lyricfall.compat;

import com.mojang.blaze3d.platform.NativeImage;

/** Minecraft's built-in image decoder (stb), used when Java's ImageIO is not available. */
public final class NativeDecode {
    private NativeDecode() {}

    public static int[] decode(byte[] bytes, int size) throws Exception {
        try (NativeImage img = NativeImage.read(bytes)) {
            int w = img.getWidth(), h = img.getHeight();
            if (w <= 0 || h <= 0) return null;
            int[] out = new int[size * size];
            for (int y = 0; y < size; y++)
                for (int x = 0; x < size; x++)
                    out[y * size + x] = img.getPixel(Math.min(w - 1, x * w / size), Math.min(h - 1, y * h / size));
            return out;
        }
    }
}
