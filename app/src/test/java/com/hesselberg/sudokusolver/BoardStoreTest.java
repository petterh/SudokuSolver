package com.hesselberg.sudokusolver;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BoardStoreTest {

    @Test
    public void encodeDecodeClues_roundTrips() {
        boolean[] clues = new boolean[Board.DIM * Board.DIM];
        clues[0] = true;
        clues[40] = true;
        clues[80] = true;

        String encoded = BoardStore.encodeClues(clues);
        assertEquals(81, encoded.length());
        assertEquals('1', encoded.charAt(0));
        assertEquals('0', encoded.charAt(1));
        assertEquals('1', encoded.charAt(40));
        assertEquals('1', encoded.charAt(80));

        assertArrayEquals(clues, BoardStore.decodeClues(encoded));
    }

    @Test
    public void decodeClues_rejectsInvalidPayloads() {
        assertNull(BoardStore.decodeClues(null));
        assertNull(BoardStore.decodeClues("101"));
        assertNull(BoardStore.decodeClues(SampleBoards.EMPTY.replace(' ', '2')));
    }
}
