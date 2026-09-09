package ch.bbw.m450.tictactoe;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

class TicTacToeMainTest {

    private Stone[] board;

    @BeforeEach
    void setUp() {
        board = emptyBoard();
    }

    // Helper
    private static Stone[] board(Stone... stones) {
        return stones;
    }

    // Fixture
    private static Stone[] emptyBoard() {
        return new Stone[9];
    }

static Stream<Arguments> drawBoards() {
    return Stream.of(
            Arguments.of((Object) board(
                    Stone.CROSS, Stone.CIRCLE, Stone.CROSS,
                    Stone.CIRCLE, Stone.CROSS, Stone.CIRCLE,
                    Stone.CIRCLE, Stone.CROSS, Stone.CIRCLE
            )),

            Arguments.of((Object) board(
                    Stone.CIRCLE, Stone.CROSS, Stone.CIRCLE,
                    Stone.CROSS, Stone.CIRCLE, Stone.CROSS,
                    Stone.CROSS, Stone.CIRCLE, Stone.CROSS
            )),

            Arguments.of((Object) board(
                    Stone.CROSS, Stone.CIRCLE, Stone.CIRCLE,
                    Stone.CIRCLE, Stone.CROSS, Stone.CROSS,
                    Stone.CIRCLE, Stone.CROSS, Stone.CIRCLE
            )),

            Arguments.of((Object) board(
                    Stone.CIRCLE, Stone.CROSS, Stone.CIRCLE,
                    Stone.CIRCLE, Stone.CROSS, Stone.CROSS,
                    Stone.CROSS, Stone.CIRCLE, Stone.CROSS
            )),

            Arguments.of((Object) board(
                    Stone.CROSS, Stone.CROSS, Stone.CIRCLE,
                    Stone.CIRCLE, Stone.CIRCLE, Stone.CROSS,
                    Stone.CIRCLE, Stone.CROSS, Stone.CROSS
            ))
    );
}

    static Stream<Arguments> winningBoards() {
    return Stream.of(
            Arguments.of(
                    board(
                            Stone.CROSS, Stone.CROSS, Stone.CROSS,
                            null, null, null,
                            null, null, null
                    ),
                    Stone.CROSS
            ),

            Arguments.of(
                    board(
                            null, null, null,
                            Stone.CIRCLE, Stone.CIRCLE, Stone.CIRCLE,
                            null, null, null
                    ),
                    Stone.CIRCLE
            ),

            Arguments.of(
                    board(
                            Stone.CROSS, null, null,
                            Stone.CROSS, null, null,
                            Stone.CROSS, null, null
                    ),
                    Stone.CROSS
            ),

            Arguments.of(
                    board(
                            null, null, Stone.CIRCLE,
                            null, null, Stone.CIRCLE,
                            null, null, Stone.CIRCLE
                    ),
                    Stone.CIRCLE
            ),

            Arguments.of(
                    board(
                            Stone.CROSS, null, null,
                            null, Stone.CROSS, null,
                            null, null, Stone.CROSS
                    ),
                    Stone.CROSS
            ),

            Arguments.of(
                    board(
                            null, null, Stone.CIRCLE,
                            null, Stone.CIRCLE, null,
                            Stone.CIRCLE, null, null
                    ),
                    Stone.CIRCLE
            )
    );
}


    @ParameterizedTest
    @MethodSource("winningBoards")
    void playerWins(Stone[] winningBoard, Stone player) {
        boolean result = TicTacToeMain.isWin(winningBoard, player);

        assertThat(result).isTrue();
    }


    @ParameterizedTest
    @MethodSource("drawBoards")
    void nobodyWins(Stone[] board) {
        boolean crossWins = TicTacToeMain.isWin(board, Stone.CROSS);
        boolean circleWins = TicTacToeMain.isWin(board, Stone.CIRCLE);

        assertThat(crossWins).isFalse();
        assertThat(circleWins).isFalse();
    }
}
