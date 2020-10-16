package com.hesselberg.sudokusolver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
    public void convertToTextViewIndex() {
        assertEquals(0, Board.convertToTextViewIndex(0));
        assertEquals(18, Board.convertToTextViewIndex(6));
        assertEquals(26, Board.convertToTextViewIndex(26));
        assertEquals(54, Board.convertToTextViewIndex(54));
        assertEquals(57, Board.convertToTextViewIndex(63));
        assertEquals(4, Board.convertToTextViewIndex(10));
        assertEquals(4, Board.convertToTextViewIndex(10));
        assertEquals(4, Board.convertToTextViewIndex(10));
        assertEquals(81, Board.convertToTextViewIndex(81));
    }

    @Test
    public void constructor() {
        Board board = new Board();
    }
}
