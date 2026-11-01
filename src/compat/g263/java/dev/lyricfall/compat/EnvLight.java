package dev.lyricfall.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

import java.lang.reflect.Method;

/** How dark it is around the player: 0 bright daylight, 1 a dark cave or night without torches. */
public final class EnvLight {
    private EnvLight() {}

    private static Method skyDarken;
    private static boolean resolved;

    public static float darkness() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return 0f;
        try {
            BlockPos p = BlockPos.containing(mc.player.getEyePosition());
            int block = mc.level.getBrightness(LightLayer.BLOCK, p);
            int sky = mc.level.getBrightness(LightLayer.SKY, p);
            float lit = Math.max(block, sky * daylight(mc.level)) / 15f;
            return Math.max(0f, Math.min(1f, 1f - lit));
        } catch (Throwable e) {
            return 0f;
        }
    }

    /** 1 at noon, about 0.2 at midnight. The method moved around between versions, so it is looked up once. */
    private static float daylight(Level level) {
        try {
            if (!resolved) {
                resolved = true;
                for (Method m : level.getClass().getMethods()) {
                    if (m.getParameterCount() == 0 && m.getReturnType() == int.class && m.getName().equals("getSkyDarken")) {
                        skyDarken = m;
                        break;
                    }
                }
            }
            if (skyDarken == null) return 1f;
            int d = (Integer) skyDarken.invoke(level);
            return 1f - Math.max(0, Math.min(11, d)) / 11f * 0.8f;
        } catch (Throwable e) {
            return 1f;
        }
    }
}
