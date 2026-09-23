package ch.bbw.m450.tictactoe;

import java.util.Arrays;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

/**
 * Helper: statische Hilfsmethoden und Test-Spieler, die von allen Tests verwendet werden.
 */
public final class TicTacToeTestHelpers {

    private TicTacToeTestHelpers() {
    }

    /**
     * Baut ein Board aus einem lesbaren Layout, z.B. "X O . / . X . / O . X".
     * X = CROSS, O = CIRCLE, alles andere = leer. Leerzeichen und Zeilenumbrueche werden ignoriert.
     */
    public static Stone[] board(String layout) {
        var compact = layout.replaceAll("\\s", "");
        if (compact.length() != TicTacToeMain.BOARD_SIZE) {
            throw new IllegalArgumentException(
                    "layout braucht " + TicTacToeMain.BOARD_SIZE + " Felder, hat aber " + compact.length());
        }
        var board = emptyBoard();
        for (var i = 0; i < TicTacToeMain.BOARD_SIZE; i++) {
            board[i] = switch (compact.charAt(i)) {
                case 'X', 'x' -> Stone.CROSS;
                case 'O', 'o' -> Stone.CIRCLE;
                default -> null;
            };
        }
        return board;
    }

    public static Stone[] emptyBoard() {
        return new Stone[TicTacToeMain.BOARD_SIZE];
    }

    /** Board, bei dem die ersten {@code count} Felder mit {@code stone} belegt sind. */
    public static Stone[] boardFilledUpTo(int count, Stone stone) {
        var board = emptyBoard();
        Arrays.fill(board, 0, count, stone);
        return board;
    }

    /** Vertauscht X und O in einem Layout. */
    public static String swapColours(String layout) {
        var swapped = new StringBuilder(layout.length());
        for (var c : layout.toCharArray()) {
            swapped.append(switch (c) {
                case 'X' -> 'O';
                case 'O' -> 'X';
                default -> c;
            });
        }
        return swapped.toString();
    }

    /**
     * Test-Spieler, der eine fest vorgegebene Liste von Zuegen abspielt.
     */
    public static class ScriptedPlayer implements TicTacToePlayer {

        private final int[] moves;
        private int cursor;

        public ScriptedPlayer(int... moves) {
            this.moves = moves;
        }

        @Override
        public int play(Stone[] board, Stone colorToPlay) {
            if (cursor >= moves.length) {
                throw new IllegalStateException("scripted player hat keine Zuege mehr");
            }
            return moves[cursor++];
        }
    }

    /**
     * Test-Spieler, der das uebergebene Board komplett ueberschreibt (versuchter Betrug).
     */
    public static final class CheatingPlayer extends ScriptedPlayer {

        public CheatingPlayer(int... moves) {
            super(moves);
        }

        @Override
        public int play(Stone[] board, Stone colorToPlay) {
            Arrays.fill(board, colorToPlay);
            return super.play(board, colorToPlay);
        }
    }
}
