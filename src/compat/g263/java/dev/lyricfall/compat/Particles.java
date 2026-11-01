package dev.lyricfall.compat;

import dev.lyricfall.config.HudConfig.WorldParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;

import java.util.concurrent.ThreadLocalRandom;

/** Small client-side particle bursts at a sung word. Only the player with the mod sees them. */
public final class Particles {
    private Particles() {}

    public static void burst(WorldParticles kind, double x, double y, double z, int count, float hue) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || kind == WorldParticles.OFF) return;
        if (kind == WorldParticles.AUTO) {
            if (mc.level.dimension() == Level.NETHER) kind = WorldParticles.SOUL;
            else if (mc.level.dimension() == Level.END) kind = WorldParticles.GLOW;
            else kind = WorldParticles.NOTES;
        }
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            double ox = (r.nextDouble() - 0.5) * 0.6, oy = (r.nextDouble() - 0.5) * 0.4, oz = (r.nextDouble() - 0.5) * 0.6;
            ParticleOptions type;
            double vx = (r.nextDouble() - 0.5) * 0.03, vy = 0.01 + r.nextDouble() * 0.03, vz = (r.nextDouble() - 0.5) * 0.03;
            switch (kind) {
                case NOTES -> {
                    // the note particle reads its color from the x speed (0..1 around the color wheel)
                    type = ParticleTypes.NOTE;
                    vx = (hue + i * 0.08f) % 1f;
                    vy = 0;
                    vz = 0;
                }
                case SPARKS -> type = ParticleTypes.ELECTRIC_SPARK;
                case SOUL -> type = ParticleTypes.SOUL_FIRE_FLAME;
                case ASH -> { type = ParticleTypes.WHITE_ASH; vy = -0.02; }
                case SNOW -> { type = ParticleTypes.SNOWFLAKE; vy = -0.01; }
                default -> type = ParticleTypes.END_ROD;
            }
            mc.level.addParticle(type, x + ox, y + oy, z + oz, vx, vy, vz);
        }
    }
}
