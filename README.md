# Lyricfall

Die Lyrics von dem Song, den du gerade in Spotify hörst, fliegen synchron durch deine Minecraft-Welt. Wort für Wort, genau dann, wenn es gesungen wird.

Fabric Mod für **Minecraft Java 26.3**. Läuft nur auf dem Client, funktioniert also auf jedem Server (auch Nitrado, Realms usw.). Der Server und deine Freunde brauchen die Mod nicht.

Lyricfall ist ein abgespeckter Fork von [Overtone](https://github.com/chifir4ik/Overtone) (MIT). Übrig sind nur die Lyrics: kein eigener Musikplayer, keine Logins, kein Visualizer, keine Kinokamera, kein Discord-Status.

## Was die Mod kann

**Fliegende Lyrics in der Welt** (Taste `J` an/aus)
* Jedes Wort fliegt herein, wenn es gesungen wird, das gerade gesungene Wort leuchtet in der Akzentfarbe
* Modus: beim Spieler (folgt dir) oder an der Stelle der Zeile (bleibt stehen, du läufst daran vorbei)
* Mitbewegen: immer vor deinen Augen oder fest in der Welt
* Flüssige Bewegung: der Text zieht nicht hinterher, bleibt beim Seitwärtslaufen ruhig stehen, richtet sich auf kurvigen Wegen sanft neu aus und fliegt mit der Elytra vor dir her (mit Neigung und mehr Abstand bei hohem Tempo)
* In Kurven neigen: leichte Schräglage beim Drehen, rollt mit [Do a Barrel Roll](https://modrinth.com/mod/do-a-barrel-roll) automatisch mit
* Wörter in einer Reihe oder verstreut um dich herum
* 8 Animationen: Heranfliegen, Aufsteigen, Fallen und hüpfen, Ploppen, Welle, Schreibmaschine, Drehen, Glitch
* Animieren nach Wörtern oder Buchstaben, Tempo, Schwingen, Verzögerung zwischen Buchstaben
* 7 Arten zu verschwinden: Ausblenden, auf den Boden fallen (Buchstaben bleiben auf Blöcken liegen), Zerspringen, Davonschweben, Wegwehen, Schrumpfen, Versinken
* Farben: **Albumcover** (Farben aus dem Cover des Songs, weiche Überblendung beim Songwechsel), Klassisch, Hauptfarbe, Verlauf, Regenbogen, Weiß, plus Leuchten mit Stärke und Größe
* Kontur für gute Lesbarkeit auf Schnee und hellem Himmel, lange Töne wachsen und steigen beim Singen leicht an
* Größe, Abstand, Höhe, Bereich, Schatten, durch Blöcke sichtbar oder nicht
* Vorschau-Knopf, der sofort eine Beispielzeile vor dir zeigt

**Lyrics auf dem Bildschirm** (optional)
* Untertitel (aktuelle und nächste Zeile) oder Karaoke (3 bis 9 scrollende Zeilen)
* Karaoke Wort für Wort, Animationen beim Erscheinen und Verschwinden
* Position, Größe, Breite, Hintergrund frei einstellbar
* Standardmäßig ausgeblendet, solange die fliegenden Lyrics laufen (Schalter "Untertitel zusätzlich zu Welt-Lyrics")

**Lyrics-Quellen**
* AMLL TTML DB (wortgenaue Zeitmarken), LRCLIB, optional KuGou
* Passt Titel, Künstler und Länge ab, damit keine falschen Lyrics kommen
* Eigene Lyrics: `Künstler - Titel.lrc` (oder `.txt`) in `config/lyricfall/lyrics` legen, die haben immer Vorrang
* Regler "Lyrics verschieben", falls die Lyrics zu früh oder zu spät kommen

## Woher weiß die Mod, was läuft?

Über die Windows-Medienanzeige (die Box, die beim Lautstärke ändern erscheint). Spotify meldet dort Titel, Künstler, Position und ob gerade gespielt wird. Dafür braucht es **kein Spotify-Login und kein Premium**, nur die Spotify Desktop-App unter Windows 10 oder 11. In den Einstellungen kannst du auch andere Apps erlauben (z.B. YouTube im Browser).

## Installation

1. [Fabric Loader](https://fabricmc.net/use/) für 26.3 und [Fabric API](https://modrinth.com/mod/fabric-api) installieren
2. Die `lyricfall-*.jar` aus den [Releases](https://github.com/MagicKey08/Lyricfall/releases) (oder aus dem letzten [Actions-Build](https://github.com/MagicKey08/Lyricfall/actions)) in deinen `mods` Ordner legen
3. Spotify starten, Minecraft starten, Song abspielen

## Tasten

| Taste | Aktion |
|---|---|
| `J` | Fliegende Lyrics an / aus |
| `K` | Einstellungen |
| frei | Bildschirm-Lyrics an / aus |
| frei | Lyrics für den aktuellen Song neu suchen |

Im Tab "Allgemein" siehst du ganz oben, welchen Song die Mod gerade erkennt und ob Lyrics gefunden wurden.

## Selbst bauen

```
./gradlew buildAll
```

Die Jar landet in `dist/`. Gradle lädt JDK 25 bei Bedarf selbst.

## Lizenz

MIT, siehe [LICENSE](LICENSE). Basiert auf Overtone von chifir4ik und PabJlo, Render-Layer nach SpotifyMusicMod von Kart-Modding. Details in [THIRD_PARTY_NOTICES.txt](THIRD_PARTY_NOTICES.txt).
