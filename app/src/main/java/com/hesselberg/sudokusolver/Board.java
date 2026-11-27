package com.hesselberg.sudokusolver;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.List;

/**
 * TODO: Dimensions need generalizing to 16x16 and 6x6 with 2x3 unsquare groups.
 */
public class Board {

    private static final int SUB = 3;
    static final int DIM = SUB * SUB;
    public static final int ALL_CELLS_TAKEN = -1;

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

    public Board(@NonNull String board) {
        for (int i = 0; i < board.length(); i++) {
            fields[i] = new Field(board.charAt(i));
            int row  = i / DIM;
            int column  = i % DIM;
            rows[row][column] = fields[i];
            columns[column][row] = fields[i];
            int group = (row / SUB) * SUB + (column / SUB);
            int sub = (row % SUB) * SUB + (column % SUB);
            groups[group][sub] = fields[i];
        }
    }

    private boolean isAcceptable() {
        return cluesAreConsistent();
    }

    /** True when no digit is duplicated in any row, column, or block. */
    public boolean cluesAreConsistent() {
        for (int i = 0; i < DIM; i++) {
            if (!isAcceptable(rows[i]) || !isAcceptable(columns[i]) || !isAcceptable(groups[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Marks every filled cell that shares its digit with another cell in the same
     * row, column, or block (string-index order).
     */
    @NonNull
    public boolean[] conflictingCells() {
        boolean[] conflict = new boolean[DIM * DIM];
        for (int row = 0; row < DIM; row++) {
            int[] indices = new int[DIM];
            for (int col = 0; col < DIM; col++) {
                indices[col] = row * DIM + col;
            }
            markDuplicateDigits(rows[row], indices, conflict);
        }
        for (int col = 0; col < DIM; col++) {
            int[] indices = new int[DIM];
            for (int row = 0; row < DIM; row++) {
                indices[row] = row * DIM + col;
            }
            markDuplicateDigits(columns[col], indices, conflict);
        }
        for (int group = 0; group < DIM; group++) {
            int[] indices = new int[DIM];
            int groupRow = group / SUB;
            int groupCol = group % SUB;
            for (int sub = 0; sub < DIM; sub++) {
                int row = groupRow * SUB + sub / SUB;
                int col = groupCol * SUB + sub % SUB;
                indices[sub] = row * DIM + col;
            }
            markDuplicateDigits(groups[group], indices, conflict);
        }
        return conflict;
    }

    private static void markDuplicateDigits(
            @NonNull Field[] unit, @NonNull int[] stringIndices, @NonNull boolean[] conflict) {
        int[] counts = new int[10];
        for (Field field : unit) {
            if (field == null) {
                continue;
            }
            char value = field.getValue();
            if (value >= '1' && value <= '9') {
                counts[value - '0']++;
            }
        }
        for (int i = 0; i < DIM; i++) {
            Field field = unit[i];
            if (field == null) {
                continue;
            }
            char value = field.getValue();
            if (value >= '1' && value <= '9' && counts[value - '0'] > 1) {
                conflict[stringIndices[i]] = true;
            }
        }
    }

    @VisibleForTesting
    boolean isAcceptable(int i) {
        int row  = i / DIM;
        int column  = i % DIM;
        int group = (row / SUB) * SUB + (column / SUB);
        return isAcceptable(rows[row]) && isAcceptable(columns[column]) && isAcceptable(groups[group]);
    }

    private boolean isAcceptable(Field[] fields) {
        int mask = 0;
        for (int i = 0; i < DIM; i++) {
            Field field = fields[i];
            if (field != null) {
                char value = field.getValue();
                if (value > '0') {
                    int d = 1 << (value - '0');
                    if ((mask & d) != 0) {
                        return false;
                    }

                    mask |= d;
                }
            }
        }

        return true;
    }

    /** Finds the first solution (if any) and appends it to {@code solutions}. */
    public boolean solve(@NonNull List<String> solutions) {
        return solve(solutions, 1);
    }

    /**
     * Collects up to {@code maxSolutions} complete boards into {@code solutions}.
     * Useful to detect underdetermined puzzles (e.g. after OCR missed a clue).
     *
     * @return true if at least one solution was found
     */
    public boolean solve(@NonNull List<String> solutions, int maxSolutions) {
        if (maxSolutions < 1) {
            throw new IllegalArgumentException("maxSolutions must be >= 1");
        }
        collectSolutions(solutions, 0, maxSolutions);
        return !solutions.isEmpty();
    }

    /**
     * @return true when enough solutions have been collected and search should stop
     */
    private boolean collectSolutions(@NonNull List<String> solutions, int start, int maxSolutions) {
        int i = findEmptyCell(start);
        if (i == ALL_CELLS_TAKEN) {
            if (isAcceptable()) {
                solutions.add(toString());
            }
            return solutions.size() >= maxSolutions;
        }

        for (int j = 0; j < DIM; j++) {
            fields[i].setValue((char) ('1' + j));
            if (isAcceptable(i) && collectSolutions(solutions, i, maxSolutions)) {
                return true;
            }
        }

        fields[i].setValue(' ');
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Field field : fields) {
            sb.append(field.getValue());
        }

        return sb.toString();
    }

    private int findEmptyCell(int start) {
        for (int i = start; i < fields.length; i++) {
            if (fields[i].getValue() <= '0') {
                return i;
            }
        }

        return ALL_CELLS_TAKEN;
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
}
