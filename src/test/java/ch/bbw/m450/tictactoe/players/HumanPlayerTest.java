package ch.bbw.m450.tictactoe.players;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import ch.bbw.m450.tictactoe.TicTacToeMain;
import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.TicTacToeTestFixtures;

class HumanPlayerTest extends TicTacToeTestFixtures {

    // HumanPlayer liest System.in ab dem Erzeugen, darum zuerst Eingabe setzen
    private HumanPlayer playerWithInput(String input) {
        givenInput(input);
        return new HumanPlayer();
    }

    @ParameterizedTest(name = "Eingabe {0} wird als Zug gelesen")
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
    void readsMoveFromInput(int move) {
        var player = playerWithInput(move + "\n");

        assertThat(player.play(board, Stone.CROSS)).isEqualTo(move);
    }

    // Gefunden mit PIT: ohne diesen Test fiel das Entfernen der Eingabeaufforderung nicht auf
    @ParameterizedTest(name = "fordert {0} mit Board zur Eingabe auf")
    @EnumSource(Stone.class)
    void promptsForMoveWithBoard(Stone color) {
        var player = playerWithInput("4\n");

        player.play(board, color);

        assertThat(output()).isEqualTo(
                TicTacToeMain.toString(board)
                        + "where to put the next " + color + "? (0-8): " + System.lineSeparator());
    }

    @Test
    void readsConsecutiveMoves() {
        var player = playerWithInput("0\n8\n");

        assertThat(player.play(board, Stone.CROSS)).isZero();
        assertThat(player.play(board, Stone.CROSS)).isEqualTo(8);
    }

    // ---------- ungueltige Eingaben (BUG-01 bis BUG-04, siehe Testkonzept) ----------

    private static final String INVALID = "invalid input, please enter a free field (0-8)";

    /** Arabisch-indische Ziffer 4 (U+0664): Integer.parseInt wuerde sie als 4 akzeptieren */
    private static final String ARABIC_FOUR = "" + (char) 0x0664;

    private int countInvalid() {
        return output().split(Pattern.quote(INVALID), -1).length - 1;
    }

    @ParameterizedTest(name = "ungueltige Eingabe \"{0}\" wird abgelehnt und erneut gefragt")
    @ValueSource(strings = {"abc", "", "   ", "4.5", "9", "-1", "+4", "04", ARABIC_FOUR, "99999999999"})
    void invalidInputIsAskedAgain(String input) {
        var player = playerWithInput(input + "\n7\n");

        assertThat(player.play(board, Stone.CIRCLE)).isEqualTo(7);
        assertThat(countInvalid()).isEqualTo(1);
    }

    @ParameterizedTest(name = "besetztes Feld {0} (von {1}) wird mit eigener Meldung abgelehnt")
    @CsvSource({"4, CROSS", "0, CIRCLE", "8, CIRCLE"})
    void occupiedFieldIsAskedAgainWithClearMessage(int field, Stone owner) {
        board[field] = owner;
        var player = playerWithInput(field + "\n5\n");

        assertThat(player.play(board, Stone.CROSS)).isEqualTo(5);
        assertThat(output())
                .contains("field " + field + " is already taken by " + owner + ", please choose a free field")
                .doesNotContain(INVALID);
    }

    @ParameterizedTest(name = "Eingabe \"{0}\" mit Leerzeichen/Zeilenende wird akzeptiert")
    @ValueSource(strings = {" 3\n", "3 \n", "\t3\n", "3\r\n", "3"})
    void surroundingWhitespaceAndLineEndsAreAccepted(String input) {
        var player = playerWithInput(input);

        assertThat(player.play(board, Stone.CROSS)).isEqualTo(3);
        assertThat(countInvalid()).isZero();
    }

    @Test
    void endOfInputAbortsTheGame() {
        var player = playerWithInput("");

        assertThatThrownBy(() -> player.play(board, Stone.CROSS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("no more input");
    }

    @Test
    void lastAllowedAttemptIsAccepted() {
        var player = playerWithInput("x\n".repeat(HumanPlayer.MAX_ATTEMPTS - 1) + "2\n");

        assertThat(player.play(board, Stone.CROSS)).isEqualTo(2);
        assertThat(countInvalid()).isEqualTo(HumanPlayer.MAX_ATTEMPTS - 1);
    }

    @Test
    void tooManyInvalidInputsAbortTheGame() {
        var player = playerWithInput("x\n".repeat(HumanPlayer.MAX_ATTEMPTS) + "2\n");

        assertThatThrownBy(() -> player.play(board, Stone.CROSS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("too many invalid inputs");
        assertThat(countInvalid()).isEqualTo(HumanPlayer.MAX_ATTEMPTS);
    }

    @Test
    void tooLongLineCountsAsOneInvalidInput() {
        var player = playerWithInput("4".repeat(1000) + "\n4\n");

        assertThat(player.play(board, Stone.CROSS)).isEqualTo(4);
        assertThat(countInvalid()).isEqualTo(1);
    }

    @Test
    void endlessLineIsOnlySkippedUpToTheLimit() {
        // Genau an der Grenze: MAX_LINE_LENGTH + 1 Zeichen werden behalten, MAX_SKIPPED uebersprungen.
        // Danach bricht das Lesen ab, das folgende Zeilenende ergibt eine zweite (leere) ungueltige Eingabe.
        // Ein Zeichen mehr oder weniger wuerde "4" bzw. nur eine ungueltige Eingabe ergeben.
        var length = HumanPlayer.MAX_LINE_LENGTH + 1 + HumanPlayer.MAX_SKIPPED;
        var player = playerWithInput("4".repeat(length) + "\n5\n");

        assertThat(player.play(board, Stone.CROSS)).isEqualTo(5);
        assertThat(countInvalid()).isEqualTo(2);
    }

    @Test
    void carriageReturnsAreIgnored() {
        // \r zaehlt nicht zur Zeilenlaenge (Windows-Zeilenenden)
        var player = playerWithInput("\r".repeat(HumanPlayer.MAX_LINE_LENGTH + 10) + "3\n");

        assertThat(player.play(board, Stone.CROSS)).isEqualTo(3);
    }

    @Test
    void readErrorIsReported() {
        System.setIn(new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("disk on fire");
            }
        });
        var player = new HumanPlayer();

        assertThatThrownBy(() -> player.play(board, Stone.CROSS))
                .isInstanceOf(UncheckedIOException.class)
                .hasRootCauseMessage("disk on fire");
    }

    @ParameterizedTest(name = "funktioniert fuer Farbe {0}")
    @EnumSource(Stone.class)
    void worksForBothColours(Stone color) {
        var player = playerWithInput("7\n");

        assertThat(player.play(board, color)).isEqualTo(7);
    }
}
