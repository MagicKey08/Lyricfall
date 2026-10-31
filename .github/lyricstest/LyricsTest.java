import dev.lyricfall.Env;
import dev.lyricfall.lyrics.LyricsService;
import dev.lyricfall.model.LyricLine;
import dev.lyricfall.model.Track;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Looks up lyrics for a few songs exactly the way the mod does, without Minecraft. */
public final class LyricsTest {
    public static void main(String[] a) throws Exception {
        Path cfg = Files.createTempDirectory("lf");
        Env.init(cfg, cfg);
        Object[][] songs = {
                {"Lucid Dreams", "Juice WRLD", "Goodbye & Good Riddance", 239_836L},
                {"Lucid Dreams", "Juice WRLD", "Goodbye & Good Riddance", 0L},
                {"SAD!", "XXXTENTACION", "?", 166_605L},
                {"Glimpse of Us", "Joji", "SMITHEREENS", 233_456L},
                {"Sweater Weather", "The Neighbourhood", "I Love You.", 240_400L},
        };
        int ok = 0;
        for (Object[] s : songs) {
            Track t = new Track("id", (String) s[0], (String) s[1], (String) s[2], (Long) s[3], null, "Spotify.exe");
            long t0 = System.currentTimeMillis();
            List<LyricLine> l = LyricsService.fetch(t).get();
            boolean synced = !l.isEmpty() && l.get(0).timeMs() != LyricLine.UNSYNCED;
            String first = l.isEmpty() ? "-" : l.get(Math.min(3, l.size() - 1)).text();
            System.out.println("::notice::" + s[1] + " - " + s[0] + " (dur " + s[3] + "): " + l.size() + " lines, synced=" + synced
                    + ", " + (System.currentTimeMillis() - t0) + " ms, e.g. '" + first + "'");
            if (synced) ok++;
        }
        System.out.println(ok > 0 ? "::notice::lyrics test " + ok + "/" + songs.length : "::error::lyrics test found nothing");
        System.exit(ok > 0 ? 0 : 1);
    }
}
