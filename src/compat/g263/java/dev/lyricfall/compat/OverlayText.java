package dev.lyricfall.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.lang.reflect.Method;

/**
 * Draws the world lyrics on the HUD, projected with the camera so they sit exactly where the 3D text would be.
 * The HUD is drawn after the shader pack, so the text stays crisp: no shader lighting, fog, bloom, TAA smearing
 * or darkening at night. Only depth is lost, the text is always visible (like "visible through blocks").
 */
public final class OverlayText extends dev.lyricfall.compat.WorldText {
    private final Canvas canvas;
    private final Quaternionf toView;
    private final float tanY, tanX, w, h;
    private final boolean ok;

    private static Method fovMethod;
    private static boolean fovResolved;

    public OverlayText(Canvas canvas) {
        this.canvas = canvas;
        Quaternionf rot = CameraPos.rotation();
        this.toView = rot == null ? null : new Quaternionf(rot).conjugate();
        this.w = canvas.screenWidth();
        this.h = canvas.screenHeight();
        float fov = (float) Math.toRadians(fovDegrees());
        this.tanY = (float) Math.tan(fov / 2.0);
        this.tanX = tanY * (h > 0 ? w / h : 1f);
        this.ok = toView != null && w > 0 && h > 0;
    }

    @Override public boolean overlay() { return true; }

    /** The real field of view incl. sprinting, flying and zoom mods; the plain option value as fallback. */
    private static double fovDegrees() {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (!fovResolved) {
                fovResolved = true;
                for (Method m : GameRenderer.class.getDeclaredMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 3 && p[0] == Camera.class && p[1] == float.class && p[2] == boolean.class
                            && (m.getReturnType() == float.class || m.getReturnType() == double.class)) {
                        m.setAccessible(true);
                        fovMethod = m;
                        break;
                    }
                }
            }
            if (fovMethod != null) {
                Object v = fovMethod.invoke(mc.gameRenderer, CameraPos.camera(), 1.0f, true);
                double d = ((Number) v).doubleValue();
                if (d > 1 && d < 179) return d;
            }
        } catch (Throwable ignored) {
            fovMethod = null;
        }
        try {
            return mc.options.fov().get();
        } catch (Throwable e) {
            return 70.0;
        }
    }

    /** Camera relative world position to GUI pixels, or null when behind the camera. */
    private float[] project(float x, float y, float z) {
        Vector3f v = toView.transform(new Vector3f(x, y, z));
        float depth = -v.z;
        if (depth < 0.08f) return null;
        float nx = v.x / (depth * tanX), ny = v.y / (depth * tanY);
        return new float[]{(nx + 1f) * 0.5f * w, (1f - ny) * 0.5f * h};
    }

    @Override
    public void text(PoseStack pose, float x, float y, FormattedCharSequence seq, boolean shadow, boolean seeThrough, int argb) {
        if (!ok || (argb >>> 24) < 5) return;
        Matrix4f m = pose.last().pose();
        Vector4f p = m.transform(new Vector4f(x, y, 0f, 1f));
        Vector4f ex = m.transform(new Vector4f(1f, 0f, 0f, 0f));
        Vector4f ey = m.transform(new Vector4f(0f, 1f, 0f, 0f));
        float[] o = project(p.x, p.y, p.z);
        if (o == null || o[0] < -w * 0.5f || o[0] > w * 1.5f || o[1] < -h * 0.5f || o[1] > h * 1.5f) return;
        float[] a = project(p.x + ex.x, p.y + ex.y, p.z + ex.z);
        float[] b = project(p.x + ey.x, p.y + ey.y, p.z + ey.z);
        if (a == null || b == null) return;
        float dxX = a[0] - o[0], dxY = a[1] - o[1];
        float dyX = b[0] - o[0], dyY = b[1] - o[1];
        float kx = (float) Math.sqrt(dxX * dxX + dxY * dxY);
        if (kx < 0.02f || kx > 60f) return;
        float ky = (dxX * dyY - dxY * dyX) / kx;   // part of the y axis perpendicular to x keeps tilt and flip
        canvas.pushMatrix();
        canvas.translate(o[0], o[1]);
        canvas.rotate((float) Math.atan2(dxY, dxX));
        canvas.scale(kx, ky);
        canvas.textSeq(Minecraft.getInstance().font, seq, 0, 0, argb, shadow);
        canvas.popMatrix();
    }

    @Override public void rotateY(PoseStack pose, float rad) { pose.mulPose(new Matrix4f().rotationY(rad)); }
    @Override public void rotateX(PoseStack pose, float rad) { pose.mulPose(new Matrix4f().rotationX(rad)); }
    @Override public void rotateZ(PoseStack pose, float rad) { pose.mulPose(new Matrix4f().rotationZ(rad)); }
    @Override public void rotate(PoseStack pose, Quaternionf q) { pose.mulPose(new Matrix4f().rotation(q)); }
}
