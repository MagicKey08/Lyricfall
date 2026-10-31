package dev.lyricfall.hud;

import dev.lyricfall.util.T;
import dev.lyricfall.media.NowPlaying;
import dev.lyricfall.lyrics.LrcParser;
import dev.lyricfall.model.LyricLine;
import dev.lyricfall.model.Track;

import java.util.ArrayList;
import java.util.List;

/** Мгновенный снимок состояния плеера для отрисовки (или демо-данные для предпросмотра в настройках). */
public final class Snapshot {
    public String id, title, artist, coverUrl;
    public long durationMs, posMs;
    public boolean playing;
    public List<LyricLine> lyrics = List.of();
    public boolean synced;

    private static List<LyricLine> demoLyrics;

    public static Snapshot live() {
        Track t = NowPlaying.current();
        if (t == null || NowPlaying.idle()) return null;
        Snapshot s = new Snapshot();
        s.id = t.id();
        s.title = t.title() == null ? "" : t.title();
        s.artist = t.artist() == null ? "" : t.artist();
        s.coverUrl = t.coverUrl();
        s.durationMs = t.durationMs();
        s.posMs = NowPlaying.positionMs();
        s.playing = NowPlaying.isPlaying();
        s.lyrics = NowPlaying.lyrics();
        s.synced = !s.lyrics.isEmpty() && s.lyrics.get(0).timeMs() != LyricLine.UNSYNCED;
        return s;
    }

    public static Snapshot liveOrDemo() {
        Snapshot s = live();
        return s != null ? s : demo();
    }

    public static Snapshot demo() {
        if (demoLyrics == null) {
            String[] text = new String[8];
            for (int i = 0; i < text.length; i++) text[i] = i == 5 ? "♪" : T.get("demo.line." + i);
            List<LyricLine> l = new ArrayList<>();
            for (int i = 0; i < text.length; i++) l.add(new LyricLine(60_000L + i * 4000L, text[i]));
            demoLyrics = l;
        }
        Snapshot s = new Snapshot();
        s.id = "demo";
        s.title = T.get("demo.title");
        s.artist = T.get("demo.artist");
        s.durationMs = 215_000;
        s.posMs = 60_000L + System.currentTimeMillis() % 32_000L;
        s.playing = true;
        s.lyrics = demoLyrics;
        s.synced = true;
        return s;
    }

    public int lyricIndex() { return synced ? LrcParser.currentIndex(lyrics, posMs) : -1; }

    public double fraction() { return durationMs <= 0 ? 0 : Math.max(0, Math.min(1, posMs / (double) durationMs)); }

    public static String fmt(long ms) {
        long s = Math.max(0, ms) / 1000;
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    public String timeLabel() { return fmt(posMs) + " / " + fmt(durationMs); }
}
