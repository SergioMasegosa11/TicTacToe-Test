package ch.bbw.m450.tictactoe.players;

import java.util.Random;
import java.util.stream.IntStream;

import ch.bbw.m450.tictactoe.TicTacToeMain;
import ch.bbw.m450.tictactoe.TicTacToePlayer;

/**
 * Computer player choosing a random free field.
 * The {@link Random} can be passed in, so games are reproducible in tests (fixed seed).
 */
public class RandomPlayer implements TicTacToePlayer {

	private final Random random;

	public RandomPlayer() {
		this(new Random());
	}

	public RandomPlayer(Random random) {
		this.random = random;
	}

	@Override
	public int play(Stone[] board, Stone colorToPlay) {
		var freeFields = IntStream.range(0, TicTacToeMain.BOARD_SIZE)
				.filter(i -> board[i] == null)
				.toArray();
		if (freeFields.length == 0) {
			throw new IllegalStateException("cannot play at all");
		}
		return freeFields[random.nextInt(freeFields.length)];
	}
}
