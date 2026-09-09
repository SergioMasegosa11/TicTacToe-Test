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
| TC-11 | TZ-02 | `nobodyWins()` | Unentschieden ohne Gewinner | CROSS=`false`, CIRCLE=`false` |

Durch die parametrisierten Tests entstehen insgesamt **11 Testausführungen**:

- 6 Gewinnfälle
- 5 Fälle ohne Gewinner

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
