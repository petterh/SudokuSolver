package com.hesselberg.sudokusolver;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SudokuBoardRecognizerTest {

    @Test
    public void boardFromHits_mapsCentersIntoNineByNineGrid() {
        // Centers in a full 0..9 coordinate frame so fitted bounds match cell indices.
        List<SudokuBoardRecognizer.DigitHit> hits = Arrays.asList(
                hit('1', 8.5f, 0.5f),
                hit('8', 0.5f, 1.5f),
                hit('6', 4.5f, 2.5f),
                hit('7', 5.5f, 2.5f),
                hit('2', 8.5f, 2.5f),
                // Anchor opposite corner so Y span covers the whole grid
                hit('4', 0.5f, 8.5f)
        );

        String board = SudokuBoardRecognizer.boardFromHits(hits);

        assertEquals(81, board.length());
        assertEquals('1', board.charAt(0 * 9 + 8));
        assertEquals('8', board.charAt(1 * 9 + 0));
        assertEquals('6', board.charAt(2 * 9 + 4));
        assertEquals('7', board.charAt(2 * 9 + 5));
        assertEquals('2', board.charAt(2 * 9 + 8));
        assertEquals('4', board.charAt(8 * 9 + 0));
        assertEquals(6, SudokuBoardRecognizer.countClues(board));
    }

    @Test
    public void boardFromHits_emptyYieldsBlankBoard() {
        String board = SudokuBoardRecognizer.boardFromHits(List.of());
        assertEquals(81, board.length());
        assertTrue(board.trim().isEmpty());
        assertEquals(0, SudokuBoardRecognizer.countClues(board));
    }

    @Test
    public void filterHits_dropsOversizedAndFarAwayNoise() {
        List<SudokuBoardRecognizer.DigitHit> hits = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                hits.add(new SudokuBoardRecognizer.DigitHit(
                        '1', col + 0.5f, row + 0.5f, 100f));
            }
        }
        // Page title / background noise
        hits.add(new SudokuBoardRecognizer.DigitHit('9', 50f, 50f, 100f));
        hits.add(new SudokuBoardRecognizer.DigitHit('8', 1.5f, 1.5f, 10_000f));

        List<SudokuBoardRecognizer.DigitHit> filtered = SudokuBoardRecognizer.filterHits(hits);
        assertEquals(9, filtered.size());
        for (SudokuBoardRecognizer.DigitHit hit : filtered) {
            assertTrue(hit.centerX < 10f);
            assertTrue(hit.centerY < 10f);
            assertTrue(hit.area < 1000f);
        }
    }

    private static SudokuBoardRecognizer.DigitHit hit(char digit, float col, float row) {
        return new SudokuBoardRecognizer.DigitHit(digit, col, row);
    }
}
