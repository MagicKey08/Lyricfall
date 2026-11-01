package dev.lyricfall.fabric;

import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehaviorManager;
import dev.lyricfall.LyricfallMod;
import dev.lyricfall.hud.LyricLight;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * LambDynamicLights integration: one light source that sits on the word being sung. Only loaded when
 * LambDynamicLights is installed (it calls this entrypoint), so the mod works fine without it.
 */
public final class LyricfallLights implements DynamicLightsInitializer {
    private static DynamicLightBehaviorManager manager;
    private static final WordLight LIGHT = new WordLight();
    private static Object lastLevel;
    private static int readdIn = -1;

    @Override
    public void onInitializeDynamicLights(DynamicLightsContext context) {
        manager = context.dynamicLightBehaviorManager();
        LyricfallMod.LOGGER.info("LambDynamicLights found: sung words will light up their surroundings");
    }

    /** LambDynamicLights drops every source when the world changes, so ours is added again shortly after. */
    static void tick() {
        if (manager == null) return;
        Object level = Minecraft.getInstance().level;
        if (level != lastLevel) {
            lastLevel = level;
            readdIn = level == null ? -1 : 20;
        }
        if (readdIn > 0 && --readdIn == 0) {
            try {
                manager.remove(LIGHT);
                manager.add(LIGHT);
            } catch (Throwable e) {
                LyricfallMod.LOGGER.warn("LambDynamicLights integration failed: {}", e.toString());
                manager = null;
            }
        }
    }

    private static final class WordLight implements DynamicLightBehavior {
        private int lastX = Integer.MIN_VALUE, lastY, lastZ, lastLevel = -1;

        @Override
        public double lightAtPos(BlockPos pos, double falloffRatio) {
            int l = LyricLight.level();
            if (l <= 0) return 0;
            double dx = pos.getX() + 0.5 - LyricLight.x();
            double dy = pos.getY() + 0.5 - LyricLight.y();
            double dz = pos.getZ() + 0.5 - LyricLight.z();
            return Math.max(0.0, l - Math.sqrt(dx * dx + dy * dy + dz * dz) * falloffRatio);
        }

        @Override
        public BoundingBox getBoundingBox() {
            int x = (int) Math.floor(LyricLight.x()), y = (int) Math.floor(LyricLight.y()), z = (int) Math.floor(LyricLight.z());
            return new BoundingBox(x, y, z, x + 1, y + 1, z + 1);
        }

        @Override
        public boolean hasChanged() {
            int x = (int) Math.floor(LyricLight.x()), y = (int) Math.floor(LyricLight.y()), z = (int) Math.floor(LyricLight.z());
            int l = LyricLight.level();
            boolean changed = x != lastX || y != lastY || z != lastZ || l != lastLevel;
            if (l == 0 && lastLevel == 0) changed = false;
            lastX = x;
            lastY = y;
            lastZ = z;
            lastLevel = l;
            return changed;
        }
    }
}
