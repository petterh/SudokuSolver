package com.hesselberg.sudokusolver;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BoardHistoryTest {

    @Test
    public void undoRedo_roundTrips() {
        BoardHistory history = new BoardHistory();
        boolean[] emptyClues = new boolean[Board.DIM * Board.DIM];
        boolean[] sampleClues = new boolean[Board.DIM * Board.DIM];
        sampleClues[0] = true;

        history.push(SampleBoards.EMPTY, emptyClues);
        // Live board is now the sample; undo should restore EMPTY and stash sample on redo.
        BoardHistory.Snapshot undone = history.undo(SampleBoards.SUDOKU_SAMPLE, sampleClues);
        assertNotNull(undone);
        assertEquals(SampleBoards.EMPTY, undone.board);
        assertTrue(history.canRedo());
        assertFalse(history.canUndo());

        BoardHistory.Snapshot redone = history.redo(SampleBoards.EMPTY, emptyClues);
        assertNotNull(redone);
        assertEquals(SampleBoards.SUDOKU_SAMPLE, redone.board);
        assertTrue(redone.clues[0]);
        assertTrue(history.canUndo());
        assertFalse(history.canRedo());
    }

    @Test
    public void push_clearsRedoBranch() {
        BoardHistory history = new BoardHistory();
        boolean[] clues = new boolean[Board.DIM * Board.DIM];
        history.push(SampleBoards.EMPTY, clues);
        history.undo(SampleBoards.SUDOKU_SAMPLE, clues);
        assertTrue(history.canRedo());

        char[] edited = SampleBoards.EMPTY.toCharArray();
        edited[0] = '5';
        history.push(SampleBoards.EMPTY, clues);
        assertFalse(history.canRedo());
        assertEquals(1, history.undoSize());
        assertEquals(0, history.redoSize());
    }

    @Test
    public void push_skipsDuplicateTop() {
        BoardHistory history = new BoardHistory();
        boolean[] clues = new boolean[Board.DIM * Board.DIM];
        history.push(SampleBoards.EMPTY, clues);
        history.push(SampleBoards.EMPTY, clues);
        assertEquals(1, history.undoSize());
    }

    @Test
    public void push_evictsOldestWhenOverCapacity() {
        BoardHistory history = new BoardHistory(2);
        boolean[] clues = new boolean[Board.DIM * Board.DIM];
        history.push(SampleBoards.EMPTY, clues);
        char[] one = SampleBoards.EMPTY.toCharArray();
        one[0] = '1';
        history.push(new String(one), clues);
        char[] two = SampleBoards.EMPTY.toCharArray();
        two[0] = '2';
        history.push(new String(two), clues);

        assertEquals(2, history.undoSize());
        assertEquals('2', history.undo(SampleBoards.EMPTY, clues).board.charAt(0));
        assertEquals('1', history.undo(SampleBoards.EMPTY, clues).board.charAt(0));
        assertNull(history.undo(SampleBoards.EMPTY, clues));
    }
}
