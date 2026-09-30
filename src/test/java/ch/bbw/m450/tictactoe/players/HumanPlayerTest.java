package ch.bbw.m450.tictactoe.players;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import ch.bbw.m450.tictactoe.TicTacToeMain;
import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.TicTacToeTestFixtures;

class HumanPlayerTest extends TicTacToeTestFixtures {

    // Der Scanner liest System.in beim Erzeugen, darum zuerst Eingabe setzen
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
                        + "where to to put the next " + color + "? (0-8): " + System.lineSeparator());
    }

    @Test
    void readsConsecutiveMoves() {
        var player = playerWithInput("0\n8\n");

        assertThat(player.play(board, Stone.CROSS)).isZero();
        assertThat(player.play(board, Stone.CROSS)).isEqualTo(8);
    }

    @ParameterizedTest(name = "ungueltige Eingabe \"{0}\" wirft Exception")
    @ValueSource(strings = {"abc", "", "4.5", " 3"})
    void invalidInputThrows(String input) {
        var player = playerWithInput(input + "\n");

        assertThatThrownBy(() -> player.play(board, Stone.CIRCLE))
                .isInstanceOf(NumberFormatException.class);
    }

    @ParameterizedTest(name = "funktioniert fuer Farbe {0}")
    @EnumSource(Stone.class)
    void worksForBothColours(Stone color) {
        var player = playerWithInput("7\n");

        assertThat(player.play(board, color)).isEqualTo(7);
    }
}
