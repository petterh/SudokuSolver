package com.hesselberg.sudokusolver;

import java.util.Arrays;

/**
 * Shared puzzle fixtures. {@link #SUDOKU_SAMPLE} matches {@code img/SudokuSample.jpg}
 * (also copied under {@code androidTest/assets} for OCR regression).
 */
public final class SampleBoards {

    /** Empty 9×9 board (81 spaces) used as the app's starting state. */
    public static final String EMPTY;

    static {
        char[] cells = new char[Board.DIM * Board.DIM];
        Arrays.fill(cells, ' ');
        EMPTY = new String(cells);
    }

    /**
     * Clues from {@code SudokuSample.jpg} in the 81-character board format
     * (digits {@code '1'}–{@code '9'} or space).
     */
    public static final String SUDOKU_SAMPLE =
            "     3 18" +
            "6     425" +
            "   5     " +
            "  2 3   4" +
            " 91  8 6 " +
            "         " +
            "     463 " +
            "95   2   " +
            "2       1";

    private SampleBoards() {
    }
}
