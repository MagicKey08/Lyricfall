package dev.lyricfall.config;

import dev.lyricfall.Env;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.lyricfall.util.T;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Display settings for lyrics in the world and on screen: config/lyricfall-display.json. Missing fields get defaults. */
public final class HudConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static HudConfig instance;

    public enum Anchor {
        TOP_LEFT(-1, -1), TOP_CENTER(0, -1), TOP_RIGHT(1, -1),
        MID_LEFT(-1, 0), CENTER(0, 0), MID_RIGHT(1, 0),
        BOTTOM_LEFT(-1, 1), BOTTOM_CENTER(0, 1), BOTTOM_RIGHT(1, 1);
        /** -1 слева, 0 центр, 1 справа */
        public final int h;
        /** -1 сверху, 0 середина, 1 снизу */
        public final int v;
        Anchor(int h, int v) { this.h = h; this.v = v; }

        public static Anchor of(int h, int v) {
            for (Anchor a : values()) if (a.h == h && a.v == v) return a;
            return TOP_LEFT;
        }
    }

    public enum WorldMode { FOLLOW, FIXED }

    /** SCREEN — текст всегда перед взглядом; WORLD — следует за игроком, но не поворачивается вместе с головой. */
    public enum FollowStyle { SCREEN, WORLD }

    /** LINE — слова строки собираются в ряд; SCATTER — каждое слово появляется отдельно, в случайном месте рядом. */
    public enum WorldLayout { LINE, SCATTER }

    /** Анимация появления слов песни в мире. */
    public enum WorldAnim { FLY, RISE, DROP, POP, WAVE, TYPE, SPIN, GLITCH }
    public enum WorldColor { COVER, CLASSIC, ACCENT, GRADIENT, RAINBOW, WHITE }
    /** Как буквы уходят: падают на блоки, рассыпаются, улетают, сдуваются ветром и т. д. */
    public enum WorldExit { FADE, FALL, SHATTER, FLOAT, WIND, SHRINK, SINK }
    public enum LyricsEnter { SLIDE, RISE, POP, TYPE }
    public enum WorldGlow { OFF, SUNG, ALL }
    /** Particles that burst from every word as it is sung. AUTO picks one that suits the dimension. */
    public enum WorldParticles { OFF, AUTO, GLOW, NOTES, SPARKS, SOUL, ASH, SNOW }
    /** Ready made looks; CUSTOM as soon as anything is changed by hand. */
    public enum WorldPreset { CUSTOM, CINEMA, MIDNIGHT, HYPE, CLEAN, DREAM, CYBER, GRAVITY, FLIGHT }
    /** Что анимируется по очереди: слова целиком или отдельные буквы. */
    public enum WorldUnit { WORDS, LETTERS }

    public enum LyricsMode { OFF, SUBTITLE, KARAOKE }

    /**
     * Soft, bright tones that stay readable as floating text over sky, caves, snow and the nether
     * (all around 70 to 80 % lightness, none of them neon).
     */
    public enum Accent {
        ROSE(0xFF6F96), SAKURA(0xFFB3D1), PEACH(0xFFB48C), GOLD(0xFFD67A), MINT(0x7FEFC6),
        ICE(0x8FD6FF), LILAC(0xBBA3FF), VIOLET(0x9479FF), WHITE(0xFFFFFF);
        public final int rgb;
        Accent(int rgb) { this.rgb = rgb; }
    }

    /** Название значения перечисления на языке игры: ключ вида overtone.opt.anchor.top_left. */
    public static String label(Enum<?> e) {
        return T.get("opt." + e.getDeclaringClass().getSimpleName().toLowerCase() + "." + e.name().toLowerCase());
    }

    public static <E extends Enum<E>> E prev(E e) {
        E[] all = e.getDeclaringClass().getEnumConstants();
        return all[(e.ordinal() + all.length - 1) % all.length];
    }

    public static <E extends Enum<E>> E next(E e) {
        E[] all = e.getDeclaringClass().getEnumConstants();
        return all[(e.ordinal() + 1) % all.length];
    }

    public int accentRgb() { return customAccent >= 0 ? customAccent : accent.rgb; }

    public boolean master = true;
    public Accent accent = Accent.LILAC;
    /** Свой цвет акцента (RGB). -1 — используется {@link #accent}. */
    public int customAccent = -1;
    public boolean hideWithGui = false;
    /** Пока зажат Tab (список игроков), виджет, текст и громкость плавно гаснут. */
    public boolean hideOnTab = false;


    /** Слова песни летят в мире, когда их поют. Включено — обычный HUD-текст для треков с таймкодами не рисуется. */
    public boolean worldLyrics = true;
    /** Субтитры на экране остаются, даже когда слова летят в мире. */
    public boolean lyricsWithWorld = false;
    public WorldMode worldMode = WorldMode.FOLLOW;
    /** SCREEN — текст всегда перед взглядом; WORLD — следует за игроком, но не поворачивается вместе с головой. */
    public FollowStyle worldFollowStyle = FollowStyle.SCREEN;
    /** LINE — слова строки собираются в ряд; SCATTER — каждое слово появляется отдельно, в случайном месте рядом. */
    public WorldLayout worldLayout = WorldLayout.LINE;
    /** Smooth movement: text moves rigidly with you, gently re-centres on curvy paths, flies ahead when gliding. */
    public boolean worldSmooth = true;
    /** Text leans into turns and rolls with the camera (Do a Barrel Roll). */
    public boolean worldBank = true;
    public float worldScale = 1.0f;
    /** Расстояние от игрока до текста, блоков. */
    public float worldDistance = 9.0f;
    /** Высота текста относительно глаз, блоков. */
    public float worldHeight = 0.0f;
    /** Половина горизонтальной зоны, где появляются слова, градусов. Для узких и растянутых экранов её стоит уменьшить. */
    public float worldSpread = 57f;
    public boolean worldSeeThrough = true;
    public boolean worldShadow = false;
    public WorldAnim worldAnim = WorldAnim.RISE;
    public WorldExit worldExit = WorldExit.FALL;
    /** Сколько секунд слова остаются в мире после того, как их спели. */
    public int worldLinger = 4;
    public WorldUnit worldUnit = WorldUnit.LETTERS;
    /** Задержка между буквами, мс. */
    public int worldLetterMs = 35;
    /** Скорость анимации, %. */
    public int worldAnimSpeed = 100;
    /** Сила отскока букв при падении, %. */
    public int worldBounce = 100;
    public WorldColor worldColor = WorldColor.COVER;
    public Accent worldAccent = Accent.LILAC;
    public Accent worldAccent2 = Accent.ICE;
    /** Soft dark outline around every letter: readable on bright sky and snow without a hard shadow. */
    public boolean worldOutline = true;
    /** Long sung notes swell and glow a little stronger, like Apple Music lyrics. */
    public boolean worldSwell = true;
    /** Ad-libs in brackets, like "(yeah)", are drawn smaller and paler; whole ad-lib lines float off to the side. */
    public boolean worldAdlibs = true;
    /** Three breathing dots during instrumental breaks, filling up until the singing starts again. */
    public boolean worldInterlude = true;
    /** Title and artist float in front of you for a moment when a new song starts. */
    public boolean worldTitleCard = true;
    public WorldParticles worldParticles = WorldParticles.AUTO;
    /** Glow gets stronger in caves and at night, outline lighter where it is dark. */
    public boolean worldAdaptive = true;
    /** With LambDynamicLights the sung words light up the blocks around them. */
    public boolean worldLight = true;
    public int worldLightLevel = 10;
    public WorldPreset worldPreset = WorldPreset.CUSTOM;
    /** Schema of this file: 1 = new color system (cover colors, outline instead of shadow) applied once. */
    public int displayVersion = 0;
    public WorldGlow worldGlow = WorldGlow.SUNG;
    public int worldGlowStrength = 60;
    public int worldGlowSize = 100;

    public LyricsMode lyricsMode = LyricsMode.SUBTITLE;
    public Anchor lyricsAnchor = Anchor.BOTTOM_CENTER;
    public int lyricsOffsetX = 0;
    public int lyricsOffsetY = 76;
    public float lyricsScale = 1.0f;
    public int lyricsWidth = 320;
    public int lyricsLines = 5;
    public int lyricsBackground = 40;
    public boolean lyricsAccent = true;
    public boolean lyricsShadow = true;
    /** Ручной сдвиг текста, мс: положительный — строки появляются позже, отрицательный — раньше. */
    public int lyricsShiftMs = 0;
    public boolean lyricsWordKaraoke = true;
    public LyricsEnter lyricsEnter = LyricsEnter.RISE;
    /** Как уходит строка, которую уже спели: рассыпается на буквы, как слова в мире. */
    public WorldExit lyricsExit = WorldExit.FALL;
    public int lyricsLinger = 3;
    public int lyricsLetterMs = 25;



    public static HudConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    public static void resetToDefaults() {
        instance = new HudConfig();
        instance.save();
    }

    /** Значение по умолчанию любого поля: правый клик по настройке возвращает его. */
    public static final HudConfig DEFAULTS = new HudConfig();

    private static Path path() { return Env.configDir().resolve("lyricfall-display.json"); }

    private static HudConfig load() {
        HudConfig c = null;
        try {
            Path p = path();
            if (Files.exists(p)) c = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), HudConfig.class);
        } catch (Exception ignored) {}
        if (c == null) c = new HudConfig();
        c.sanitize();
        return c;
    }

    private void sanitize() {
        HudConfig d = new HudConfig();
        if (displayVersion < 1) {
            worldColor = WorldColor.COVER;
            worldShadow = false;
            worldOutline = true;
            displayVersion = 1;
        }
        if (accent == null) accent = d.accent;
        if (customAccent < 0 || customAccent > 0xFFFFFF) customAccent = -1;
        if (lyricsMode == null) lyricsMode = d.lyricsMode;
        if (lyricsAnchor == null) lyricsAnchor = d.lyricsAnchor;
        if (worldMode == null) worldMode = d.worldMode;
        if (worldFollowStyle == null) worldFollowStyle = d.worldFollowStyle;
        if (worldLayout == null) worldLayout = d.worldLayout;
        if (worldAnim == null) worldAnim = d.worldAnim;
        if (worldExit == null) worldExit = d.worldExit;
        if (worldUnit == null) worldUnit = d.worldUnit;
        if (worldColor == null) worldColor = d.worldColor;
        if (worldAccent == null) worldAccent = d.worldAccent;
        if (worldAccent2 == null) worldAccent2 = d.worldAccent2;
        if (worldGlow == null) worldGlow = d.worldGlow;
        if (worldParticles == null) worldParticles = d.worldParticles;
        if (worldPreset == null) worldPreset = d.worldPreset;
        worldLightLevel = Math.max(1, Math.min(15, worldLightLevel));
        worldLinger = Math.max(1, Math.min(20, worldLinger));
        worldLetterMs = Math.max(5, Math.min(150, worldLetterMs));
        worldAnimSpeed = Math.max(25, Math.min(300, worldAnimSpeed));
        worldBounce = Math.max(0, Math.min(300, worldBounce));
        worldGlowStrength = Math.max(0, Math.min(100, worldGlowStrength));
        worldGlowSize = Math.max(50, Math.min(250, worldGlowSize));
        worldScale = Math.max(0.5f, Math.min(2.5f, worldScale));
        worldDistance = Math.max(3f, Math.min(30f, worldDistance));
        worldHeight = Math.max(-1.5f, Math.min(3f, worldHeight));
        worldSpread = Math.max(15f, Math.min(90f, worldSpread));
        lyricsScale = Math.max(0.5f, Math.min(2.0f, lyricsScale));
        lyricsBackground = Math.max(0, Math.min(100, lyricsBackground));
        lyricsWidth = Math.max(120, Math.min(600, lyricsWidth));
        lyricsShiftMs = Math.max(-5000, Math.min(5000, lyricsShiftMs));
        if (lyricsEnter == null) lyricsEnter = d.lyricsEnter;
        if (lyricsExit == null) lyricsExit = d.lyricsExit;
        lyricsLinger = Math.max(1, Math.min(15, lyricsLinger));
        lyricsLetterMs = Math.max(0, Math.min(120, lyricsLetterMs));
        lyricsLines = Math.max(3, Math.min(9, lyricsLines));
    }

    public void save() {
        try {
            Path p = path();
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }
}
