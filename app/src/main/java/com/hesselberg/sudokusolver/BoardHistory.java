package com.hesselberg.sudokusolver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * Undo stack of board + clue-flag snapshots.
 */
public final class BoardHistory {

    public static final class Snapshot {
        @NonNull public final String board;
        @NonNull public final boolean[] clues;

        Snapshot(@NonNull String board, @NonNull boolean[] clues) {
            this.board = board;
            this.clues = Arrays.copyOf(clues, clues.length);
        }
    }

    private final int maxDepth;
    private final ArrayDeque<Snapshot> stack = new ArrayDeque<>();

    public BoardHistory() {
        this(30);
    }

    @VisibleForTesting
    BoardHistory(int maxDepth) {
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth must be >= 1");
        }
        this.maxDepth = maxDepth;
    }

    public void push(@NonNull String board, @NonNull boolean[] clues) {
        if (board.length() != Board.DIM * Board.DIM || clues.length != Board.DIM * Board.DIM) {
            return;
        }
        Snapshot top = stack.peek();
        if (top != null && top.board.equals(board) && Arrays.equals(top.clues, clues)) {
            return;
        }
        stack.push(new Snapshot(board, clues));
        while (stack.size() > maxDepth) {
            stack.removeLast();
        }
    }

    public boolean canUndo() {
        return !stack.isEmpty();
    }

    @Nullable
    public Snapshot pop() {
        return stack.poll();
    }

    public void clear() {
        stack.clear();
    }

    @VisibleForTesting
    int size() {
        return stack.size();
    }
}
