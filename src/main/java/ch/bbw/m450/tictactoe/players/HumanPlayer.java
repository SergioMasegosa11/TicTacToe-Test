package ch.bbw.m450.tictactoe.players;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import ch.bbw.m450.tictactoe.TicTacToeMain;
import ch.bbw.m450.tictactoe.TicTacToePlayer;

/**
 * Simple human-player taking input from stdin.
 * Invalid input is rejected and asked again, but only a limited number of times
 * and with a limited line length, so a malicious input cannot block the game forever.
 */
public class HumanPlayer implements TicTacToePlayer {

	/** after this many invalid inputs in a row the game is aborted */
	static final int MAX_ATTEMPTS = 10;

	/** longer lines are rejected */
	static final int MAX_LINE_LENGTH = 16;

	/** at most this many characters of a too long line are skipped, so an endless line cannot block the game */
	static final int MAX_SKIPPED = 4096;

	/** exactly one ASCII digit 0-8 (no sign, no leading zero, no other unicode digits) */
	private static final Pattern FIELD = Pattern.compile("[0-8]");

	private final Reader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

	@Override
	public int play(Stone[] board, Stone colorToPlay) {
		for (var attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			System.out.println(TicTacToeMain.toString(board) + "where to put the next " + colorToPlay + "? (0-8): ");
			var line = readLine();
			if (line == null) {
				throw new IllegalStateException("no more input");
			}
			var input = line.strip();
			if (FIELD.matcher(input).matches() && board[Integer.parseInt(input)] == null) {
				return Integer.parseInt(input);
			}
			// the input itself is never printed, it could contain terminal escape sequences
			System.out.println("invalid input, please enter a free field (0-8)");
		}
		throw new IllegalStateException("too many invalid inputs");
	}

	/**
	 * Reads one line, but keeps at most {@link #MAX_LINE_LENGTH} + 1 characters of it.
	 * The rest of a too long line is skipped (at most {@link #MAX_SKIPPED} characters),
	 * so a too long line counts as a single invalid input.
	 *
	 * @return the line without line break, or {@code null} at the end of the input
	 */
	private String readLine() {
		try {
			var line = new StringBuilder();
			var skipped = 0;
			int c;
			while ((c = in.read()) != -1) {
				if (c == '\n') {
					return line.toString();
				}
				if (c == '\r') {
					continue;
				}
				if (line.length() <= MAX_LINE_LENGTH) {
					line.append((char) c);
				} else if (++skipped >= MAX_SKIPPED) {
					return line.toString();
				}
			}
			return line.isEmpty() ? null : line.toString();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
