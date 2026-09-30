package ch.bbw.m450.tictactoe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

/**
 * Fixture: Basisklasse fuer die Tests. Stellt vor jedem Test ein frisches, leeres Board bereit
 * und setzt System.in nach jedem Test wieder zurueck.
 */
public abstract class TicTacToeTestFixtures {

    protected Stone[] board;

    private InputStream originalIn;

    private PrintStream originalOut;

    private ByteArrayOutputStream capturedOut;

    @BeforeEach
    void setUpFixture() {
        board = TicTacToeTestHelpers.emptyBoard();
        originalIn = System.in;
        // Konsolenausgabe mitschneiden, damit Tests sie pruefen koennen
        originalOut = System.out;
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDownFixture() {
        Arrays.fill(board, null);
        board = null;
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    /** Alles, was der Test bisher auf System.out ausgegeben hat. */
    protected String output() {
        return capturedOut.toString(StandardCharsets.UTF_8);
    }

    /** Ersetzt das Fixture-Board durch das angegebene Layout. */
    protected Stone[] givenBoard(String layout) {
        board = TicTacToeTestHelpers.board(layout);
        return board;
    }

    /** Simuliert Tastatureingaben ueber System.in (wird nach dem Test zurueckgesetzt). */
    protected void givenInput(String input) {
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    }
}
