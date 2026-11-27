package com.hesselberg.sudokusolver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * Undo / redo stacks of board + clue-flag snapshots.
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
    private final ArrayDeque<Snapshot> undoStack = new ArrayDeque<>();
    private final ArrayDeque<Snapshot> redoStack = new ArrayDeque<>();

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

    /** Records the pre-change state and clears the redo branch. */
    public void push(@NonNull String board, @NonNull boolean[] clues) {
        if (!isValid(board, clues)) {
            return;
        }
        redoStack.clear();
        pushCapped(undoStack, new Snapshot(board, clues));
    }

    /**
     * Pushes the live board onto redo, then returns the previous undo snapshot.
     */
    @Nullable
    public Snapshot undo(@NonNull String currentBoard, @NonNull boolean[] currentClues) {
        Snapshot previous = undoStack.poll();
        if (previous == null) {
            return null;
        }
        if (isValid(currentBoard, currentClues)) {
            pushCapped(redoStack, new Snapshot(currentBoard, currentClues));
        }
        return previous;
    }

    /**
     * Pushes the live board onto undo, then returns the next redo snapshot.
     */
    @Nullable
    public Snapshot redo(@NonNull String currentBoard, @NonNull boolean[] currentClues) {
        Snapshot next = redoStack.poll();
        if (next == null) {
            return null;
        }
        if (isValid(currentBoard, currentClues)) {
            pushCapped(undoStack, new Snapshot(currentBoard, currentClues));
        }
        return next;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    @VisibleForTesting
    int undoSize() {
        return undoStack.size();
    }

    @VisibleForTesting
    int redoSize() {
        return redoStack.size();
    }

    private void pushCapped(@NonNull ArrayDeque<Snapshot> stack, @NonNull Snapshot snapshot) {
        Snapshot top = stack.peek();
        if (top != null && top.board.equals(snapshot.board) && Arrays.equals(top.clues, snapshot.clues)) {
            return;
        }
        stack.push(snapshot);
        while (stack.size() > maxDepth) {
            stack.removeLast();
        }
    }

    private static boolean isValid(@NonNull String board, @NonNull boolean[] clues) {
        return board.length() == Board.DIM * Board.DIM && clues.length == Board.DIM * Board.DIM;
    }
}
