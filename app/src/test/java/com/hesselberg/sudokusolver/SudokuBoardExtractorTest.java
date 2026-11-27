package com.hesselberg.sudokusolver;

import org.junit.Test;
import org.opencv.core.Point;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SudokuBoardExtractorTest {

    @Test
    public void orderQuadPoints_ordersCornersClockwiseFromTopLeft() {
        Point[] unordered = {
                new Point(80, 10),   // top-right
                new Point(10, 10),   // top-left
                new Point(10, 90),   // bottom-left
                new Point(90, 80)    // bottom-right
        };

        Point[] ordered = SudokuBoardExtractor.orderQuadPoints(unordered);

        assertEquals(10, ordered[0].x, 0.01);
        assertEquals(10, ordered[0].y, 0.01);
        assertEquals(80, ordered[1].x, 0.01);
        assertEquals(10, ordered[1].y, 0.01);
        assertEquals(90, ordered[2].x, 0.01);
        assertEquals(80, ordered[2].y, 0.01);
        assertEquals(10, ordered[3].x, 0.01);
        assertEquals(90, ordered[3].y, 0.01);
    }

    @Test
    public void isRoughlySquare_acceptsSquareRejectsSkinnyQuad() {
        Point[] square = {
                new Point(0, 0),
                new Point(100, 0),
                new Point(100, 100),
                new Point(0, 100)
        };
        Point[] skinny = {
                new Point(0, 0),
                new Point(200, 0),
                new Point(200, 40),
                new Point(0, 40)
        };

        assertTrue(SudokuBoardExtractor.isRoughlySquare(square));
        assertFalse(
                "Skinny rectangles must not count as boards",
                SudokuBoardExtractor.isRoughlySquare(skinny));
    }

    @Test
    public void scoreBoardCandidate_prefersCenteredBoardWhenSizesAreSimilar() {
        Point[] centered = {
                new Point(400, 400),
                new Point(600, 400),
                new Point(600, 600),
                new Point(400, 600)
        };
        Point[] offCenter = {
                new Point(750, 50),
                new Point(950, 50),
                new Point(950, 250),
                new Point(750, 250)
        };

        double centeredScore = SudokuBoardExtractor.scoreBoardCandidate(
                centered, 200 * 200, 1000, 1000, 1.0);
        double offCenterScore = SudokuBoardExtractor.scoreBoardCandidate(
                offCenter, 200 * 200, 1000, 1000, 1.0);

        assertTrue(centeredScore > offCenterScore);
    }

    @Test
    public void scoreBoardCandidate_prefersInnerBoardOverFullFrameBorder() {
        Point[] innerBoard = {
                new Point(150, 150),
                new Point(850, 150),
                new Point(850, 850),
                new Point(150, 850)
        };
        Point[] pageBorder = {
                new Point(2, 2),
                new Point(998, 2),
                new Point(998, 998),
                new Point(2, 998)
        };

        double innerScore = SudokuBoardExtractor.scoreBoardCandidate(
                innerBoard, 700 * 700, 1000, 1000, 1.0);
        double borderScore = SudokuBoardExtractor.scoreBoardCandidate(
                pageBorder, 996 * 996, 1000, 1000, 1.0);

        assertTrue(innerScore > borderScore);
    }

    @Test
    public void borderTouchPenalty_penalizesEdgeHuggingCorners() {
        Point[] floating = {
                new Point(200, 200),
                new Point(800, 200),
                new Point(800, 800),
                new Point(200, 800)
        };
        Point[] onBorder = {
                new Point(1, 1),
                new Point(999, 1),
                new Point(999, 999),
                new Point(1, 999)
        };

        assertEquals(1.0, SudokuBoardExtractor.borderTouchPenalty(floating, 1000, 1000), 0.001);
        assertEquals(0.25, SudokuBoardExtractor.borderTouchPenalty(onBorder, 1000, 1000), 0.001);
    }
}
