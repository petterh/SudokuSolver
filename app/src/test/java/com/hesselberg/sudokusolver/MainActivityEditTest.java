package com.hesselberg.sudokusolver;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MainActivityEditTest {

    @Test
    public void withCellValue_setsDigitAndClear() {
        String blank = SampleBoards.SUDOKU_SAMPLE;
        String withFive = MainActivity.withCellValue(blank, 0, '5');
        assertEquals('5', withFive.charAt(0));
        assertEquals(blank.substring(1), withFive.substring(1));

        String cleared = MainActivity.withCellValue(withFive, 0, ' ');
        assertEquals(' ', cleared.charAt(0));
    }

    @Test
    public void withCellValue_ignoresOutOfRangeIndex() {
        String board = SampleBoards.SUDOKU_SAMPLE;
        assertEquals(board, MainActivity.withCellValue(board, -1, '9'));
        assertEquals(board, MainActivity.withCellValue(board, board.length(), '9'));
    }
}
