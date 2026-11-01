package dev.lyricfall;

public final class LyricfallMod {
    public static final Log LOGGER = new Log();

    public static final class Log {
        public void warn(String msg, Object... args) { System.out.println("::warning::" + fmt(msg, args)); }
        public void info(String msg, Object... args) { System.out.println("INFO " + fmt(msg, args)); }

        private static String fmt(String msg, Object... args) {
            StringBuilder sb = new StringBuilder();
            int i = 0, k = 0;
            while (i < msg.length()) {
                if (msg.startsWith("{}", i) && k < args.length) { sb.append(args[k++]); i += 2; }
                else sb.append(msg.charAt(i++));
            }
            return sb.toString();
        }
    }
}
