package ch.bbw.m450.tictactoe.players;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.TicTacToeTestFixtures;
import ch.bbw.m450.tictactoe.TicTacToeTestHelpers;

class GreedyPlayerTest extends TicTacToeTestFixtures {

    private GreedyPlayer player;

    @BeforeEach
    void setUpPlayer() {
        player = new GreedyPlayer();
    }

    @ParameterizedTest(name = "Felder 0..{0} belegt -> spielt Feld {0}")
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
    void playsLowestFreeField(int firstFreeField) {
        board = TicTacToeTestHelpers.boardFilledUpTo(firstFreeField, Stone.CIRCLE);

        assertThat(player.play(board, Stone.CROSS)).isEqualTo(firstFreeField);
    }

    @ParameterizedTest(name = "Luecke bei Feld {0} wird gefunden")
    @ValueSource(strings = {
            ".XOXOXOXO",
            "XO.OXOXOX",
            "XOXOXOXO."
    })
    void findsSingleGap(String layout) {
        givenBoard(layout);

        assertThat(player.play(board, Stone.CROSS)).isEqualTo(layout.indexOf('.'));
    }

    @ParameterizedTest(name = "volles Board wirft Exception fuer {0}")
    @EnumSource(Stone.class)
    void fullBoardThrows(Stone color) {
        givenBoard("XOXOXOXOX");

        assertThatThrownBy(() -> player.play(board, color))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("cannot play at all");
    }
}
