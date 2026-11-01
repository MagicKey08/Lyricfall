package dev.lyricfall.compat;

import dev.lyricfall.LyricfallMod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

import java.lang.reflect.Method;

/**
 * The surroundings of the text: block and sky light at a point, how bright the sky is right now, the haze color of
 * the dimension and whether the camera is under water. Used to light and fog the lyrics like a part of the world.
 */
public final class EnvLight {
    private EnvLight() {}

    /** Full brightness in Minecraft's packed light format (block 15, sky 15). */
    public static final int FULL = 0xF000F0;

    private static Method dayMethod;
    private static int dayKind;      // 1 sky darken (int), 2 sky darken with partial tick, 3 day time (long)
    private static boolean resolved;

    /** How dark it is around the player: 0 bright daylight, 1 a dark cave or night without torches. */
    public static float darkness() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return 0f;
        try {
            return 1f - brightness(packedAt(mc.player.getEyePosition().x, mc.player.getEyePosition().y, mc.player.getEyePosition().z));
        } catch (Throwable e) {
            return 0f;
        }
    }

    /** Packed light (block << 4 | sky << 20) at a world position, like signs and entities use it. */
    public static int packedAt(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return FULL;
        try {
            BlockPos p = BlockPos.containing(x, y, z);
            int block = mc.level.getBrightness(LightLayer.BLOCK, p);
            int sky = mc.level.getBrightness(LightLayer.SKY, p);
            return (block & 15) << 4 | (sky & 15) << 20;
        } catch (Throwable e) {
            return FULL;
        }
    }

    /** 0..1 how lit a packed light value really looks right now (sky light counts less at night). */
    public static float brightness(int packed) {
        int block = (packed >> 4) & 15, sky = (packed >> 20) & 15;
        return Math.max(block, sky * daylight()) / 15f;
    }

    /** 1 at noon, about 0.2 at midnight. Looked up by reflection because time handling moved around in 26.x. */
    public static float daylight() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return 1f;
        try {
            if (!resolved) {
                resolved = true;
                for (Method m : level.getClass().getMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (m.getName().equals("getSkyDarken") && p.length == 0 && m.getReturnType() == int.class) { dayMethod = m; dayKind = 1; break; }
                }
                if (dayMethod == null) for (Method m : level.getClass().getMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (m.getName().equals("getSkyDarken") && p.length == 1 && p[0] == float.class) { dayMethod = m; dayKind = 2; break; }
                }
                if (dayMethod == null) for (Method m : level.getClass().getMethods()) {
                    if ((m.getName().equals("getDayTime") || m.getName().equals("dayTime")) && m.getParameterCount() == 0
                            && m.getReturnType() == long.class) { dayMethod = m; dayKind = 3; break; }
                }
                LyricfallMod.LOGGER.info("Daylight source: {}", dayMethod == null ? "none" : dayMethod.getName());
            }
            if (dayMethod == null) return 1f;
            return switch (dayKind) {
                case 1 -> 1f - Math.max(0, Math.min(11, (Integer) dayMethod.invoke(level))) / 11f * 0.8f;
                case 2 -> {
                    float v = ((Number) dayMethod.invoke(level, 1.0f)).floatValue();
                    // older form returns 0..1 brightness, newer returns darkness; both end up in range
                    yield v <= 1f ? Math.max(0.2f, v) : 1f - Math.min(11f, v) / 11f * 0.8f;
                }
                default -> {
                    double t = ((Long) dayMethod.invoke(level) % 24000L) / 24000.0;
                    double sun = Math.cos((t - 0.25) * Math.PI * 2.0) * 2.0 + 0.5;
                    yield (float) (0.2 + 0.8 * Math.max(0.0, Math.min(1.0, sun)));
                }
            };
        } catch (Throwable e) {
            dayMethod = null;
            return 1f;
        }
    }

    /** Color distant text fades towards: blue-grey by day, deep blue at night, red in the nether, purple in the end. */
    public static int hazeColor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0xBFD2EA;
        try {
            if (mc.level.dimension() == Level.NETHER) return 0x5A2216;
            if (mc.level.dimension() == Level.END) return 0x251A33;
        } catch (Throwable ignored) {
        }
        float d = daylight();
        float k = Math.max(0f, Math.min(1f, (d - 0.2f) / 0.8f));
        return lerp(0x121826, 0xBFD2EA, k);
    }

    /** WATER, LAVA, POWDER_SNOW or NONE: what the camera is inside. */
    public static String fluidAtCamera() {
        try {
            var cam = CameraPos.camera();
            return cam == null ? "NONE" : String.valueOf(cam.getFluidInCamera());
        } catch (Throwable e) {
            return "NONE";
        }
    }

    private static int lerp(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return r << 16 | g << 8 | bl;
    }
}
