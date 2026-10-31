package dev.lyricfall.config;

import dev.lyricfall.Env;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** General settings: song source, lyrics sources and the look of the settings window. config/lyricfall-general.json */
public final class PlayerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static PlayerConfig instance;

    /** false: only Spotify. true: any app that shows up in the Windows media overlay (browser, Apple Music, ...). */
    public boolean anyApp = false;

    /** Search lyrics on LRCLIB (title, artist, album and duration are sent to lrclib.net). */
    public boolean onlineLyrics = true;

    /** Word timed lyrics from AMLL TTML DB (an index is downloaded from GitHub every few days). */
    public boolean amllLyrics = true;

    /** NetEase Cloud Music: huge catalogue of synced lyrics, often word timed. Unofficial API. */
    public boolean neteaseLyrics = true;

    /** KuGou: unofficial API of a Chinese service, fallback. */
    public boolean kugouLyrics = true;

    /** Settings schema: 1 = NetEase and KuGou switched on once for everyone (LRCLIB alone was not enough). */
    public int settingsVersion = 1;

    /** 0..3: how much accent is mixed into the window background (0 / 5 / 10 / 15 %). */
    public int tint = 1;
    public int windowOpacity = 100;
    public int dim = 45;
    public int cornerRadius = 4;
    public enum WindowAnim { ZOOM, SLIDE, POP, UNFOLD, OFF }
    public WindowAnim windowAnim = WindowAnim.ZOOM;

    public int accentRgb() { return HudConfig.get().accentRgb(); }

    public static PlayerConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    public static void resetToDefaults() {
        instance = new PlayerConfig();
        instance.save();
    }

    public static final PlayerConfig DEFAULTS = new PlayerConfig();

    private static Path path() { return Env.configDir().resolve("lyricfall-general.json"); }

    private static PlayerConfig load() {
        PlayerConfig c = null;
        try {
            Path p = path();
            if (Files.exists(p)) c = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), PlayerConfig.class);
        } catch (Exception ignored) {}
        if (c == null) c = new PlayerConfig();
        c.sanitize();
        return c;
    }

    private void sanitize() {
        if (settingsVersion < 1) {
            neteaseLyrics = true;
            kugouLyrics = true;
            settingsVersion = 1;
        }
        if (windowAnim == null) windowAnim = WindowAnim.ZOOM;
        tint = Math.max(0, Math.min(3, tint));
        dim = Math.max(0, Math.min(100, dim));
        windowOpacity = Math.max(20, Math.min(100, windowOpacity));
        cornerRadius = Math.max(0, Math.min(8, cornerRadius));
    }

    public void save() {
        try {
            Path p = path();
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }
}
