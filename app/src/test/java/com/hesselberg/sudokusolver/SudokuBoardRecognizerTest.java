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
    public void boardFromHits_rightPadding_keepsTopRowTwoInColumnSix() {
        // Warped frame is 900px but the printed grid only fills the left ~700px
        // (thick border / shadow on the right). Full-frame mapping would place the
        // top-row 2 one cell left; cluster fitting must keep it in column 6 (index 5).
        float boardSize = 700f;
        float cell = boardSize / 9f;
        List<SudokuBoardRecognizer.DigitHit> hits = Arrays.asList(
                hit('6', 0.5f * cell, 0.5f * cell),
                hit('7', 1.5f * cell, 0.5f * cell),
                hit('2', 5.5f * cell, 0.5f * cell),
                hit('8', 7.5f * cell, 0.5f * cell),
                hit('4', 8.5f * cell, 0.5f * cell),
                hit('8', 0.5f * cell, 1.5f * cell),
                hit('1', 8.5f * cell, 1.5f * cell),
                hit('9', 0.5f * cell, 8.5f * cell),
                hit('7', 5.5f * cell, 8.5f * cell)
        );

        // Full-frame binning on a padded warp shifts that 2 one cell left — cluster fit must not.
        assertEquals('2', SudokuBoardRecognizer.boardFromHitsFullFrame(hits, 900, 900).charAt(0 * 9 + 4));

        String fitted = SudokuBoardRecognizer.boardFromHits(hits);
        assertEquals('2', fitted.charAt(0 * 9 + 5));
        assertEquals(' ', fitted.charAt(0 * 9 + 4));
        assertEquals('6', fitted.charAt(0 * 9 + 0));
        assertEquals('4', fitted.charAt(0 * 9 + 8));
    }

    @Test
    public void boardFromHits_emptyYieldsBlankBoard() {
        String board = SudokuBoardRecognizer.boardFromHits(List.of());
        assertEquals(81, board.length());
        assertTrue(board.trim().isEmpty());
        assertEquals(0, SudokuBoardRecognizer.countClues(board));
    }

    @Test
    public void mergeBoardPreferPrimary_fillsBlanksWithoutOverriding() {
        String primary =
                "6        " +
                "         " +
                "         " +
                "         " +
                "         " +
                "         " +
                "         " +
                "         " +
                "         ";
        String secondary =
                "7        " +
                "8        " +
                "         " +
                "         " +
                "         " +
                "         " +
                "         " +
                "         " +
                "        9";
        String merged = SudokuBoardRecognizer.mergeBoardPreferPrimary(primary, secondary);
        assertEquals('6', merged.charAt(0));
        assertEquals('8', merged.charAt(9));
        assertEquals('9', merged.charAt(80));
    }

    @Test
    public void mergeBoardPreferPrimary_rejectsConflictingFillUnderExistingDigit() {
        // Primary has a 2 in r0c5; a spurious per-cell 2 in r1c5 must not be merged.
        char[] primary = SampleBoards.EMPTY.toCharArray();
        primary[5] = '2';
        char[] secondary = SampleBoards.EMPTY.toCharArray();
        secondary[5 + 9] = '2';
        String merged = SudokuBoardRecognizer.mergeBoardPreferPrimary(
                new String(primary), new String(secondary));
        assertEquals('2', merged.charAt(5));
        assertEquals(' ', merged.charAt(5 + 9));
    }

    @Test
    public void normalizeStrictDigit_rejectsLetterOneConfusions() {
        assertEquals(Character.valueOf('1'), SudokuBoardRecognizer.normalizeStrictDigit("1"));
        assertNull(SudokuBoardRecognizer.normalizeStrictDigit("l"));
        assertNull(SudokuBoardRecognizer.normalizeStrictDigit("|"));
        assertNull(SudokuBoardRecognizer.normalizeStrictDigit("I"));
        assertNull(SudokuBoardRecognizer.normalizeStrictDigit("g"));
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

    @Test
    public void filterCellSizedHits_dropsOversizedMarginGlyphs() {
        // Cell on 900px board ≈ 100px; cell-sized digit ≈ 40x40, title ≈ 200x80.
        List<SudokuBoardRecognizer.DigitHit> hits = Arrays.asList(
                new SudokuBoardRecognizer.DigitHit('5', 450f, 450f, 40f * 40f),
                new SudokuBoardRecognizer.DigitHit('2', 50f, 50f, 200f * 80f)
        );

        List<SudokuBoardRecognizer.DigitHit> filtered =
                SudokuBoardRecognizer.filterCellSizedHits(hits, 900, 900);

        assertEquals(1, filtered.size());
        assertEquals('5', filtered.get(0).digit);
    }

    @Test
    public void discardSpatialOutliers_dropsFarMarginDigit() {
        List<SudokuBoardRecognizer.DigitHit> hits = Arrays.asList(
                hit('1', 100f, 100f),
                hit('2', 200f, 100f),
                hit('3', 300f, 100f),
                hit('4', 100f, 200f),
                hit('5', 200f, 200f),
                hit('6', 300f, 200f),
                hit('7', 100f, 300f),
                hit('8', 200f, 300f),
                hit('9', 900f, 50f) // far margin
        );

        List<SudokuBoardRecognizer.DigitHit> kept =
                SudokuBoardRecognizer.discardSpatialOutliers(hits);

        assertEquals(8, kept.size());
        for (SudokuBoardRecognizer.DigitHit hit : kept) {
            assertTrue(hit.digit != '9' || hit.centerX < 800f);
        }
    }

    private static SudokuBoardRecognizer.DigitHit hit(char digit, float col, float row) {
        return new SudokuBoardRecognizer.DigitHit(digit, col, row);
    }
}
