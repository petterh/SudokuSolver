package com.hesselberg.sudokusolver;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BoardTest {

    private static final String SOLUTION =
            "725643918" +
            "613897425" +
            "489521376" +
            "862139754" +
            "591478263" +
            "347265189" +
            "178954632" +
            "956312847" +
            "234786591";

    /** Solution with one cell wrong. */
    private static final String ERROR1 =
            "725643918" +
            "613897425" +
            "489521376" +
            "862139754" +
            "591478261" +
            "347265189" +
            "178954632" +
            "956312847" +
            "234786591";

    /** Sample with duplicate 1 in row 1. */
    private static final String ERROR2 =
            "     3 11" +
            "6     425" +
            "   5     " +
            "  2 3   4" +
            " 91  8 6 " +
            "         " +
            "     463 " +
            "95   2   " +
            "2       1";

    /** Sample with conflicting 5 in row 3. */
    private static final String ERROR3 =
            "     3 18" +
            "6     425" +
            "  55     " +
            "  2 3   4" +
            " 91  8 6 " +
            "         " +
            "     463 " +
            "95   2   " +
            "2       1";

    /** Sample with duplicate 2 in row 8. */
    private static final String ERROR4 =
            "     3 18" +
            "6     425" +
            "   5     " +
            "  2 3   4" +
            " 91  8 6 " +
            "         " +
            "     463 " +
            "95   22  " +
            "2       1";

    @Test
    public void verify() {
        assertEquals(SOLUTION.length(), SampleBoards.SUDOKU_SAMPLE.length());

        for (int i = 0; i < SampleBoards.SUDOKU_SAMPLE.length(); i++) {
            char test = SampleBoards.SUDOKU_SAMPLE.charAt(i);
            if (test != ' ') {
                char solution = SOLUTION.charAt(i);
                assertEquals(solution, test);
            }
        }
    }

    @Test
    public void solve() {
        Board board = new Board(SampleBoards.SUDOKU_SAMPLE);
        List<String> solutions = new ArrayList<>();
        long start = System.currentTimeMillis();
        assertTrue(board.solve(solutions));
        System.out.println(System.currentTimeMillis() - start);
        assertEquals(1, solutions.size());
        assertEquals(SOLUTION, solutions.get(0));

        board = new Board(ERROR1);
        solutions.clear();
        assertFalse(board.solve(solutions));
        assertEquals(0, solutions.size());
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
            assertEquals(i, Board.convertToStringIndex(Board.convertToStringIndex(i)));
        }
    }

    @Test
    public void testIsAcceptable() {
        assertTrue(new Board("123").isAcceptable(0));
        assertFalse(new Board("11").isAcceptable(0));
        assertTrue(new Board(SOLUTION).isAcceptable(0));
        assertFalse(new Board(ERROR1).isAcceptable(8));
        assertFalse(new Board(ERROR2).isAcceptable(0));
        assertFalse(new Board(ERROR3).isAcceptable(20));
        assertFalse(new Board(ERROR4).isAcceptable(68));
    }

    @Test
    public void testToString() {
        assertEquals(SampleBoards.SUDOKU_SAMPLE, new Board(SampleBoards.SUDOKU_SAMPLE).toString());
        assertEquals(SOLUTION, new Board(SOLUTION).toString());
    }

    @Test
    public void conflictingCells_marksDuplicateDigitsInSameRow() {
        // Two 1s in row 0 (indices 7 and 8).
        boolean[] conflicts = new Board(ERROR2).conflictingCells();
        assertTrue(conflicts[7]);
        assertTrue(conflicts[8]);
        assertFalse(conflicts[0]);
    }

    @Test
    public void conflictingCells_emptyForConsistentBoard() {
        boolean[] conflicts = new Board(SampleBoards.SUDOKU_SAMPLE).conflictingCells();
        for (boolean conflict : conflicts) {
            assertFalse(conflict);
        }
    }
}
