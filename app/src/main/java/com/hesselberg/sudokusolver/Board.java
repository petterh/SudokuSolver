package com.hesselberg.sudokusolver;

import androidx.annotation.NonNull;

public class Board {

    private static final int SUB = 3;
    private static final int DIM = SUB * SUB;

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

    public boolean solve() {
        return false;
    }
}
