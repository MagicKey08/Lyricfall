package dev.lyricfall.config;

import dev.lyricfall.config.HudConfig.Accent;
import dev.lyricfall.config.HudConfig.Facing;
import dev.lyricfall.config.HudConfig.FollowStyle;
import dev.lyricfall.config.HudConfig.WorldAnim;
import dev.lyricfall.config.HudConfig.WorldColor;
import dev.lyricfall.config.HudConfig.WorldExit;
import dev.lyricfall.config.HudConfig.WorldGlow;
import dev.lyricfall.config.HudConfig.WorldLayout;
import dev.lyricfall.config.HudConfig.WorldMode;
import dev.lyricfall.config.HudConfig.WorldParticles;
import dev.lyricfall.config.HudConfig.WorldPreset;
import dev.lyricfall.config.HudConfig.WorldUnit;

/**
 * Ready made styles for the flying lyrics. Every preset sets the look (colors, glow, size, particles) and the
 * behaviour (where the text sits, how it follows you, how words come and go). Afterwards everything can still be
 * tuned by hand; that switches the preset display to "custom".
 */
public final class Presets {
    private Presets() {}

    public static void apply(HudConfig c, WorldPreset p) {
        if (p == WorldPreset.CUSTOM) return;
        base(c);
        switch (p) {
            case WORLD -> {
                // part of the world: each line appears ahead of you and stays there like a floating sign,
                // lit by its surroundings, hidden by hills and walls; the sung word glows, then letters drop onto the ground
                size(c, 2.2f, 10f, 0.8f);
                c.worldMode = WorldMode.FIXED;
                c.worldFacing = Facing.WORLD;
                c.worldSeeThrough = false;
                anim(c, WorldAnim.RISE, WorldUnit.LETTERS, 30, 90, 50);
                exit(c, WorldExit.FALL, 6);
                colors(c, WorldColor.COVER, Accent.LILAC, Accent.ICE);
                glow(c, WorldGlow.SUNG, 50, 110);
                c.worldParticles = WorldParticles.AUTO;
                c.worldLightLevel = 10;
            }
            case SKYWAY -> {
                // for the elytra: lines hang in the sky far ahead on your path and you fly through them
                size(c, 2.5f, 14f, 0.3f);
                c.worldMode = WorldMode.FIXED;
                c.worldFacing = Facing.WORLD;
                c.worldSeeThrough = false;
                anim(c, WorldAnim.FLY, WorldUnit.WORDS, 20, 110, 40);
                exit(c, WorldExit.WIND, 2);
                colors(c, WorldColor.COVER, Accent.ICE, Accent.LILAC);
                glow(c, WorldGlow.SUNG, 70, 120);
                c.worldParticles = WorldParticles.GLOW;
                c.worldLightLevel = 12;
            }
            case CINEMA -> {
                // calm and premium: like subtitles in a film, colors from the cover
                size(c, 2.0f, 10f, 1.4f);
                anim(c, WorldAnim.RISE, WorldUnit.LETTERS, 28, 85, 40);
                exit(c, WorldExit.FADE, 3);
                colors(c, WorldColor.COVER, Accent.LILAC, Accent.ICE);
                glow(c, WorldGlow.SUNG, 55, 110);
                c.worldParticles = WorldParticles.OFF;
                c.worldLightLevel = 9;
            }
            case MIDNIGHT -> {
                // moody and slow: violet to rose, everything glows softly, words sink away, ash drifts down
                size(c, 2.2f, 9f, 1.6f);
                anim(c, WorldAnim.FLY, WorldUnit.WORDS, 30, 70, 60);
                exit(c, WorldExit.SINK, 4);
                colors(c, WorldColor.GRADIENT, Accent.VIOLET, Accent.ROSE);
                glow(c, WorldGlow.ALL, 40, 150);
                c.worldParticles = WorldParticles.ASH;
                c.worldLightLevel = 8;
            }
            case HYPE -> {
                // energetic: words pop in your face, shatter, sparks fly, strong glow
                size(c, 2.4f, 9f, 1.4f);
                c.worldFollowStyle = FollowStyle.SCREEN;
                anim(c, WorldAnim.POP, WorldUnit.WORDS, 20, 140, 180);
                exit(c, WorldExit.SHATTER, 2);
                colors(c, WorldColor.COVER, Accent.ROSE, Accent.GOLD);
                glow(c, WorldGlow.SUNG, 85, 130);
                c.worldParticles = WorldParticles.SPARKS;
                c.worldLightLevel = 13;
            }
            case CLEAN -> {
                // minimal and readable: white text, no effects, just the lyrics
                size(c, 1.8f, 8f, 0.8f);
                c.worldFollowStyle = FollowStyle.SCREEN;
                anim(c, WorldAnim.RISE, WorldUnit.WORDS, 25, 110, 0);
                exit(c, WorldExit.FADE, 2);
                colors(c, WorldColor.WHITE, Accent.ICE, Accent.ICE);
                glow(c, WorldGlow.OFF, 0, 100);
                c.worldSwell = false;
                c.worldBank = false;
                c.worldTitleCard = false;
                c.worldParticles = WorldParticles.OFF;
                c.worldLight = false;
            }
            case DREAM -> {
                // pastel and floaty: words appear all around you in waves and drift up
                size(c, 2.1f, 10f, 1.6f);
                c.worldLayout = WorldLayout.SCATTER;
                anim(c, WorldAnim.WAVE, WorldUnit.LETTERS, 45, 75, 170);
                exit(c, WorldExit.FLOAT, 4);
                colors(c, WorldColor.GRADIENT, Accent.SAKURA, Accent.ICE);
                glow(c, WorldGlow.ALL, 45, 170);
                c.worldParticles = WorldParticles.GLOW;
                c.worldLightLevel = 9;
            }
            case CYBER -> {
                // glitchy: letters flicker in, get blown away, mint to violet with electric sparks
                size(c, 2.2f, 9f, 1.2f);
                anim(c, WorldAnim.GLITCH, WorldUnit.LETTERS, 15, 130, 30);
                exit(c, WorldExit.WIND, 2);
                colors(c, WorldColor.GRADIENT, Accent.MINT, Accent.VIOLET);
                glow(c, WorldGlow.SUNG, 80, 120);
                c.worldParticles = WorldParticles.SPARKS;
                c.worldLightLevel = 11;
            }
            case GRAVITY -> {
                // the lines stay where they were sung, drop in and fall onto the blocks; you walk past them
                size(c, 2.3f, 8f, 1.0f);
                c.worldMode = WorldMode.FIXED;
                anim(c, WorldAnim.DROP, WorldUnit.LETTERS, 25, 100, 140);
                exit(c, WorldExit.FALL, 8);
                colors(c, WorldColor.COVER, Accent.PEACH, Accent.LILAC);
                glow(c, WorldGlow.SUNG, 60, 110);
                c.worldParticles = WorldParticles.OFF;
                c.worldLightLevel = 10;
            }
            case FLIGHT -> {
                // made for the elytra: big text far ahead, follows your view, leans into turns, blown away behind you
                size(c, 2.5f, 12f, 1.0f);
                c.worldFollowStyle = FollowStyle.SCREEN;
                anim(c, WorldAnim.FLY, WorldUnit.WORDS, 20, 120, 50);
                exit(c, WorldExit.WIND, 2);
                colors(c, WorldColor.COVER, Accent.ICE, Accent.LILAC);
                glow(c, WorldGlow.SUNG, 70, 120);
                c.worldParticles = WorldParticles.GLOW;
                c.worldLightLevel = 12;
            }
            default -> { }
        }
        c.worldPreset = p;
    }

    private static void base(HudConfig c) {
        c.worldLyrics = true;
        c.lyricsWithWorld = false;
        c.worldMode = WorldMode.FOLLOW;
        c.worldFollowStyle = FollowStyle.WORLD;
        c.worldLayout = WorldLayout.LINE;
        c.worldSmooth = true;
        c.worldBank = true;
        c.worldSpread = 57f;
        c.worldSeeThrough = true;
        c.worldShadow = false;
        c.worldOutline = true;
        c.worldSwell = true;
        c.worldAdlibs = true;
        c.worldInterlude = true;
        c.worldTitleCard = true;
        c.worldAdaptive = true;
        c.worldLight = true;
        c.worldFacing = Facing.SCREEN;
        c.worldImmersive = true;
    }

    private static void size(HudConfig c, float scale, float distance, float height) {
        c.worldScale = scale;
        c.worldDistance = distance;
        c.worldHeight = height;
    }

    private static void anim(HudConfig c, WorldAnim a, WorldUnit unit, int letterMs, int speed, int bounce) {
        c.worldAnim = a;
        c.worldUnit = unit;
        c.worldLetterMs = letterMs;
        c.worldAnimSpeed = speed;
        c.worldBounce = bounce;
    }

    private static void exit(HudConfig c, WorldExit e, int linger) {
        c.worldExit = e;
        c.worldLinger = linger;
    }

    private static void colors(HudConfig c, WorldColor mode, Accent a, Accent b) {
        c.worldColor = mode;
        c.worldAccent = a;
        c.worldAccent2 = b;
    }

    private static void glow(HudConfig c, WorldGlow g, int strength, int size) {
        c.worldGlow = g;
        c.worldGlowStrength = strength;
        c.worldGlowSize = size;
    }
}
