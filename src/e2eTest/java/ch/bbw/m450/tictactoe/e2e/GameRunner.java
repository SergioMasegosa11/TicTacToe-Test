package ch.bbw.m450.tictactoe.e2e;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import ch.bbw.m450.tictactoe.TicTacToeMain;

/**
 * Helper fuer E2E-Tests mit dynamisch erzeugter Eingabe (z.B. endlose Streams),
 * die sich nicht als Konstante in {@code @StdIo} angeben laesst.
 * Startet das komplette Spiel ueber {@link TicTacToeMain#main(String[])} und liefert die Konsolenausgabe.
 */
final class GameRunner {

    private GameRunner() {
    }

    /** Spielt ein komplettes Spiel mit den angegebenen Eingabezeilen. */
    static String play(String... lines) {
        var input = String.join("\n", lines) + "\n";
        return play(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    }

    /** Spielt ein komplettes Spiel mit einem beliebigen Eingabe-Stream. */
    static String play(InputStream input) {
        var originalIn = System.in;
        var originalOut = System.out;
        var output = new ByteArrayOutputStream();
        try {
            System.setIn(input);
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            TicTacToeMain.main(new String[0]);
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    /** Endloser Stream, der immer wieder dieselben Bytes liefert (z.B. "x\n" oder ein Zeichen ohne Zeilenende). */
    static InputStream endless(String pattern) {
        var bytes = pattern.getBytes(StandardCharsets.UTF_8);
        return new InputStream() {
            private int position;

            @Override
            public int read() {
                var b = bytes[position];
                position = (position + 1) % bytes.length;
                return b;
            }
        };
    }
}
