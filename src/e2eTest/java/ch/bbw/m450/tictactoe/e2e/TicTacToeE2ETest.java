package ch.bbw.m450.tictactoe.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.Issue;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Values;

import ch.bbw.m450.tictactoe.TicTacToeMain;

/**
 * End-to-End-Tests: das komplette Spiel wird wie von einem Benutzer gespielt.
 * Mensch (X, Eingabe ueber stdin) gegen RandomPlayer (O, waehlt ein zufaelliges freies Feld).
 * Damit die Spiele reproduzierbar sind, wird der Zufall mit einem festen Seed gestartet
 * (@SetSystemProperty): mit Seed 3 spielt O auf die Eingabe 3, 4 hin die Felder 6 und 2.
 * Die Test-IDs (E2E-xx) und gefundenen Fehler (BUG-xx) sind im Testkonzept dokumentiert.
 * <p>
 * Jeder Test laeuft in einem eigenen Thread mit Timeout: haengt das Spiel (z.B. Endlosschleife
 * oder blockierendes Lesen), schlaegt der Test fehl, statt den Build zu blockieren.
 */
@Timeout(value = 5, unit = TimeUnit.SECONDS, threadMode = ThreadMode.SEPARATE_THREAD)
@SetSystemProperty(key = TicTacToeMain.SEED_PROPERTY, value = TicTacToeE2ETest.SEED)
class TicTacToeE2ETest {

    static final String SEED = "3";

    private static final String PROMPT = "? (0-8): ";
    private static final String INVALID = "invalid input, please enter a free field (0-8)";
    private static final String X_WINS = "...and the winner is: CROSS";

    /** Arabisch-indische Ziffer 4 (U+0664), als Konstante damit sie in Annotationen verwendet werden kann */
    private static final String ARABIC_FOUR = "" + (char) 0x0664;

    private static int count(String text, String part) {
        return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }

    @Nested
    @DisplayName("Kompletter Spielablauf")
    class Spielablauf {

        @Test
        @DisplayName("E2E-01: X gewinnt mit der mittleren Reihe")
        @StdIo({"3", "4", "5"})
        void xWins(StdOut out) {
            TicTacToeMain.main(new String[0]);

            assertThat(out.capturedString()).endsWith(X_WINS + System.lineSeparator());
            assertThat(count(out.capturedString(), PROMPT)).as("drei Eingabeaufforderungen").isEqualTo(3);
        }

        @Test
        @DisplayName("E2E-02: O gewinnt")
        @StdIo({"0", "1", "3"})
        void oWins(StdOut out) {
            TicTacToeMain.main(new String[0]);

            assertThat(out.capturedString()).endsWith("...and the winner is: CIRCLE" + System.lineSeparator());
        }

        @Test
        @DisplayName("E2E-03: Unentschieden nach 9 Zuegen")
        @StdIo({"0", "2", "3", "5", "7"})
        void draw(StdOut out) {
            TicTacToeMain.main(new String[0]);

            assertThat(out.capturedString()).endsWith("it's a draw!" + System.lineSeparator());
            assertThat(count(out.capturedString(), PROMPT)).as("fuenf Eingabeaufforderungen").isEqualTo(5);
        }

        @Test
        @Issue("BUG-05")
        @DisplayName("E2E-04: Eingabeaufforderung zeigt Board und Farbe")
        @StdIo({"3", "4", "5"})
        void promptShowsBoardAndColour(StdOut out) {
            TicTacToeMain.main(new String[0]);

            assertThat(out.capturedString())
                    .startsWith(TicTacToeMain.toString(new ch.bbw.m450.tictactoe.TicTacToePlayer.Stone[9]))
                    .contains("where to put the next CROSS" + PROMPT);
        }
    }

    @Nested
    @DisplayName("Zufaelliger Computer-Gegner")
    class ZufaelligerGegner {

        /** Feld, auf das O nach dem ersten Zug von X gespielt hat (aus dem zweiten angezeigten Board gelesen). */
        private int firstMoveOfO(String output) {
            var withoutColours = output.replaceAll("\033\\[[0-9;]*m", "");
            var secondBoard = withoutColours.split("where to put the next CROSS\\? \\(0-8\\): ")[1];
            var cells = List.of(secondBoard.strip().split("\\s+")).subList(0, 9);
            return cells.indexOf("O");
        }

        @Test
        @Issue("BUG-06")
        @DisplayName("E2E-05: O waehlt zufaellige freie Felder statt immer das tiefste")
        void computerPlaysRandomFreeFields() {
            var firstMoves = new HashSet<Integer>();
            try {
                for (var game = 1; game <= 20; game++) {
                    // weit auseinanderliegende Seeds, damit java.util.Random gut streut
                    System.setProperty(TicTacToeMain.SEED_PROPERTY, String.valueOf(game * 1_000_003L));
                    var output = GameRunner.play("4");

                    var move = firstMoveOfO(output);
                    assertThat(move).as("O darf nicht auf das besetzte Feld 4 spielen").isNotEqualTo(4).isBetween(0, 8);
                    firstMoves.add(move);
                }
            } finally {
                System.setProperty(TicTacToeMain.SEED_PROPERTY, SEED);
            }

            // frueher (GreedyPlayer) war der erste Zug von O immer Feld 0
            assertThat(firstMoves).as("verschiedene erste Zuege von O").hasSizeGreaterThanOrEqualTo(4);
        }

        @Test
        @ClearSystemProperty(key = TicTacToeMain.SEED_PROPERTY)
        @DisplayName("E2E-06: ohne Seed laeuft ein echtes Zufallsspiel bis zum Ende")
        void gameWithoutSeedEndsNormally() {
            // X spielt alle Felder der Reihe nach, ungueltige (besetzte) Felder werden einfach abgelehnt
            var output = GameRunner.play("0", "1", "2", "3", "4", "5", "6", "7", "8");

            assertThat(output).containsAnyOf("...and the winner is: CROSS", "...and the winner is: CIRCLE", "it's a draw!");
            assertThat(output).doesNotContain("game aborted");
        }
    }

    @Nested
    @DisplayName("Ungueltige Eingaben")
    class UngueltigeEingaben {

        @Issue("BUG-01")
        @CartesianTest(name = "E2E-10: \"{0}\" vor Zug {1} wird abgelehnt, das Spiel geht weiter")
        void invalidInputIsRejected(
                @Values(strings = {"abc", "", "   ", "9", "-1", "100", "4.5", "99999999999", "vier"}) String invalid,
                @Values(ints = {0, 1, 2}) int beforeMove) {
            var output = GameRunner.play(withInvalidInputBefore(beforeMove, invalid, "3", "4", "5"));

            assertThat(output).endsWith(X_WINS + System.lineSeparator());
            assertThat(count(output, INVALID)).isEqualTo(1);
        }

        @Test
        @Issue("BUG-07")
        @DisplayName("E2E-11: bereits besetztes Feld wird mit klarer Meldung abgelehnt")
        @StdIo({"3", "3", "6", "4", "5"})
        void occupiedFieldIsRejected(StdOut out) {
            // Feld 3 hat X selbst belegt, Feld 6 hat O (Seed 3) belegt
            TicTacToeMain.main(new String[0]);

            assertThat(out.capturedString())
                    .contains("field 3 is already taken by CROSS, please choose a free field")
                    .contains("field 6 is already taken by CIRCLE, please choose a free field")
                    .doesNotContain(INVALID)
                    .endsWith(X_WINS + System.lineSeparator());
        }

        @Test
        @DisplayName("E2E-12: Leerzeichen um die Zahl werden toleriert")
        @StdIo({" 3", "4 ", "\t5"})
        void surroundingWhitespaceIsAccepted(StdOut out) {
            TicTacToeMain.main(new String[0]);

            assertThat(out.capturedString()).endsWith(X_WINS + System.lineSeparator()).doesNotContain(INVALID);
        }
    }

    @Nested
    @DisplayName("Boeswillige Eingaben")
    class BoeswilligeEingaben {

        @Issue("BUG-02")
        @CartesianTest(name = "E2E-20: \"{0}\" wird abgelehnt und nicht ausgegeben")
        void maliciousInputIsRejectedAndNotEchoed(
                @Values(strings = {
                        "\u001b[2J\u001b[H",    // ANSI: Bildschirm loeschen (Terminal-Injection)
                        "\u001b]0;hacked\u0007", // ANSI: Fenstertitel aendern
                        "%s%n%x",                // Format-String
                        "4\u00004",              // Null-Byte
                        ARABIC_FOUR,             // Arabisch-indische Ziffer 4 (Integer.parseInt akzeptiert sie)
                        "+4",                    // Vorzeichen
                        "04",                    // fuehrende Null
                        "4; rm -rf /",           // Shell-Injection
                        "${jndi:ldap://x/a}"     // Log4Shell-Muster
                }) String malicious) {
            var output = GameRunner.play(malicious, "3", "4", "5");

            assertThat(count(output, INVALID)).as("Eingabe muss abgelehnt werden").isEqualTo(1);
            assertThat(output).as("Eingabe darf nicht zurueckgegeben werden").doesNotContain(malicious);
            assertThat(output).endsWith(X_WINS + System.lineSeparator());
        }
    }

    @Nested
    @DisplayName("Denial of Service")
    class DenialOfService {

        @Test
        @Issue("BUG-03")
        @DisplayName("E2E-30: keine Eingabe (EOF) beendet das Spiel geordnet")
        void noInputAbortsGracefully() {
            // leerer Stream = sofort Ende der Eingabe (z.B. "java -jar tictactest.jar < /dev/null")
            var output = GameRunner.play(InputStream.nullInputStream());

            assertThat(output).endsWith("game aborted: no more input" + System.lineSeparator());
        }

        @Test
        @Issue("BUG-03")
        @DisplayName("E2E-31: Eingabe endet mitten im Spiel")
        @StdIo({"3", "4"})
        void inputEndsMidGame(StdOut out) {
            assertThatCode(() -> TicTacToeMain.main(new String[0])).doesNotThrowAnyException();

            assertThat(out.capturedString()).endsWith("game aborted: no more input" + System.lineSeparator());
        }

        @Test
        @Issue("BUG-04")
        @DisplayName("E2E-32: endlos ungueltige Eingaben fuehren nicht zu einer Endlosschleife")
        void endlessInvalidInputIsLimited() {
            var output = GameRunner.play(GameRunner.endless("x\n"));

            assertThat(output).endsWith("game aborted: too many invalid inputs" + System.lineSeparator());
            assertThat(count(output, INVALID)).isEqualTo(10);
        }

        @Test
        @Issue("BUG-04")
        @DisplayName("E2E-33: endlose Zeile ohne Zeilenende fuehrt nicht zu Haenger oder OutOfMemory")
        void endlessLineIsLimited() {
            var output = GameRunner.play(GameRunner.endless("9"));

            assertThat(output).endsWith("game aborted: too many invalid inputs" + System.lineSeparator());
        }

        @Test
        @DisplayName("E2E-34: sehr lange Eingabe (1 MB) wird schnell abgelehnt")
        void hugeInputIsRejectedQuickly() {
            var lines = new ArrayList<>(List.of("3".repeat(1_000_000)));
            lines.addAll(List.of("3", "4", "5"));

            var output = GameRunner.play(lines.toArray(String[]::new));

            assertThat(output).doesNotContain(X_WINS).endsWith("game aborted: too many invalid inputs" + System.lineSeparator());
        }
    }

    private static String[] withInvalidInputBefore(int move, String invalid, String... moves) {
        var lines = new ArrayList<>(List.of(moves));
        lines.add(move, invalid);
        return lines.toArray(String[]::new);
    }
}
