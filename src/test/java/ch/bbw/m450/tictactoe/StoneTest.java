package ch.bbw.m450.tictactoe;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

class StoneTest {

    @ParameterizedTest(name = "Gegner von {0} ist {1}")
    @CsvSource({
            "CROSS,  CIRCLE",
            "CIRCLE, CROSS"
    })
    void opponentIsOtherColour(Stone stone, Stone expectedOpponent) {
        assertThat(stone.opponent()).isEqualTo(expectedOpponent);
    }

    @ParameterizedTest(name = "Gegner vom Gegner von {0} ist wieder {0}")
    @EnumSource(Stone.class)
    void opponentOfOpponentIsSelf(Stone stone) {
        assertThat(stone.opponent().opponent()).isEqualTo(stone);
    }
}
