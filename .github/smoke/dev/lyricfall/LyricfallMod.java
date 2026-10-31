package dev.lyricfall;

/** Stub for the Windows smoke test: only the logger the media helper uses. */
public final class LyricfallMod {
    public static final Log LOGGER = new Log();

    public static final class Log {
        public void warn(String msg, Object... args) { System.out.println("WARN " + msg + " " + java.util.Arrays.toString(args)); }
        public void info(String msg, Object... args) { System.out.println("INFO " + msg + " " + java.util.Arrays.toString(args)); }
    }
}
