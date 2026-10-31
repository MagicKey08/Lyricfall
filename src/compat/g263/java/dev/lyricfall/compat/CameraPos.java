package dev.lyricfall.compat;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

import java.lang.reflect.Method;

public final class CameraPos {
    private CameraPos() {}

    private static Method cameraMethod;
    private static Method rotationMethod;
    private static boolean resolved, rotationResolved;

    /** The main camera; method names changed between versions, so it is looked up by type. */
    private static Camera camera() {
        GameRenderer renderer = Minecraft.getInstance().gameRenderer;
        if (renderer == null) return null;
        try {
            if (!resolved) {
                resolved = true;
                for (Method m : GameRenderer.class.getMethods()) {
                    if (m.getParameterCount() == 0 && Camera.class.isAssignableFrom(m.getReturnType())
                            && (m.getName().equals("getMainCamera") || m.getName().equals("mainCamera"))) {
                        cameraMethod = m;
                        break;
                    }
                }
                if (cameraMethod == null) {
                    for (Method m : GameRenderer.class.getMethods()) {
                        if (m.getParameterCount() == 0 && Camera.class.isAssignableFrom(m.getReturnType())) {
                            cameraMethod = m;
                            break;
                        }
                    }
                }
            }
            if (cameraMethod != null) return (Camera) cameraMethod.invoke(renderer);
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** Camera position of the main view; null if it cannot be found. */
    public static Vec3 get() {
        try {
            Camera c = camera();
            return c == null ? null : c.position();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Camera orientation including roll. Mods like Do a Barrel Roll put their roll into this quaternion,
     * so text drawn with it stays upright on screen while the world spins around it. null if unavailable.
     */
    public static Quaternionf rotation() {
        try {
            Camera c = camera();
            if (c == null) return null;
            if (!rotationResolved) {
                rotationResolved = true;
                for (Method m : Camera.class.getMethods()) {
                    if (m.getParameterCount() == 0 && Quaternionfc.class.isAssignableFrom(m.getReturnType())
                            && (m.getName().equals("rotation") || m.getName().equals("getRotation"))) {
                        rotationMethod = m;
                        break;
                    }
                }
                if (rotationMethod == null) {
                    for (Method m : Camera.class.getMethods()) {
                        if (m.getParameterCount() == 0 && Quaternionfc.class.isAssignableFrom(m.getReturnType())) {
                            rotationMethod = m;
                            break;
                        }
                    }
                }
            }
            if (rotationMethod == null) return null;
            Object q = rotationMethod.invoke(c);
            return q instanceof Quaternionfc qc ? new Quaternionf(qc) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
