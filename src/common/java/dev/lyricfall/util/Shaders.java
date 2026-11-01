package dev.lyricfall.util;

import java.lang.reflect.Method;

/** Whether an Iris shader pack is running (Complementary, BSL, ...). Uses Iris' public API by reflection. */
public final class Shaders {
    private Shaders() {}

    private static boolean resolved;
    private static Object api;
    private static Method inUse;

    public static boolean active() {
        try {
            if (!resolved) {
                resolved = true;
                Class<?> c = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                api = c.getMethod("getInstance").invoke(null);
                inUse = api.getClass().getMethod("isShaderPackInUse");
                inUse.setAccessible(true);
            }
            return inUse != null && (Boolean) inUse.invoke(api);
        } catch (Throwable e) {
            inUse = null;
            return false;
        }
    }

    public static boolean irisInstalled() {
        try {
            Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}
