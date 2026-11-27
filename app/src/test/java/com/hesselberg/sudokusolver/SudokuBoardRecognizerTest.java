package com.hesselberg.sudokusolver;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SudokuBoardRecognizerTest {

    @Test
    public void boardFromHitsFullFrame_mapsUsingImageExtents() {
        List<SudokuBoardRecognizer.DigitHit> hits = Arrays.asList(
                hit('1', 850f, 50f),
                hit('8', 50f, 150f),
                hit('6', 450f, 250f),
                hit('4', 50f, 850f)
        );

        String board = SudokuBoardRecognizer.boardFromHitsFullFrame(hits, 900, 900);

        assertEquals(81, board.length());
        assertEquals('1', board.charAt(0 * 9 + 8));
        assertEquals('8', board.charAt(1 * 9 + 0));
        assertEquals('6', board.charAt(2 * 9 + 4));
        assertEquals('4', board.charAt(8 * 9 + 0));
        assertEquals(4, SudokuBoardRecognizer.countClues(board));
    }

    @Test
    public void boardFromHits_emptyYieldsBlankBoard() {
        String board = SudokuBoardRecognizer.boardFromHits(List.of());
        assertEquals(81, board.length());
        assertTrue(board.trim().isEmpty());
        assertEquals(0, SudokuBoardRecognizer.countClues(board));
    }

    @Test
    public void normalizeSudokuDigit_mapsNineConfusionsAndUnicode() {
        assertEquals(Character.valueOf('9'), SudokuBoardRecognizer.normalizeSudokuDigit("9"));
        assertEquals(Character.valueOf('9'), SudokuBoardRecognizer.normalizeSudokuDigit("g"));
        assertEquals(Character.valueOf('9'), SudokuBoardRecognizer.normalizeSudokuDigit("Q"));
        assertEquals(Character.valueOf('9'), SudokuBoardRecognizer.normalizeSudokuDigit("９"));
        assertEquals(Character.valueOf('5'), SudokuBoardRecognizer.normalizeSudokuDigit("S"));
        assertNull(SudokuBoardRecognizer.normalizeSudokuDigit("a"));
        assertNull(SudokuBoardRecognizer.normalizeSudokuDigit("0"));
    }

    private static SudokuBoardRecognizer.DigitHit hit(char digit, float col, float row) {
        return new SudokuBoardRecognizer.DigitHit(digit, col, row);
    }
}
