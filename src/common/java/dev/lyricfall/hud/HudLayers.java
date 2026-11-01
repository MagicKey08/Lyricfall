package dev.lyricfall.hud;

import dev.lyricfall.compat.Canvas;
import dev.lyricfall.compat.Game;
import dev.lyricfall.ui.HudConfigScreen;

/** Все слои HUD мода в одном месте: загрузчики зовут отсюда, чтобы не повторять порядок отрисовки. */
public final class HudLayers {
    private HudLayers() {}

    public static void render(Canvas c) {
        if (HudConfigScreen.OPEN && Game.guiOpen()) return;   // в окне настроек вместо HUD рисуется предпросмотр

        LyricsRenderer.render(c, false);
    }
}
