package com.hesselberg.sudokusolver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class BoardTest {

    private static final String TEST_BOARD =
            "       1 " +
            "8        " +
            "    67  2" +
            " 6  82  3" +
            "9  4 5   " +
            "     3 81" +
            "3  5     " +
            "  5   1 6" +
            "4  2  73 ";

    private static final String SOLUTION =
            "679324518" +
            "823159674" +
            "154867392" +
            "761982453" +
            "938415267" +
            "542673981" +
            "317546829" +
            "295738146" +
            "486291735";

    private static final String ERROR1 =
            "679324518" +
            "823159674" +
            "154867392" +
            "761982453" +
            "938415261" +
            "542673981" +
            "317546829" +
            "295738146" +
            "486291735";

    private static final String ERROR2 =
            "       11" +
            "8        " +
            "    67  2" +
            " 6  82  3" +
            "9  4 5   " +
            "     3 81" +
            "3  5     " +
            "  5   1 6" +
            "4  2  73 ";

    private static final String ERROR3 =
            "       1 " +
            "8        " +
            " 8  67  2" +
            " 6  82  3" +
            "9  4 5   " +
            "     3 81" +
            "3  5     " +
            "  5   1 6" +
            "4  2  73 ";

    private static final String ERROR4 =
            "       1 " +
            "8        " +
            "    67  2" +
            " 6  82  3" +
            "9  4 5   " +
            "     3 81" +
            "3  5     " +
            "  5    16" +
            "4  2  73 ";

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void verify() {
        assertEquals(SOLUTION.length(), TEST_BOARD.length());

        for (int i = 0; i < TEST_BOARD.length(); i++) {
            char test = TEST_BOARD.charAt(i);
            if (test != ' ') {
                char solution = SOLUTION.charAt(i);
                assertEquals(solution, test);
            }
        }
    }

    @Test
    public void solve() {
        Board board = new Board((TEST_BOARD));
        List<String> solutions = new ArrayList<>();
        long start = System.currentTimeMillis();
        assertTrue(board.solve(solutions));
        System.out.println(System.currentTimeMillis() - start);
        assertEquals(1, solutions.size());
        assertEquals(SOLUTION, solutions.get(0));
    }

    @Test
    public void convertToTextViewIndex() {
        assertEquals(0, Board.convertToTextViewIndex(0));
        assertEquals(18, Board.convertToTextViewIndex(6));
        assertEquals(26, Board.convertToTextViewIndex(26));
        assertEquals(54, Board.convertToTextViewIndex(54));
        assertEquals(57, Board.convertToTextViewIndex(63));
        assertEquals(81, Board.convertToTextViewIndex(81));
    }

    @Test
    public void convertToStringIndex() {
        assertEquals(0, Board.convertToStringIndex(0));
        assertEquals(6, Board.convertToStringIndex(18));
        assertEquals(26, Board.convertToStringIndex(26));
        assertEquals(54, Board.convertToStringIndex(54));
        assertEquals(63, Board.convertToStringIndex(57));
        assertEquals(81, Board.convertToStringIndex(81));
    }

    @Test
    public void verifyConversionSymmetry() {
        for (int i = 0; i < Board.DIM * Board.DIM; i++) {
            assertEquals(i, Board.convertToTextViewIndex(Board.convertToTextViewIndex(i)));
        }
    }

    @Test
    public void testIsAcceptable() {
        assertTrue(new Board("123").isAcceptable(0));
        assertFalse(new Board("11").isAcceptable(0));
        assertTrue(new Board(SOLUTION).isAcceptable(0));
        assertFalse(new Board(ERROR1).isAcceptable(8));
        assertFalse(new Board(ERROR2).isAcceptable(0));
        assertFalse(new Board(ERROR3).isAcceptable(0));
        assertFalse(new Board(ERROR4).isAcceptable(7));
    }

    @Test
    public void testToString() {
        assertEquals(TEST_BOARD, new Board(TEST_BOARD).toString());
        assertEquals(SOLUTION, new Board(SOLUTION).toString());
    }

    @Test
    public void constructor() {
        Board board = new Board();
    }
}
