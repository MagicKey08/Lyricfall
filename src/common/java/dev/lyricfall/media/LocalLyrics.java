package dev.lyricfall.media;

import dev.lyricfall.Env;
import dev.lyricfall.lyrics.LrcParser;
import dev.lyricfall.model.LyricLine;
import dev.lyricfall.model.Track;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Own lyrics for songs that have none online, or a better timed version: put "Artist - Title.lrc" (or just
 * "Title.lrc", .txt works too) into config/lyricfall/lyrics. Own files always win over the internet.
 */
public final class LocalLyrics {
    private LocalLyrics() {}

    public static Path folder() { return Env.configDir().resolve("lyricfall").resolve("lyrics"); }

    public static List<LyricLine> find(Track t) {
        try {
            Path dir = folder();
            Files.createDirectories(dir);
            String full = norm(t.artist() + " - " + t.title());
            String title = norm(t.title());
            Path match = null;
            try (Stream<Path> files = Files.list(dir)) {
                for (Path p : (Iterable<Path>) files::iterator) {
                    String name = p.getFileName().toString();
                    int dot = name.lastIndexOf('.');
                    if (dot <= 0) continue;
                    String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
                    if (!ext.equals("lrc") && !ext.equals("txt")) continue;
                    String base = norm(name.substring(0, dot));
                    if (base.equals(full)) { match = p; break; }
                    if (base.equals(title) && match == null) match = p;
                }
            }
            if (match == null) return null;
            String text = read(match);
            List<LyricLine> synced = LrcParser.parse(text);
            return synced.isEmpty() ? LrcParser.plain(text) : synced;
        } catch (Exception e) {
            return null;
        }
    }

    private static String read(Path p) throws Exception {
        byte[] bytes = Files.readAllBytes(p);
        String s = new String(bytes, StandardCharsets.UTF_8);
        if (s.indexOf('�') >= 0) s = new String(bytes, Charset.forName("windows-1252"));
        if (!s.isEmpty() && s.charAt(0) == '﻿') s = s.substring(1);
        return s;
    }

    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[\\\\/:*?\"<>|]", "").replaceAll("\\s+", " ").trim();
    }
}
