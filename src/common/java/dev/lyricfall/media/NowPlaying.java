package dev.lyricfall.media;

import dev.lyricfall.LyricfallMod;
import dev.lyricfall.config.PlayerConfig;
import dev.lyricfall.lyrics.LyricsService;
import dev.lyricfall.model.LyricLine;
import dev.lyricfall.model.Track;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The song that is playing in Spotify (or another app, if allowed in the settings) plus its lyrics.
 * Everything that draws lyrics reads from here: current track, position, play state and the lyric lines.
 */
public final class NowPlaying {
    private NowPlaying() {}

    public enum LyricsState { WAITING, SEARCHING, SYNCED, PLAIN, NONE }

    /** Retries when nothing was found: LRCLIB and friends sometimes answer slowly or fail once. */
    private static final long[] RETRY_MS = {6_000, 20_000, 60_000};
    private static final int CACHE_SIZE = 60;

    private static volatile Track current;
    private static volatile SystemMedia.Session session;
    private static volatile List<LyricLine> lyrics = List.of();
    private static volatile LyricsState lyricsState = LyricsState.WAITING;
    private static int attempt;
    private static long retryAt;
    private static long generation;

    private static final Map<String, List<LyricLine>> CACHE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<LyricLine>> e) { return size() > CACHE_SIZE; }
    };

    public static Track current() { return current; }

    public static List<LyricLine> lyrics() { return lyrics; }

    public static LyricsState lyricsState() { return lyricsState; }

    /** Nothing to show: no song, or the app has been stopped. */
    public static boolean idle() {
        SystemMedia.Session s = session;
        return current == null || s == null || !SystemMedia.fresh();
    }

    public static boolean isPlaying() {
        SystemMedia.Session s = session;
        return s != null && s.playing() && SystemMedia.fresh();
    }

    public static long positionMs() {
        SystemMedia.Session s = session;
        return s == null ? 0 : s.positionNow();
    }

    public static boolean synced() {
        List<LyricLine> l = lyrics;
        return !l.isEmpty() && l.get(0).timeMs() != LyricLine.UNSYNCED;
    }

    /** Called every client tick. Cheap: only compares the latest reading with the last one. */
    public static void tick() {
        SystemMedia.Session s = pick(SystemMedia.sessions());
        session = s;
        if (s == null || s.title() == null || s.title().isBlank()) return;

        String key = key(s);
        Track t = current;
        if (t == null || !t.id().equals(key)) {
            current = new Track(key, s.title(), s.artist(), s.album(), s.durationMs(), null, s.app());
            startLookup();
        } else if (t.durationMs() <= 0 && s.durationMs() > 0) {
            current = new Track(t.id(), t.title(), t.artist(), t.album(), s.durationMs(), null, t.provider());
        }

        if (lyricsState == LyricsState.NONE && attempt < RETRY_MS.length && System.currentTimeMillis() >= retryAt) {
            fetch(current, generation, true);
        }
    }

    /** Forget the cached lyrics of this song and search again (button in the settings). */
    public static void reload() {
        Track t = current;
        if (t == null) return;
        synchronized (CACHE) { CACHE.remove(t.id()); }
        startLookup();
    }

    private static void startLookup() {
        generation++;
        attempt = 0;
        lyrics = List.of();
        Track t = current;
        List<LyricLine> cached;
        synchronized (CACHE) { cached = CACHE.get(t.id()); }
        if (cached != null) {
            setLyrics(cached);
            return;
        }
        List<LyricLine> local = LocalLyrics.find(t);
        if (local != null && !local.isEmpty()) {
            setLyrics(local);
            return;
        }
        fetch(t, generation, false);
    }

    private static void fetch(Track t, long gen, boolean retry) {
        lyricsState = LyricsState.SEARCHING;
        if (retry) attempt++;
        LyricsService.fetch(t).whenComplete((found, err) -> {
            if (gen != generation) return;            // song changed in the meantime
            List<LyricLine> l = found == null ? List.of() : found;
            if (l.isEmpty()) {
                lyrics = List.of();
                lyricsState = LyricsState.NONE;
                retryAt = System.currentTimeMillis() + (attempt < RETRY_MS.length ? RETRY_MS[attempt] : 0);
                return;
            }
            synchronized (CACHE) { CACHE.put(t.id(), l); }
            setLyrics(l);
            LyricfallMod.LOGGER.info("Lyrics for \"{}\" by {}: {} lines{}", t.title(), t.artist(), l.size(),
                    synced() ? "" : " (not synced)");
        });
    }

    private static void setLyrics(List<LyricLine> l) {
        lyrics = l;
        lyricsState = l.isEmpty() ? LyricsState.NONE
                : l.get(0).timeMs() != LyricLine.UNSYNCED ? LyricsState.SYNCED : LyricsState.PLAIN;
    }

    /** Spotify first; other apps only when allowed. Among the candidates the one that is playing wins. */
    private static SystemMedia.Session pick(List<SystemMedia.Session> all) {
        boolean any = PlayerConfig.get().anyApp;
        SystemMedia.Session best = null;
        int bestScore = -1;
        for (SystemMedia.Session s : all) {
            if (!any && !s.isSpotify()) continue;
            int score = (s.playing() ? 4 : 0) + (s.isSpotify() ? 2 : 0) + (s.title() != null && !s.title().isBlank() ? 1 : 0);
            if (score > bestScore) { best = s; bestScore = score; }
        }
        return best;
    }

    private static String key(SystemMedia.Session s) {
        return (s.artist() + "\u0000" + s.title() + "\u0000" + s.album()).toLowerCase(Locale.ROOT);
    }
}
