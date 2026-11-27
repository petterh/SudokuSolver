package com.hesselberg.sudokusolver;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BoardHistoryTest {

    @Test
    public void pushPop_restoresPriorSnapshot() {
        BoardHistory history = new BoardHistory();
        boolean[] cluesA = new boolean[Board.DIM * Board.DIM];
        cluesA[0] = true;
        boolean[] cluesB = new boolean[Board.DIM * Board.DIM];
        cluesB[1] = true;

        history.push(SampleBoards.EMPTY, cluesA);
        history.push(SampleBoards.SUDOKU_SAMPLE, cluesB);

        BoardHistory.Snapshot undone = history.pop();
        assertNotNull(undone);
        assertEquals(SampleBoards.SUDOKU_SAMPLE, undone.board);
        assertTrue(undone.clues[1]);
        assertFalse(undone.clues[0]);

        BoardHistory.Snapshot earlier = history.pop();
        assertNotNull(earlier);
        assertEquals(SampleBoards.EMPTY, earlier.board);
        assertTrue(earlier.clues[0]);
        assertNull(history.pop());
    }

    @Test
    public void push_skipsDuplicateTop() {
        BoardHistory history = new BoardHistory();
        boolean[] clues = new boolean[Board.DIM * Board.DIM];
        history.push(SampleBoards.EMPTY, clues);
        history.push(SampleBoards.EMPTY, clues);
        assertEquals(1, history.size());
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

        assertEquals(2, history.size());
        assertEquals('2', history.pop().board.charAt(0));
        assertEquals('1', history.pop().board.charAt(0));
        assertNull(history.pop());
    }
}
