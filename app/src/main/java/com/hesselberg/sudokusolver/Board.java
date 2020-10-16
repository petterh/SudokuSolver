package com.hesselberg.sudokusolver;

import androidx.annotation.NonNull;

/**
 * TODO: Dimensions need generalizing to 16x16 and 6x6 with 2x3 unsquare groups.
 */
public class Board {

    private static final int SUB = 3;
    static final int DIM = SUB * SUB;

    static final String TEST_BOARD =
            "       1 " +
            "8        " +
            "    67  2" +
            " 6  82  3" +
            "9  4 5   " +
            "     3 81" +
            "3  5     " +
            "  5   1 6" +
            "4  2  73 ";

    private static class Field {
        private char value;

        public Field(char value) {
            this.value = value;
        }

        public char getValue() {
            return value;
        }

        public void setValue(char value) {
            this.value = value;
        }
    }

    private final Field[] fields = new Field[DIM * DIM];

    private final Field[][] rows = new Field[DIM][DIM];
    private final Field[][] columns = new Field[DIM][DIM];
    private final Field[][] groups = new Field[DIM][DIM];

    Board() {
        this(TEST_BOARD);
    }

    public Board(@NonNull String board) {
        for (int i = 0; i < DIM * DIM; i++) {
            fields[i] = new Field(TEST_BOARD.charAt(i));
            int row  = i / DIM;
            int column  = i % DIM;
            rows[row][column] = fields[i];
            columns[column][row] = fields[i];
            int group = (row / SUB) * SUB + (column / SUB);
            int sub = (row % SUB) * SUB + (column % SUB);
            groups[group][sub] = fields[i];
        }
    }

    public static int convertToTextViewIndex(int i) {
        int x = i % DIM;
        int y = i / DIM;
        int group_x = x / SUB;
        int group_y = y / SUB;
        int group = group_y * SUB + group_x;
        int cell = (y % SUB) * SUB + x % SUB;
        return group * DIM + cell;
    }

    /**
     * This turns out to be exactly the same as {@link #convertToTextViewIndex(int)}.
     * I'm not yet clear on where this symmetry comes from.
     */
    public static int convertToStringIndex(int i) {
        int group = i / DIM;
        int cell = i % DIM;
        int group_x = group % SUB;
        int group_y = group / SUB;
        int y = group_y * SUB + cell / SUB;
        int x = group_x * SUB + cell % SUB;
        return y * DIM + x;
    }

    public boolean solve() {
        return false;
    }
}
