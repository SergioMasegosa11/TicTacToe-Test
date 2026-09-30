# Testkonzept – TicTacTest

## 1. Ziel des Testkonzepts

Das Testkonzept beschreibt den aktuellen Stand der automatisierten Tests im TicTacToe-Projekt.

Der Fokus liegt auf der Methode `TicTacToeMain.isWin()`. Es wird überprüft, ob ein Spieler bei einer gültigen Gewinnkombination korrekt als Gewinner erkannt wird und ob bei Spielfeldern ohne Gewinner kein Gewinner erkannt wird.

Das Testkonzept beschreibt ausschließlich den aktuellen IST-Zustand des Projekts.

## 2. Teststrategie

Für das Projekt werden automatisierte Unit-Tests mit **JUnit 5** und **AssertJ** verwendet.

Die Tests prüfen die Methode `TicTacToeMain.isWin()` unabhängig vom restlichen Spielablauf. Dadurch wird die Gewinnerkennung gezielt getestet.

Für mehrere ähnliche Spielsituationen werden parametrisierte Tests verwendet. Die verschiedenen Spielfeld-Konstellationen werden über `@MethodSource` bereitgestellt.

Aktuell gibt es zwei Testmethoden:

- `playerWins()` – prüft verschiedene Gewinnsituationen.
- `nobodyWins()` – prüft Spielfelder ohne Gewinner.

## 3. Teststruktur

Die Tests befinden sich in der Klasse:

`TicTacToeMainTest`

Verwendete JUnit-Elemente:

| Element | Verwendung |
|---|---|
| `@BeforeEach` | Erstellt vor jedem Test ein neues Spielfeld |
| `@ParameterizedTest` | Führt einen Test mit mehreren Testdaten aus |
| `@MethodSource` | Liefert die verschiedenen Spielfeld-Konstellationen |
| `Stream<Arguments>` | Enthält die Testdaten |
| AssertJ `assertThat()` | Überprüft die erwarteten Ergebnisse |

Für die Erstellung der Testdaten werden Hilfsmethoden verwendet:

- `board(...)` – erstellt ein Spielfeld aus den angegebenen Steinen.
- `emptyBoard()` – erstellt ein leeres 9-Felder-Spielfeld.
- `winningBoards()` – enthält verschiedene Gewinnsituationen.
- `drawBoards()` – enthält verschiedene Spielfelder ohne Gewinner.

## 4. Testziele

### TZ-01 – Gewinn eines Spielers erkennen

Es soll überprüft werden, dass `TicTacToeMain.isWin()` einen Spieler korrekt als Gewinner erkennt, wenn dieser drei Steine in einer gültigen Gewinnkombination besitzt.

Aktuell werden folgende Situationen getestet:

- CROSS gewinnt horizontal.
- CIRCLE gewinnt horizontal.
- CROSS gewinnt vertikal.
- CIRCLE gewinnt vertikal.
- CROSS gewinnt diagonal.
- CIRCLE gewinnt diagonal.

**Erwartetes Ergebnis:**  
`isWin()` liefert `true`.

**Zugehöriger Test:**  
`playerWins()`

### TZ-02 – Kein Gewinner bei einem Unentschieden erkennen

Es soll überprüft werden, dass `TicTacToeMain.isWin()` keinen Gewinner meldet, wenn das Spielfeld vollständig belegt ist, aber keine Gewinnkombination vorhanden ist.

Dabei wird das Spielfeld jeweils für beide Spieler geprüft.

**Erwartetes Ergebnis:**  
Für CROSS und CIRCLE liefert `isWin()` jeweils `false`.

**Zugehöriger Test:**  
`nobodyWins()`

## 5. Testfallübersicht

| Test-ID | Testziel | Testmethode | Testdaten | Erwartetes Ergebnis |
|---|---|---|---|---|
| TC-01 | TZ-01 | `playerWins()` | CROSS gewinnt horizontal | `true` |
| TC-02 | TZ-01 | `playerWins()` | CIRCLE gewinnt horizontal | `true` |
| TC-03 | TZ-01 | `playerWins()` | CROSS gewinnt vertikal | `true` |
| TC-04 | TZ-01 | `playerWins()` | CIRCLE gewinnt vertikal | `true` |
| TC-05 | TZ-01 | `playerWins()` | CROSS gewinnt diagonal | `true` |
| TC-06 | TZ-01 | `playerWins()` | CIRCLE gewinnt diagonal | `true` |
| TC-07 | TZ-02 | `nobodyWins()` | Unentschieden ohne Gewinner | CROSS=`false`, CIRCLE=`false` |
| TC-08 | TZ-02 | `nobodyWins()` | Unentschieden ohne Gewinner | CROSS=`false`, CIRCLE=`false` |
| TC-09 | TZ-02 | `nobodyWins()` | Unentschieden ohne Gewinner | CROSS=`false`, CIRCLE=`false` |
| TC-10 | TZ-02 | `nobodyWins()` | Unentschieden ohne Gewinner | CROSS=`false`, CIRCLE=`false` |

Durch die parametrisierten Tests entstehen insgesamt **10 Testausführungen**:

- 6 Gewinnfälle
- 4 Fälle ohne Gewinner

## 6. Aktueller Testumfang

Der aktuelle Testumfang konzentriert sich auf die Gewinnerkennung der Methode `TicTacToeMain.isWin()`.

Im vorhandenen Testcode werden derzeit keine Tests für folgende Bereiche durchgeführt:

- Spielzüge setzen
- ungültige Spielzüge
- bereits belegte Felder
- Spielerwechsel
- Benutzereingaben
- kompletter Spielablauf

Diese Bereiche sind deshalb nicht Bestandteil des aktuellen Testumfangs.

> **Update:** Inzwischen werden diese Bereiche abgedeckt: durch die erweiterten Unit-Tests
> (Line-Coverage 100 %, Mutation Score mit PIT siehe [docs/mutation-testing.md](docs/mutation-testing.md))
> und durch die End-to-End-Tests in [Kapitel 9](#9-end-to-end-tests).



## 7. Test Coverage (JaCoCo)

Die Test Coverage wird mit **JaCoCo** gemessen (`./gradlew test jacocoTestReport`).

| Workflow | Auslöser | Zweck |
|---|---|---|
| `jaCoCo.yml` | jeder Push (alle Branches) | Tests + HTML-Coverage-Report als GitHub Actions Artifact |
| `coverage-pages.yml` | Push auf `main` | Coverage-Wert zur Historie hinzufügen, Time-Series auf GitHub Pages |
| `coverage-gate.yml` | Pull Request / manuell | Branch-Coverage darf nicht tiefer sein als `main` (PASS/FAIL + PR-Kommentar) |

Gemessen wird die **Line-Coverage**. Details zum Design: [docs/coverage-design.md](docs/coverage-design.md).

Time-Series: https://sergiomasegosa11.github.io/TicTacToe-Test/

## 8. Feedback von Auditor(Cristian)
Grundsätzlich alles Gut eventuell mehr Tests schreiben, ansonsten alles gut erklärt und es ist übersichtlich.
Erhaltene Note: 5.5

Das Coverage Gate wird bei jedem Pull Request automatisch ausgeführt.

## 9. End-to-End-Tests

### 9.1 Ziel und Umfang

Die End-to-End-Tests (E2E) spielen das komplette Spiel so, wie ein Benutzer es in der Konsole
spielt: von der ersten Eingabe bis zum Resultat. Getestet wird über den echten Einstiegspunkt
`TicTacToeMain.main()`, mit Eingaben über `System.in` und Prüfung der Ausgabe auf `System.out`.

- **Spieler X:** der Mensch (`HumanPlayer`, liest von der Tastatur)
- **Spieler O:** der Computer (`RandomPlayer`, wählt ein zufälliges freies Feld; bis BUG-06 war es der `GreedyPlayer`, der immer das tiefste freie Feld nahm)

Getestet werden vier Bereiche:

1. kompletter Spielablauf (Sieg X, Sieg O, Unentschieden)
2. ungültige Eingaben
3. böswillige Eingaben
4. Verhalten, das zu einem Denial of Service (DoS) führen kann

### 9.2 Umsetzung

| Thema | Umsetzung |
|---|---|
| Testklasse | [`src/e2eTest/java/.../e2e/TicTacToeE2ETest.java`](src/e2eTest/java/ch/bbw/m450/tictactoe/e2e/TicTacToeE2ETest.java) |
| Eigenes Source-Set | `src/e2eTest/java`, getrennt von den Unit-Tests in `src/test/java` |
| Gradle-Task | `./gradlew e2eTest` (läuft auch bei `./gradlew check` und `./gradlew build`) |
| CI-Pipeline | [`.github/workflows/e2e.yml`](.github/workflows/e2e.yml): bei jedem Push und Pull Request, im DevContainer-Image, Testreport als Artefakt `e2e-test-report` |
| JUnit Pioneer `@StdIo` + `StdOut` | ersetzt `System.in` durch feste Eingabezeilen und schneidet `System.out` mit |
| JUnit Pioneer `@CartesianTest` | kombiniert z.B. jede ungültige Eingabe mit jedem Zeitpunkt im Spiel (vor Zug 1, 2 oder 3) |
| JUnit Pioneer `@Issue` | verknüpft einen Test mit dem gefundenen Fehler (BUG-xx, siehe 9.4) |
| JUnit Pioneer `@SetSystemProperty` | setzt `tictactest.seed=3` für die ganze Testklasse: Der zufällige Gegner spielt dadurch in jedem Lauf gleich, die Tests sind reproduzierbar |
| Timeouts | `@Timeout(5 s, threadMode = SEPARATE_THREAD)` auf der ganzen Klasse: Jeder Test läuft in einem eigenen Thread. Hängt das Spiel (Endlosschleife, blockierendes Lesen), schlägt der Test nach 5 s fehl, statt den Build zu blockieren. Zusätzlich `timeout-minutes: 10` im CI-Job |
| Helper `GameRunner` | für Eingaben, die sich nicht als Konstante in `@StdIo` angeben lassen, z.B. ein endloser Eingabe-Stream |

### 9.3 Testfälle

„Vorher“ ist das tatsächliche Verhalten des ursprünglichen Codes, „Nachher“ das Verhalten nach
den Korrekturen. Bei ungültigen Eingaben ist das Spiel danach jeweils mit `3, 4, 5` fortgesetzt
worden (X gewinnt).

#### Kompletter Spielablauf

| ID | Testfall | Eingabe | Erwartet | Vorher | Nachher |
|---|---|---|---|---|---|
| E2E-01 | X gewinnt | `3, 4, 5` | `...and the winner is: CROSS`, 3 Eingabeaufforderungen | wie erwartet | ✅ |
| E2E-02 | O gewinnt | `0, 1, 3` (Seed 3) | `...and the winner is: CIRCLE` | wie erwartet | ✅ |
| E2E-03 | Unentschieden | `0, 2, 3, 5, 7` (Seed 3) | `it's a draw!` nach 9 Zügen | wie erwartet | ✅ |
| E2E-04 | Eingabeaufforderung | `3, 4, 5` | Board wird angezeigt, Text `where to put the next CROSS? (0-8):` | Tippfehler `where to to put` (BUG-05) | ✅ |
| E2E-05 | O spielt zufällig | 20 Spiele mit verschiedenen Seeds, X spielt `4` | O wählt verschiedene freie Felder (mind. 4 verschiedene erste Züge), nie ein besetztes | O spielte **immer Feld 0**, danach 1, 2, … (BUG-06) | ✅ |
| E2E-06 | echtes Zufallsspiel ohne Seed | X probiert `0` bis `8` der Reihe nach | Spiel endet mit Sieg oder Unentschieden, kein Abbruch | – (neu) | ✅ |

#### Ungültige Eingaben

| ID | Testfall | Eingabe | Erwartet | Vorher | Nachher |
|---|---|---|---|---|---|
| E2E-10 | Text | `abc`, `vier` | Meldung `invalid input, please enter a free field (0-8)`, erneute Eingabe, Spiel geht weiter | `NumberFormatException`, Spiel stürzt ab (BUG-01) | ✅ |
| E2E-10 | leere Eingabe | ``, `   ` | wie oben | `NumberFormatException`, Absturz (BUG-01) | ✅ |
| E2E-10 | ausserhalb des Feldes | `9`, `-1`, `100` | wie oben | `IllegalStateException: cannot play to position 9`, Absturz (BUG-01) | ✅ |
| E2E-10 | Kommazahl, Überlauf | `4.5`, `99999999999` | wie oben | `NumberFormatException`, Absturz (BUG-01) | ✅ |
| E2E-11 | besetztes Feld | `3`, dann `3` (eigenes) und `6` (von O, Seed 3) | klare Meldung `field 3 is already taken by CROSS, please choose a free field` bzw. `… by CIRCLE …`, Spiel geht weiter | `IllegalStateException: cannot play to position 3`, Absturz (BUG-01). Nach dem ersten Fix nur die allgemeine Meldung `invalid input …` (BUG-07) | ✅ |
| E2E-12 | Leerzeichen/Tab um die Zahl | ` 3`, `4 `, `\t5` | wird als gültige Zahl akzeptiert | `NumberFormatException` bei ` 3`, Absturz (BUG-01) | ✅ |

E2E-10 läuft als `@CartesianTest`: 9 ungültige Eingaben × 3 Zeitpunkte = 27 Testausführungen.

#### Böswillige Eingaben

Erwartet ist jeweils: Die Eingabe wird abgelehnt, **nicht** auf der Konsole ausgegeben, und das
Spiel geht weiter.

| ID | Eingabe | Gefahr | Vorher | Nachher |
|---|---|---|---|---|
| E2E-20 | `ESC[2J ESC[H` | Terminal-Injection: Bildschirm löschen | Absturz, die Escape-Sequenz steht im Stacktrace und wird vom Terminal ausgeführt (BUG-02) | ✅ |
| E2E-20 | `ESC]0;hacked BEL` | Terminal-Injection: Fenstertitel ändern | wie oben (BUG-02) | ✅ |
| E2E-20 | `%s%n%x` | Format-String | Absturz (BUG-01) | ✅ |
| E2E-20 | `4\0 4` (Null-Byte) | Null-Byte-Injection | Absturz (BUG-01) | ✅ |
| E2E-20 | `٤` (arabische Ziffer 4) | unerwartete Unicode-Ziffer | **als Feld 4 akzeptiert**, weil `Integer.parseInt` alle Unicode-Ziffern kennt (BUG-02) | ✅ |
| E2E-20 | `+4`, `04` | alternative Zahlenformate | **als Feld 4 akzeptiert** (BUG-02) | ✅ |
| E2E-20 | `4; rm -rf /` | Shell-Injection | Absturz (BUG-01) | ✅ |
| E2E-20 | `${jndi:ldap://x/a}` | Log4Shell-Muster | Absturz, Eingabe im Stacktrace (BUG-01, BUG-02) | ✅ |

#### Denial of Service

| ID | Testfall | Eingabe | Erwartet | Vorher | Nachher |
|---|---|---|---|---|---|
| E2E-30 | keine Eingabe (EOF) | leerer Stream | Meldung `game aborted: no more input`, kein Absturz | `NoSuchElementException`, Absturz (BUG-03) | ✅ |
| E2E-31 | Eingabe endet mitten im Spiel | `3, 4`, dann EOF | wie oben | `NoSuchElementException`, Absturz (BUG-03) | ✅ |
| E2E-32 | endlos ungültige Eingaben | `x`, `x`, `x`, … (endlos) | nach 10 Versuchen `game aborted: too many invalid inputs` | Absturz beim 1. `x`. Nach einer naiven Korrektur („einfach erneut fragen“) wäre es eine **Endlosschleife** (BUG-04) | ✅ |
| E2E-33 | endlose Zeile ohne Zeilenende | `999999…` (endlos) | Abbruch nach begrenzter Zeit, kein OutOfMemory | **Spiel hängt**: Der `Scanner` puffert die ganze Zeile, Test nach 5 s Timeout abgebrochen, auf Dauer OutOfMemory (BUG-04) | ✅ |
| E2E-34 | sehr lange Eingabe | 1 MB lange Zeile | schnell abgelehnt | Absturz, 1 MB Text im Stacktrace (BUG-01) | ✅ |

**Resultat vor der Korrektur:** 3 von 47 Testausführungen grün (nur die gültigen Spielabläufe).
**Resultat nach der Korrektur:** 47 von 47 grün (mit BUG-06/07 kamen E2E-05 und E2E-06 dazu: 49 von 49), jede DoS-Prüfung dauert unter 0,1 s.

### 9.4 Gefundene Fehler

| ID | Fehler | Schwere | Korrektur |
|---|---|---|---|
| BUG-01 | Jede ungültige Eingabe (Text, leer, ausserhalb 0–8, besetztes Feld, Leerzeichen) beendet das Spiel mit einer Exception und Stacktrace. | hoch | `HumanPlayer` prüft die Eingabe, meldet `invalid input, please enter a free field (0-8)` und fragt erneut. Führende und folgende Leerzeichen werden entfernt. |
| BUG-02 | Die Eingabe wird ungefiltert verarbeitet: `+4`, `04` und Unicode-Ziffern wie `٤` werden als Zahl akzeptiert, und im Fehlerfall landet die Eingabe samt Terminal-Escape-Sequenzen im Stacktrace. | mittel | Erlaubt ist nur genau eine ASCII-Ziffer `[0-8]` (Regex). Die Eingabe wird nie auf der Konsole ausgegeben. |
| BUG-03 | Ende der Eingabe (EOF, z.B. Ctrl+D oder `< /dev/null`) führt zu `NoSuchElementException`. | mittel | `HumanPlayer` meldet das Ende der Eingabe. `main()` fängt den Abbruch ab und gibt `game aborted: no more input` aus. |
| BUG-04 | Keine Begrenzung der Eingabe: Eine endlose Zeile ohne Zeilenende lässt den `Scanner` unbegrenzt Speicher belegen (Hänger/OutOfMemory). Endlos ungültige Eingaben würden nach einer naiven Korrektur zur Endlosschleife. | hoch (DoS) | Höchstens 10 ungültige Versuche (`MAX_ATTEMPTS`), danach `game aborted: too many invalid inputs`. Eine Zeile wird mit eigenem Lesen auf 17 Zeichen gekürzt, vom Rest werden höchstens 4096 Zeichen übersprungen (`MAX_LINE_LENGTH`, `MAX_SKIPPED`). |
| BUG-05 | Tippfehler in der Eingabeaufforderung: `where to to put the next …` | tief | Text korrigiert: `where to put the next …` |
| BUG-06 | Der Computer-Gegner ist vorhersehbar: Der `GreedyPlayer` nimmt immer das tiefste freie Feld (0, 1, 2, …). Man gewinnt mit immer denselben Zügen. | mittel | Neuer `RandomPlayer` wählt zufällig eines der freien Felder und ersetzt den `GreedyPlayer` im Spiel. Für reproduzierbare Tests kann der Zufall mit `-Dtictactest.seed=<zahl>` fest gestartet werden. |
| BUG-07 | Bei einem bereits besetzten Feld kam nur die allgemeine Meldung `invalid input, please enter a free field (0-8)`. Der Spieler erfährt nicht, dass das Feld besetzt ist und von wem. | tief | Eigene Meldung `field 4 is already taken by CROSS, please choose a free field`. Die Feldnummer darf ausgegeben werden, weil sie vorher als genau eine Ziffer 0–8 geprüft wurde. |

### 9.5 Test-first

Alle Fehler wurden **test-first** behoben:

1. Die E2E-Tests wurden zuerst geschrieben. Sie beschreiben das gewünschte Verhalten und wurden
   gegen den unveränderten Code ausgeführt: **44 von 47 rot**. Das ist im Commit
   *„E2E-Tests mit JUnit Pioneer (test-first, noch rot)“* festgehalten.
2. Das tatsächliche Verhalten aus den Fehlermeldungen wurde in der Spalte „Vorher“ dokumentiert.
3. Erst danach wurden `HumanPlayer` und `TicTacToeMain.main()` korrigiert, bis alle Tests grün
   waren.
4. Die bestehenden Unit-Tests in `HumanPlayerTest`, die das alte Verhalten (Exception) erwartet
   hatten, wurden an das neue Verhalten angepasst und um Grenzwerttests ergänzt, z.B. genau
   10 Versuche oder genau die maximale Zeilenlänge. So bleiben Line-Coverage (100 %) und
   Mutation Score (99 %) hoch.

**Hinweis zu PIT:** Ein überlebender Mutant ist *äquivalent*. In `HumanPlayer.readLine()` kann
die gekürzte Zeile durch `""` ersetzt werden, ohne dass sich das Verhalten ändert, denn beides
ist eine ungültige Eingabe. Solche Mutanten kann kein Test töten.

### 9.6 Weitere Testfälle, die nicht automatisiert abgedeckt werden

| Testfall | Warum nicht automatisiert |
|---|---|
| Farben und Fettschrift werden im echten Terminal richtig dargestellt (Windows-Konsole, PowerShell, Linux-Terminal, IntelliJ) | Die Darstellung der ANSI-Codes hängt vom Terminal ab. Geprüft werden kann nur, dass die Codes ausgegeben werden, nicht wie sie aussehen. |
| Das Spiel ist verständlich und angenehm zu bedienen (Usability) | subjektiv, nur durch echte Benutzer beurteilbar |
| Ctrl+C während der Eingabe beendet das Spiel sauber | Signale an den Prozess lassen sich in JUnit nicht realistisch auslösen |
| Eingabe bleibt offen, aber der Benutzer tippt nie etwas | Das Spiel wartet absichtlich unbegrenzt auf den Menschen. Ein Test würde nur den Timeout prüfen, das ist gewolltes Verhalten und kein Fehler. |
| Umlaute und Sonderzeichen in der Konsole (Zeichensatz der Windows-Konsole) | hängt von der Codepage des Systems ab (`chcp`) |
