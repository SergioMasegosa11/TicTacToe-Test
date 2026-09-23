package ch.bbw.m450.tictactoe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.TicTacToeTestHelpers.CheatingPlayer;
import ch.bbw.m450.tictactoe.TicTacToeTestHelpers.ScriptedPlayer;
import ch.bbw.m450.tictactoe.players.GreedyPlayer;

class TicTacToeMainTest extends TicTacToeTestFixtures {

    // ---------- isWin ----------

    static Stream<Arguments> winningLines() {
        return Stream.of(
                Arguments.of("obere Reihe", """
                        X X X
                        . O .
                        O . .
                        """),
                Arguments.of("mittlere Reihe", """
                        O . O
                        X X X
                        . . .
                        """),
                Arguments.of("untere Reihe", """
                        O . .
                        . O .
                        X X X
                        """),
                Arguments.of("linke Spalte", """
                        X O .
                        X O .
                        X . .
                        """),
                Arguments.of("mittlere Spalte", """
                        O X .
                        . X O
                        . X .
                        """),
                Arguments.of("rechte Spalte", """
                        . O X
                        . . X
                        O . X
                        """),
                Arguments.of("Hauptdiagonale", """
                        X O .
                        O X .
                        . . X
                        """),
                Arguments.of("Gegendiagonale", """
                        . O X
                        O X .
                        X . .
                        """));
    }

    @ParameterizedTest(name = "{0}: CROSS gewinnt, CIRCLE nicht")
    @MethodSource("winningLines")
    void crossWinsWithLine(String description, String layout) {
        givenBoard(layout);

        assertThat(TicTacToeMain.isWin(board, Stone.CROSS)).as(description).isTrue();
        assertThat(TicTacToeMain.isWin(board, Stone.CIRCLE)).as(description).isFalse();
    }

    @ParameterizedTest(name = "{0}: CIRCLE gewinnt, CROSS nicht")
    @MethodSource("winningLines")
    void circleWinsWithSameLine(String description, String layout) {
        givenBoard(TicTacToeTestHelpers.swapColours(layout));

        assertThat(TicTacToeMain.isWin(board, Stone.CIRCLE)).as(description).isTrue();
        assertThat(TicTacToeMain.isWin(board, Stone.CROSS)).as(description).isFalse();
    }

    @ParameterizedTest(name = "kein Gewinner: {0}")
    @CsvSource(delimiter = '|', textBlock = """
            leeres Board          | .........
            nur zwei in der Reihe | XX.OO....
            halbe Diagonale       | X...X....
            volles Board 1        | XOXOXOOXO
            volles Board 2        | OXOXOXXOX
            volles Board 3        | XOOOXXOXO
            volles Board 4        | OXOOXXXOX
            """)
    void nobodyWins(String description, String layout) {
        givenBoard(layout);

        assertThat(TicTacToeMain.isWin(board, Stone.CROSS)).as(description).isFalse();
        assertThat(TicTacToeMain.isWin(board, Stone.CIRCLE)).as(description).isFalse();
    }

    @ParameterizedTest(name = "leeres Board ist kein Sieg fuer {0}")
    @EnumSource(Stone.class)
    void emptyBoardIsNoWin(Stone color) {
        // board kommt frisch und leer aus der Fixture
        assertThat(TicTacToeMain.isWin(board, color)).isFalse();
    }

    // ---------- play ----------

    static Stream<Arguments> scriptedGames() {
        return Stream.of(
                // X-Zuege, O-Zuege, erwarteter Gewinner (null -> unentschieden)
                Arguments.of(new int[] {0, 1, 2}, new int[] {3, 4}, Stone.CROSS),
                Arguments.of(new int[] {4, 0, 8}, new int[] {1, 2}, Stone.CROSS),
                Arguments.of(new int[] {0, 1, 8}, new int[] {3, 4, 5}, Stone.CIRCLE),
                Arguments.of(new int[] {0, 1, 5, 8}, new int[] {4, 2, 6}, Stone.CIRCLE),
                Arguments.of(new int[] {0, 2, 3, 7, 8}, new int[] {1, 4, 5, 6}, null));
    }

    @ParameterizedTest(name = "Spiel #{index} -> Gewinner {2}")
    @MethodSource("scriptedGames")
    void playReturnsWinnerOrNullOnDraw(int[] xMoves, int[] oMoves, Stone expectedWinner) {
        var winner = TicTacToeMain.play(new ScriptedPlayer(xMoves), new ScriptedPlayer(oMoves));

        assertThat(winner).isEqualTo(expectedWinner);
    }

    @Test
    void playWithTwoGreedyPlayersCrossWins() {
        var winner = TicTacToeMain.play(new GreedyPlayer(), new GreedyPlayer());

        assertThat(winner).isEqualTo(Stone.CROSS);
    }

    @Test
    void playWithSamePlayerTwiceThrows() {
        var player = new GreedyPlayer();

        assertThatThrownBy(() -> TicTacToeMain.play(player, player))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("players must differ");
    }

    @ParameterizedTest(name = "Zug auf {0} ausserhalb des Boards wirft Exception")
    @ValueSource(ints = {-100, -1, 9, 100})
    void playOutsideOfBoardThrows(int invalidMove) {
        assertThatThrownBy(() -> TicTacToeMain.play(new ScriptedPlayer(invalidMove), new ScriptedPlayer()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("cannot play to position " + invalidMove);
    }

    @ParameterizedTest(name = "zweiter Zug auf besetztes Feld {0} wirft Exception")
    @ValueSource(ints = {0, 4, 8})
    void playOnOccupiedFieldThrows(int field) {
        assertThatThrownBy(() -> TicTacToeMain.play(new ScriptedPlayer(field), new ScriptedPlayer(field)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("cannot play to position " + field);
    }

    @Test
    void playerCannotModifyRealBoard() {
        // Betrueger ueberschreibt seine Kopie des Boards, das echte Board bleibt unveraendert
        var winner = TicTacToeMain.play(new CheatingPlayer(0, 1, 2), new ScriptedPlayer(3, 4));

        assertThat(winner).isEqualTo(Stone.CROSS);
    }

    // ---------- toString ----------

    @ParameterizedTest(name = "leeres Feld {0} zeigt seine Nummer")
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
    void toStringShowsIndexForEmptyField(int field) {
        var result = TicTacToeMain.toString(board);

        assertThat(result).contains("\033[37m" + field + "\033[0m");
        assertThat(result.lines()).hasSize(3);
    }

    @ParameterizedTest(name = "{0} wird als {1} angezeigt")
    @CsvSource({
            "CROSS,  X",
            "CIRCLE, O"
    })
    void toStringShowsStone(Stone stone, String symbol) {
        board[4] = stone;

        var result = TicTacToeMain.toString(board);

        assertThat(result)
                .contains("\033[1m" + symbol + "\033[0m")
                .doesNotContain("\033[37m4\033[0m");
    }

    // ---------- main ----------

    @Test
    void mainPlaysHumanAgainstGreedy() {
        // Mensch (X) spielt 3, 4, 5; Greedy (O) nimmt 0, 1 -> X gewinnt die mittlere Reihe
        givenInput("3\n4\n5\n");

        TicTacToeMain.main(new String[0]);

        assertThat(new TicTacToeMain()).isNotNull();
    }
}
