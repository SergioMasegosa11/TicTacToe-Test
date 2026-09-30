package ch.bbw.m450.tictactoe.players;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.TicTacToeTestFixtures;
import ch.bbw.m450.tictactoe.TicTacToeTestHelpers;

class RandomPlayerTest extends TicTacToeTestFixtures {

    @ParameterizedTest(name = "auf \"{0}\" wird nur ein freies Feld gespielt")
    @ValueSource(strings = {".........", "X........", "XO.......", "XOXOXOXO.", ".OXOXOXOX", "XOX.O.XOX"})
    void playsOnlyFreeFields(String layout) {
        givenBoard(layout);
        var player = new RandomPlayer(new Random(1));

        for (var i = 0; i < 100; i++) {
            var move = player.play(board, Stone.CIRCLE);
            assertThat(board[move]).as("Feld %d muss frei sein", move).isNull();
        }
    }

    @Test
    void choosesEveryFreeFieldSometimes() {
        // Regression: der alte Gegner (GreedyPlayer) nahm immer das tiefste freie Feld
        givenBoard("X...O...X");
        var player = new RandomPlayer(new Random(42));

        var chosen = new HashSet<Integer>();
        for (var i = 0; i < 500; i++) {
            chosen.add(player.play(board, Stone.CIRCLE));
        }

        assertThat(chosen).containsExactlyInAnyOrder(1, 2, 3, 5, 6, 7);
    }

    @Test
    void onlyOneFreeFieldIsAlwaysChosen() {
        givenBoard("XOXOXOXO.");
        var player = new RandomPlayer();

        assertThat(IntStream.range(0, 20).map(i -> player.play(board, Stone.CIRCLE))).containsOnly(8);
    }

    @Test
    void sameSeedPlaysSameMoves() {
        var first = new RandomPlayer(new Random(7));
        var second = new RandomPlayer(new Random(7));

        for (var i = 0; i < 20; i++) {
            assertThat(first.play(board, Stone.CIRCLE)).isEqualTo(second.play(board, Stone.CIRCLE));
        }
    }

    @ParameterizedTest(name = "volles Board wirft Exception fuer {0}")
    @EnumSource(Stone.class)
    void fullBoardThrows(Stone color) {
        board = TicTacToeTestHelpers.board("XOXOXOXOX");
        var player = new RandomPlayer();

        assertThatThrownBy(() -> player.play(board, color))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("cannot play at all");
    }
}
