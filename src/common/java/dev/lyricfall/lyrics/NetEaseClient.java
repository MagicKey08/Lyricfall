package dev.lyricfall.lyrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.lyricfall.lyrics.TrackMatch.Level;
import dev.lyricfall.model.LyricLine;
import dev.lyricfall.model.Track;
import dev.lyricfall.net.SafeHttp;
import dev.lyricfall.util.Json;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lyrics from NetEase Cloud Music. Its public web API has synced lyrics for a huge amount of western songs,
 * often with word timings ("yrc"), and keeps working when LRCLIB is down. Only title, artist and the song id
 * are sent. Unofficial, but used by many lyrics tools for years.
 */
public final class NetEaseClient {
    private static final SafeHttp HTTP = new SafeHttp(Set.of("music.163.com"));
    private static final Map<String, String> HEADERS = Map.of(
            "User-Agent", SafeHttp.BROWSER_UA,
            "Referer", "https://music.163.com/",
            "Accept", "application/json");
    private static final double MAX_DIFF = 5;
    private static final int SONGS = 8, TRIES = 3;

    /** One yrc line: [lineStart,lineDur](wordStart,wordDur,0)word(wordStart,wordDur,0)word ... */
    private static final Pattern YRC_LINE = Pattern.compile("^\\[(\\d+),(\\d+)](.*)$");
    private static final Pattern YRC_WORD = Pattern.compile("\\((\\d+),(\\d+),\\d+\\)");

    private NetEaseClient() {}

    public static CompletableFuture<List<LyricLine>> fetch(Track t) {
        return LyricsIo.run(() -> lookup(t));
    }

    private static List<LyricLine> lookup(Track t) throws Exception {
        TrackMatch.Query q = TrackMatch.queries(t).get(0);
        String keyword = TrackMatch.cleanTitle(q.title()) + " " + TrackMatch.firstArtist(q.artist());
        JsonObject found = HTTP.getJson("https://music.163.com/api/search/get?type=1&offset=0&limit=" + SONGS
                + "&s=" + enc(keyword), HEADERS);
        JsonArray songs = Json.array(Json.obj(found, "result"), "songs");

        int tried = 0;
        for (JsonElement e : songs) {
            if (!e.isJsonObject()) continue;
            JsonObject s = e.getAsJsonObject();
            if (!sameSong(s, t)) continue;
            List<LyricLine> lines = lyricsFor(s.get("id").getAsString());
            if (!lines.isEmpty() && TrackMatch.scriptOk(t.title(), lines)) return lines;
            if (++tried >= TRIES) break;
        }
        return List.of();
    }

    private static boolean sameSong(JsonObject s, Track t) {
        double sec = s.has("duration") && s.get("duration").isJsonPrimitive() ? s.get("duration").getAsDouble() / 1000.0 : 0;
        double diff = TrackMatch.durationDiff(sec, t);
        String title = Json.str(s, "name");
        for (JsonElement a : Json.array(s, "artists")) {
            if (!a.isJsonObject()) continue;
            Level level = TrackMatch.level(title, Json.str(a.getAsJsonObject(), "name"), t);
            if (level != Level.NONE && TrackMatch.durationFits(diff, MAX_DIFF, level)) return true;
        }
        return false;
    }

    private static List<LyricLine> lyricsFor(String id) throws Exception {
        JsonObject res = HTTP.getJson("https://music.163.com/api/song/lyric?lv=1&kv=0&tv=0&yv=1&id=" + enc(id), HEADERS);

        String yrc = Json.str(Json.obj(res, "yrc"), "lyric");
        if (!yrc.isBlank()) {
            List<LyricLine> words = clean(LrcParser.parse(yrcToLrc(yrc)));
            if (!words.isEmpty()) return words;
        }
        String lrc = Json.str(Json.obj(res, "lrc"), "lyric");
        if (lrc.isBlank()) return List.of();
        List<LyricLine> lines = clean(LrcParser.parse(lrc));
        return !lines.isEmpty() && lines.get(0).timeMs() == LyricLine.UNSYNCED ? List.of() : lines;
    }

    /** Turns word timed yrc into enhanced LRC, which LrcParser already understands. */
    static String yrcToLrc(String yrc) {
        StringBuilder out = new StringBuilder();
        for (String raw : yrc.split("\\R")) {
            Matcher m = YRC_LINE.matcher(raw.trim());
            if (!m.matches()) continue;                   // credit lines come as JSON objects
            long lineMs = Long.parseLong(m.group(1));
            String body = m.group(3);
            Matcher w = YRC_WORD.matcher(body);
            StringBuilder line = new StringBuilder("[").append(stamp(lineMs)).append(']');
            int last = -1;
            long lastStart = -1;
            while (w.find()) {
                if (last >= 0) line.append('<').append(stamp(lastStart)).append('>').append(body, last, w.start());
                lastStart = Long.parseLong(w.group(1));
                last = w.end();
            }
            if (last >= 0) line.append('<').append(stamp(lastStart)).append('>').append(body.substring(last));
            out.append(line).append('\n');
        }
        return out.toString();
    }

    private static String stamp(long ms) {
        ms = Math.max(0, ms);
        return String.format(java.util.Locale.ROOT, "%02d:%02d.%02d", ms / 60_000, (ms / 1000) % 60, (ms % 1000) / 10);
    }

    // ---- shared clean up for lyrics from Chinese services ----

    private static final Pattern CREDIT = Pattern.compile(
            "^\\s*([^:：]{1,30})\\s*[:：].*$");
    private static final Pattern CREDIT_WORDS = Pattern.compile(
            "(?i).*(作词|作曲|编曲|制作|混音|母带|和声|吉他|贝斯|鼓|录音|监制|出品|发行|企划|统筹|演唱|原唱|词|曲|"
                    + "lyric|lyrics|written|writer|composer|compose|producer|produced|arrange|mix|master|vocal|"
                    + "op\\b|sp\\b|isrc|label|publisher).*");

    /** Drops writer/producer credit lines, empty lines and "instrumental, enjoy" placeholders. */
    static List<LyricLine> clean(List<LyricLine> in) {
        List<LyricLine> out = new ArrayList<>();
        for (LyricLine l : in) {
            String text = l.text() == null ? "" : l.text().trim();
            if (text.isEmpty()) continue;
            Matcher m = CREDIT.matcher(text);
            if (m.matches() && (hasCjk(m.group(1)) || CREDIT_WORDS.matcher(m.group(1)).matches())) continue;
            if (text.contains("纯音乐") || text.contains("此歌曲为没有填词")) return List.of();
            out.add(l);
        }
        return out;
    }

    private static boolean hasCjk(String s) {
        for (int i = 0; i < s.length(); i++) {
            Character.UnicodeScript sc = Character.UnicodeScript.of(s.charAt(i));
            if (sc == Character.UnicodeScript.HAN) return true;
        }
        return false;
    }

    private static String enc(String s) { return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8); }
}
