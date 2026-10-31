package dev.lyricfall.media;

import dev.lyricfall.LyricfallMod;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Reads what Windows shows in its media overlay (System Media Transport Controls). Spotify, browsers and most players
 * publish title, artist, album, position and play state there, so no login, token or Premium is needed.
 *
 * WinRT is reached through Windows PowerShell 5.1 (powershell.exe, present on every Windows 10/11). The script runs
 * as one long lived child process and prints one line per poll; Java never blocks the game thread on it.
 * The script exits by itself when the game process is gone, so it can never be left behind.
 */
public final class SystemMedia {
    private SystemMedia() {}

    /** One media session as Windows reports it at the moment of the poll. */
    public record Session(String app, String status, String title, String artist, String album,
                          long positionMs, long durationMs, long ageMs, long readNanos) {
        public boolean playing() { return "Playing".equalsIgnoreCase(status); }

        public boolean isSpotify() { return app != null && app.toLowerCase(Locale.ROOT).contains("spotify"); }

        /** Position right now: Windows only refreshes it on play, pause and seek, the rest is extrapolated. */
        public long positionNow() {
            long p = positionMs;
            if (playing()) p += ageMs + (System.nanoTime() - readNanos) / 1_000_000L;
            if (durationMs > 0) p = Math.min(p, durationMs);
            return Math.max(0, p);
        }
    }

    public enum State { STARTING, RUNNING, UNSUPPORTED, FAILED }

    private static final char FIELD = '\u001F';
    private static final char RECORD = '\u001E';
    private static final long POLL_MS = 350;

    private static volatile List<Session> sessions = List.of();
    private static volatile State state = State.STARTING;
    private static volatile String lastError = "";
    private static volatile long lastLineNanos;
    private static Thread worker;

    public static List<Session> sessions() { return sessions; }

    public static State state() { return state; }

    public static String lastError() { return lastError; }

    public static boolean supported() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public static synchronized void start() {
        if (worker != null) return;
        if (!supported()) {
            state = State.UNSUPPORTED;
            LyricfallMod.LOGGER.warn("Song detection needs Windows (System Media Transport Controls); lyrics stay off.");
            return;
        }
        worker = new Thread(SystemMedia::loop, "lyricfall-media");
        worker.setDaemon(true);
        worker.start();
    }

    private static void loop() {
        long backoff = 2000;
        while (true) {
            Process p = null;
            try {
                p = launch();
                Process proc = p;
                Runtime.getRuntime().addShutdownHook(new Thread(proc::destroyForcibly));
                drainErrors(proc);
                try (BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.startsWith("ERR")) {
                            lastError = line.substring(3).trim();
                            continue;
                        }
                        if (!line.startsWith("OK")) continue;
                        sessions = parse(line.substring(2), System.nanoTime());
                        lastLineNanos = System.nanoTime();
                        state = State.RUNNING;
                        backoff = 2000;
                    }
                }
                int code = proc.waitFor();
                LyricfallMod.LOGGER.warn("Media helper exited with code {} {}", code, lastError);
            } catch (Exception e) {
                lastError = e.getMessage() == null ? e.toString() : e.getMessage();
                LyricfallMod.LOGGER.warn("Media helper failed: {}", lastError);
            } finally {
                if (p != null) p.destroyForcibly();
            }
            sessions = List.of();
            state = State.FAILED;
            try { Thread.sleep(backoff); } catch (InterruptedException e) { return; }
            backoff = Math.min(60_000, backoff * 2);
        }
    }

    /** True when the helper answered recently; a stale reading must not keep lyrics running. */
    public static boolean fresh() {
        return state == State.RUNNING && System.nanoTime() - lastLineNanos < 5_000_000_000L;
    }

    private static Process launch() throws Exception {
        String script = SCRIPT.replace("__PARENT__", Long.toString(ProcessHandle.current().pid()))
                .replace("__POLL__", Long.toString(POLL_MS));
        String encoded = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
        String root = System.getenv("SystemRoot");
        String exe = root != null ? root + "\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" : "powershell.exe";
        if (!new java.io.File(exe).isFile()) exe = "powershell.exe";
        ProcessBuilder pb = new ProcessBuilder(exe, "-NoLogo", "-NoProfile", "-NonInteractive",
                "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-EncodedCommand", encoded);
        pb.redirectInput(ProcessBuilder.Redirect.PIPE);
        return pb.start();
    }

    private static void drainErrors(Process p) {
        Thread t = new Thread(() -> {
            try (BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = err.readLine()) != null) {
                    if (!line.isBlank()) lastError = line.trim();
                }
            } catch (Exception ignored) {
            }
        }, "lyricfall-media-err");
        t.setDaemon(true);
        t.start();
    }

    static List<Session> parse(String body, long readNanos) {
        List<Session> out = new ArrayList<>();
        for (String rec : body.split(String.valueOf(RECORD), -1)) {
            if (rec.isEmpty()) continue;
            String[] f = rec.split(String.valueOf(FIELD), -1);
            if (f.length < 8) continue;
            out.add(new Session(f[0], f[1], f[2], f[3], f[4], num(f[5]), num(f[6]), num(f[7]), readNanos));
        }
        return List.copyOf(out);
    }

    private static long num(String s) {
        try {
            return Math.round(Double.parseDouble(s.trim().replace(',', '.')));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Polls every session. Fields: app id, play state, title, artist, album, position ms, duration ms,
     * milliseconds since Windows last refreshed the position. Separators are ASCII unit and record separators.
     */
    private static final String SCRIPT = String.join("\n",
            "$ErrorActionPreference = 'Stop'",
            "[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding $false",
            "$parent = __PARENT__",
            "$US = [char]0x1F; $RS = [char]0x1E",
            "try {",
            "  Add-Type -AssemblyName System.Runtime.WindowsRuntime",
            "  $asTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]",
            "  function Await($op, [Type]$type) { $t = $asTask.MakeGenericMethod($type).Invoke($null, @($op)); [void]$t.Wait(3000); if ($t.IsCompleted) { return $t.Result } else { return $null } }",
            "  [void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]",
            "  [void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties,Windows.Media.Control,ContentType=WindowsRuntime]",
            "  $mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])",
            "} catch { [Console]::Out.WriteLine('ERR ' + $_.Exception.Message); [Console]::Out.Flush(); exit 2 }",
            "if ($mgr -eq $null) { [Console]::Out.WriteLine('ERR media manager unavailable'); [Console]::Out.Flush(); exit 3 }",
            "function Clean([string]$s) { if ($s -eq $null) { return '' }; return ($s -replace '[\\x00-\\x1F]', ' ') }",
            "$cache = @{}",
            "$ErrorActionPreference = 'SilentlyContinue'",
            "while ($true) {",
            "  if (-not (Get-Process -Id $parent -ErrorAction SilentlyContinue)) { exit 0 }",
            "  $sb = New-Object System.Text.StringBuilder",
            "  try {",
            "    foreach ($s in $mgr.GetSessions()) {",
            "      $app = [string]$s.SourceAppUserModelId",
            "      $pb = $s.GetPlaybackInfo()",
            "      $tl = $s.GetTimelineProperties()",
            "      $key = $app + '|' + $tl.EndTime.Ticks + '|' + $tl.StartTime.Ticks",
            "      $now = [DateTime]::UtcNow",
            "      $c = $cache[$app]",
            "      if ($c -eq $null -or $c.Key -ne $key -or ($now - $c.At).TotalMilliseconds -gt 1500) {",
            "        $props = Await ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])",
            "        if ($props -ne $null) { $c = @{ Key = $key; At = $now; Title = $props.Title; Artist = $props.Artist; Album = $props.AlbumTitle }; $cache[$app] = $c }",
            "      }",
            "      if ($c -eq $null) { continue }",
            "      $pos = ($tl.Position - $tl.StartTime).TotalMilliseconds",
            "      $dur = ($tl.EndTime - $tl.StartTime).TotalMilliseconds",
            "      $age = ([DateTimeOffset]::UtcNow - $tl.LastUpdatedTime).TotalMilliseconds",
            "      if ($age -lt 0 -or $age -gt 86400000) { $age = 0 }",
            "      $inv = [Globalization.CultureInfo]::InvariantCulture",
            "      [void]$sb.Append((Clean $app)).Append($US).Append([string]$pb.PlaybackStatus).Append($US).Append((Clean $c.Title)).Append($US).Append((Clean $c.Artist)).Append($US).Append((Clean $c.Album)).Append($US).Append($pos.ToString('0', $inv)).Append($US).Append($dur.ToString('0', $inv)).Append($US).Append($age.ToString('0', $inv)).Append($RS)",
            "    }",
            "  } catch { [Console]::Out.WriteLine('ERR ' + $_.Exception.Message) }",
            "  [Console]::Out.WriteLine('OK' + $sb.ToString())",
            "  [Console]::Out.Flush()",
            "  Start-Sleep -Milliseconds __POLL__",
            "}");
}
