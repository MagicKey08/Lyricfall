import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

/** Starts the real media helper script on Windows and prints its first lines (no Minecraft needed). */
public final class Smoke {
    public static void main(String[] a) throws Exception {
        Class<?> c = Class.forName("dev.lyricfall.media.SystemMedia");
        Method launch = c.getDeclaredMethod("launch");
        launch.setAccessible(true);
        Process p = (Process) launch.invoke(null);
        Thread err = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8))) {
                String l; while ((l = r.readLine()) != null) System.out.println("::warning::stderr " + l);
            } catch (Exception ignored) {}
        });
        err.setDaemon(true); err.start();
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
        int ok = 0;
        long end = System.currentTimeMillis() + 60_000;
        String line;
        while (System.currentTimeMillis() < end && (line = in.readLine()) != null) {
            String shown = line.replace('\u001F', '|').replace('\u001E', ';');
            System.out.println("::notice::helper " + shown);
            if (line.startsWith("OK")) { if (++ok >= 3) break; }
            if (line.startsWith("ERR")) break;
        }
        p.destroyForcibly();
        System.out.println(ok >= 3 ? "::notice::smoke OK, helper produced " + ok + " polls" : "::error::smoke failed, polls=" + ok);
        System.exit(ok >= 3 ? 0 : 1);
    }
}
