# Mutation Testing mit PIT

## Was ist Mutation Testing?

Line Coverage zeigt nur, welcher Code von Tests **ausgeführt** wird, nicht, ob die Tests Fehler
darin auch **erkennen**. Mutation Testing prüft genau das:

1. [PIT](https://pitest.org/) baut kleine, absichtliche Fehler in den Code ein, sogenannte
   **Mutanten**, z.B. `==` wird zu `!=`, ein `return true` wird zu `return false` oder ein
   `println` wird entfernt.
2. Für jeden Mutanten laufen die Tests.
3. Schlägt mindestens ein Test fehl, ist der Mutant **getötet** (killed): Die Tests haben den
   Fehler bemerkt. Bleiben alle Tests grün, hat der Mutant **überlebt** (survived): Es gibt eine
   Lücke in den Tests.

**Mutation Score** = getötete Mutanten / alle Mutanten.

## Einrichtung

In [`build.gradle`](../build.gradle) über das Gradle-Plugin `info.solidsoft.pitest`:

| Einstellung | Wert | Bedeutung |
|---|---|---|
| `pitestVersion` | `1.30.0` | PIT-Version |
| `junit5PluginVersion` | `1.2.3` | damit PIT die JUnit-5-Tests ausführen kann |
| `targetClasses` / `targetTests` | `ch.bbw.m450.tictactoe.*` | welcher Code mutiert und welche Tests verwendet werden |
| `mutators` | `STRONGER` | mehr Mutationsarten als der Standard (u.a. entfernte Bedingungen) |
| `mutationThreshold` | `90` | Build schlägt fehl, wenn der Mutation Score unter 90 % fällt |
| `coverageThreshold` | `90` | Build schlägt fehl, wenn die Line Coverage unter 90 % fällt |

**Lokal ausführen:**

```bash
./gradlew pitest
```

Der Report liegt danach in `build/reports/pitest/index.html` (im Browser öffnen). Dort ist jede
Zeile markiert: grün = alle Mutanten getötet, rot = Mutant überlebt.

**In der Pipeline:** [`.github/workflows/pitest.yml`](../.github/workflows/pitest.yml) läuft bei
jedem Push im DevContainer-Image. Der Mutation Score steht in der Zusammenfassung des Laufs, der
HTML-Report kann als Artefakt `pitest-report` heruntergeladen werden.

## Ergebnis

| | Mutanten | Getötet | Überlebt | Mutation Score | Line Coverage |
|---|---|---|---|---|---|
| Vorher (bestehende Tests) | 96 | 90 | 6 | 94 % | 100 % |
| Nachher (neue Tests) | 96 | 96 | 0 | **100 %** | 100 % |

Obwohl die Line Coverage schon bei 100 % lag, fand PIT 6 Lücken in den Tests.

### Überlebende Mutanten und neue Tests

| Mutant | Warum hat er überlebt? | Neuer Test |
|---|---|---|
| `isWin`: Vergleich `b[7] == b[8]` immer wahr → **X X .** in der unteren Reihe zählt als Sieg | Kein Test prüfte, dass zwei Steine in der unteren Reihe noch kein Sieg sind | `TicTacToeMainTest.twoOfThreeIsNoWin`: jede der 8 Linien mit einem fehlenden Feld, für beide Farben (48 Fälle) |
| `isWin`: Vergleich `b[5] == b[8]` immer wahr → zwei Steine oben in der rechten Spalte zählen als Sieg | dasselbe für die rechte Spalte | ebenfalls `twoOfThreeIsNoWin` |
| `play`: `println` des Gewinners entfernt | Tests prüften nur den Rückgabewert, nicht die Ausgabe | `playPrintsBoardAndWinner` |
| `play`: `println("it's a draw!")` entfernt | dasselbe für Unentschieden | `playPrintsDraw` |
| `play`: `println` des Boards vor einem ungültigen Zug entfernt | Tests prüften nur die Exception | `playPrintsBoardBeforeInvalidMove` |
| `HumanPlayer.play`: Eingabeaufforderung entfernt | Tests prüften nur den gelesenen Zug | `HumanPlayerTest.promptsForMoveWithBoard` |

Damit die Ausgaben geprüft werden können, schneidet die Fixture
[`TicTacToeTestFixtures`](../src/test/java/ch/bbw/m450/tictactoe/TicTacToeTestFixtures.java)
jetzt `System.out` mit (`output()`) und stellt es nach jedem Test wieder her.

### Erkenntnis

Die beiden `isWin`-Mutanten zeigen den Unterschied zwischen Coverage und Mutation Testing: Die
Zeile war ausgeführt, aber nur mit Boards, auf denen eine Linie **komplett** oder **leer** war. Ein
Fehler, bei dem schon zwei von drei Steinen gewinnen, wäre unbemerkt geblieben. Solche
Grenzfälle sind typische Lücken, die nur Mutation Testing aufdeckt.
