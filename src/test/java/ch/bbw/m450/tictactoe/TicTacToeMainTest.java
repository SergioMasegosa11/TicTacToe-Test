package ch.bbw.m450.tictactoe;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

class TicTacToeMainTest {

    @Test
    void crossWinsHorizontally() {
        Stone[] board = {
                Stone.CROSS, Stone.CROSS, Stone.CROSS,
                null, null, null,
                null, null, null
        };

        boolean result = TicTacToeMain.isWin(board, Stone.CROSS);

        assertThat(result).isTrue();
    }

    @Test
    void circleWinsHorizontally() {
        Stone[] board = {
                null, null, null,
                Stone.CIRCLE, Stone.CIRCLE, Stone.CIRCLE,
                null, null, null
        };

        boolean result = TicTacToeMain.isWin(board, Stone.CIRCLE);

        assertThat(result).isTrue();
    }

    @Test
    void crossWinsVertically() {
        Stone[] board = {
                Stone.CROSS, null, null,
                Stone.CROSS, null, null,
                Stone.CROSS, null, null
        };

        boolean result = TicTacToeMain.isWin(board, Stone.CROSS);

        assertThat(result).isTrue();
    }

    @Test
    void crossWinsDiagonally() {
        Stone[] board = {
                Stone.CROSS, null, null,
                null, Stone.CROSS, null,
                null, null, Stone.CROSS
        };

        boolean result = TicTacToeMain.isWin(board, Stone.CROSS);

        assertThat(result).isTrue();
    }

    @Test
    void nobodyWins() {
        Stone[] board = {
                Stone.CROSS, Stone.CIRCLE, Stone.CROSS,
                Stone.CIRCLE, Stone.CROSS, Stone.CIRCLE,
                Stone.CIRCLE, Stone.CROSS, Stone.CIRCLE
        };

        boolean crossWins = TicTacToeMain.isWin(board, Stone.CROSS);
        boolean circleWins = TicTacToeMain.isWin(board, Stone.CIRCLE);

        assertThat(crossWins).isFalse();
        assertThat(circleWins).isFalse();
    }
}