package dev.lyricfall.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.lyricfall.compat.Game;
import dev.lyricfall.compat.KeyInput;
import dev.lyricfall.compat.Keys;
import dev.lyricfall.config.HudConfig;
import dev.lyricfall.hud.WorldLyricsRenderer;
import dev.lyricfall.media.NowPlaying;
import dev.lyricfall.ui.HudConfigScreen;
import dev.lyricfall.util.T;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Hotkeys. The source of truth are normal KeyMappings (options.txt), so they also show up in the vanilla controls
 * menu in their own category.
 */
public final class Hotkeys {
    private Hotkeys() {}

    public enum Action {
        WORLD_LYRICS("world_lyrics", InputConstants.KEY_J),
        HUD_SETTINGS("hud_settings", InputConstants.KEY_K),
        TOGGLE_HUD("toggle_hud", InputConstants.UNKNOWN.getValue()),
        RELOAD("reload", InputConstants.UNKNOWN.getValue());

        public final int defaultKey;
        final String translationKey;
        Action(String id, int defaultKey) { this.defaultKey = defaultKey; this.translationKey = "key.lyricfall." + id; }

        public String label() { return Component.translatable(translationKey).getString(); }
    }

    private static final int UNBOUND = -1;
    private static final KeyMapping[] MAPPINGS = new KeyMapping[Action.values().length];

    /** Creates the KeyMappings; the loader registers them (see {@link #mappings()}). */
    public static void create() {
        for (Action a : Action.values())
            MAPPINGS[a.ordinal()] = Keys.create(a.translationKey, a.defaultKey);
    }

    public static KeyMapping[] mappings() { return MAPPINGS.clone(); }

    public static void tick() {
        boolean inWorld = Game.screen() == null;
        for (Action a : Action.values()) {
            KeyMapping km = MAPPINGS[a.ordinal()];
            if (km == null) continue;
            while (km.consumeClick()) if (inWorld) perform(a, null);
        }
    }

    public static Action actionFor(KeyInput event) {
        for (Action a : Action.values()) {
            KeyMapping km = MAPPINGS[a.ordinal()];
            if (km != null && !km.isUnbound() && event.matches(km)) return a;
        }
        return null;
    }

    public static void perform(Action a, Screen menu) {
        switch (a) {
            case HUD_SETTINGS -> Game.open(new HudConfigScreen(menu));
            case TOGGLE_HUD -> {
                HudConfig c = HudConfig.get();
                c.master = !c.master;
                c.save();
            }
            case WORLD_LYRICS -> {
                HudConfig c = HudConfig.get();
                c.worldLyrics = !c.worldLyrics;
                c.save();
                WorldLyricsRenderer.toast(T.get(c.worldLyrics ? "toast.world_on" : "toast.world_off"));
            }
            case RELOAD -> {
                NowPlaying.reload();
                WorldLyricsRenderer.toast(T.get("toast.reload"));
            }
        }
    }

    /** Assigns a key (-1 clears it). If another action of the mod already uses it, that one is cleared. */
    public static void setKey(Action a, int code) {
        KeyMapping km = MAPPINGS[a.ordinal()];
        if (km == null) return;
        InputConstants.Key key = code == UNBOUND ? InputConstants.UNKNOWN : Keys.key(code);
        if (code != UNBOUND) {
            for (Action other : Action.values()) {
                KeyMapping o = MAPPINGS[other.ordinal()];
                if (other != a && o != null && !o.isUnbound() && o.saveString().equals(key.getName()))
                    o.setKey(InputConstants.UNKNOWN);
            }
        }
        km.setKey(key);
        KeyMapping.resetMapping();
        save();
    }

    public static void resetAll() {
        for (Action a : Action.values()) {
            KeyMapping km = MAPPINGS[a.ordinal()];
            if (km != null) km.setKey(km.getDefaultKey());
        }
        KeyMapping.resetMapping();
        save();
    }

    private static void save() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null) mc.options.save();
    }

    public static String keyName(Action a) {
        KeyMapping km = MAPPINGS[a.ordinal()];
        if (km == null || km.isUnbound()) return T.get("keys.unbound");
        return km.getTranslatedKeyMessage().getString();
    }

    public static boolean isDefault(Action a) {
        KeyMapping km = MAPPINGS[a.ordinal()];
        return km != null && km.isDefault();
    }
}
