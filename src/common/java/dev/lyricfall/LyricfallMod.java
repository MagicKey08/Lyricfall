package dev.lyricfall;

import dev.lyricfall.config.HudConfig;
import dev.lyricfall.input.Hotkeys;
import dev.lyricfall.media.NowPlaying;
import dev.lyricfall.media.SystemMedia;

import java.nio.file.Path;

/** Common start-up; the loader calls init and wires up the game events. */
public final class LyricfallMod {
    private LyricfallMod() {}

    public static final String MOD_ID = "lyricfall";
    public static final String NAME = "Lyricfall";
    public static final String AUTHORS = "MagicKey08, based on Overtone by chifir4ik and PabJlo";
    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(NAME);

    public static void init(Path configDir, Path gameDir) {
        Env.init(configDir, gameDir);
        LOGGER.info("{} ({})", NAME, AUTHORS);
        HudConfig.get();
        Hotkeys.create();
        SystemMedia.start();
    }

    public static void onClientStarted() {}

    public static void onClientTick() {
        Hotkeys.tick();
        NowPlaying.tick();
    }
}
