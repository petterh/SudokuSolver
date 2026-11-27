package com.hesselberg.sudokusolver;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Finds the Sudoku board quadrilateral in a photo and warps it to a square.
 * Outside noise is removed before OCR by discarding everything outside that quad.
 */
public final class SudokuBoardExtractor {

    private static final String TAG = "SudokuBoardExtractor";
    static final int WARPED_SIZE = 900;
    private static final double MIN_SQUARE_QUALITY = 0.45;
    private static final double[] APPROX_EPSILONS = {0.015, 0.02, 0.03, 0.04};
    private static final int[] ADAPTIVE_BLOCK_SIZES = {11, 21, 31};

    private static boolean openCvReady;

    private SudokuBoardExtractor() {
    }

    public static synchronized boolean ensureOpenCvLoaded() {
        if (openCvReady) {
            return true;
        }
        openCvReady = OpenCVLoader.initLocal();
        if (!openCvReady) {
            Log.e(TAG, "OpenCV failed to load");
        }
        return openCvReady;
    }

    /**
     * @return a square, board-only bitmap, or {@code null} if no plausible board was found
     */
    @Nullable
    public static Bitmap extractBoard(@NonNull Bitmap source) {
        if (!ensureOpenCvLoaded()) {
            return null;
        }

        Mat rgba = new Mat();
        Mat gray = new Mat();
        try {
            Utils.bitmapToMat(source, rgba);
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY);

            Point[] quad = findBestBoardQuadAcrossStrategies(gray, source.getWidth(), source.getHeight());
            if (quad == null) {
                Log.w(TAG, "No board quad found");
                return null;
            }
            return warpBoard(rgba, quad);
        } finally {
            rgba.release();
            gray.release();
        }
    }

    @Nullable
    private static Point[] findBestBoardQuadAcrossStrategies(
            @NonNull Mat gray, int imageWidth, int imageHeight) {
        double bestScore = Double.NEGATIVE_INFINITY;
        Point[] bestQuad = null;

        Mat blurred = new Mat();
        Mat binary = new Mat();
        try {
            Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);

            for (int blockSize : ADAPTIVE_BLOCK_SIZES) {
                Imgproc.adaptiveThreshold(
                        blurred,
                        binary,
                        255,
                        Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                        Imgproc.THRESH_BINARY_INV,
                        blockSize,
                        2);
                strengthenBoardEdges(binary);
                Point[] quad = bestQuadFromBinary(binary, imageWidth, imageHeight);
                if (quad != null) {
                    double area = polygonArea(quad);
                    double score = scoreBoardCandidate(
                            quad, area, imageWidth, imageHeight, squareQuality(quad));
                    if (score > bestScore) {
                        bestScore = score;
                        bestQuad = quad;
                    }
                }
            }

            // Canny fallback helps when adaptive threshold merges the board into page noise.
            Imgproc.Canny(blurred, binary, 50, 150);
            strengthenBoardEdges(binary);
            Point[] cannyQuad = bestQuadFromBinary(binary, imageWidth, imageHeight);
            if (cannyQuad != null) {
                double area = polygonArea(cannyQuad);
                double score = scoreBoardCandidate(
                        cannyQuad, area, imageWidth, imageHeight, squareQuality(cannyQuad));
                if (score > bestScore) {
                    bestQuad = cannyQuad;
                }
            }
        } finally {
            blurred.release();
            binary.release();
        }
        return bestQuad;
    }

    private static void strengthenBoardEdges(@NonNull Mat binary) {
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        try {
            Imgproc.dilate(binary, binary, kernel);
            Imgproc.morphologyEx(binary, binary, Imgproc.MORPH_CLOSE, kernel);
        } finally {
            kernel.release();
        }
    }

    private static double polygonArea(@NonNull Point[] quad) {
        MatOfPoint2f mat = new MatOfPoint2f(quad);
        try {
            return Math.abs(Imgproc.contourArea(mat));
        } finally {
            mat.release();
        }
    }

    @Nullable
    private static Point[] bestQuadFromBinary(
            @NonNull Mat binary, int imageWidth, int imageHeight) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        try {
            Imgproc.findContours(
                    binary, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
            return findBestBoardQuad(contours, imageWidth, imageHeight);
        } finally {
            hierarchy.release();
            for (MatOfPoint contour : contours) {
                contour.release();
            }
        }
    }

    @Nullable
    private static Point[] findBestBoardQuad(
            @NonNull List<MatOfPoint> contours, int imageWidth, int imageHeight) {
        double imageArea = imageWidth * (double) imageHeight;
        // Newspaper pages: primary puzzle is often a modest fraction of the tall frame.
        double minArea = imageArea * 0.04;
        double bestScore = Double.NEGATIVE_INFINITY;
        Point[] bestQuad = null;

        for (MatOfPoint contour : contours) {
            MatOfPoint2f curve = new MatOfPoint2f(contour.toArray());
            try {
                double peri = Imgproc.arcLength(curve, true);
                for (double epsilonFactor : APPROX_EPSILONS) {
                    MatOfPoint2f approx = new MatOfPoint2f();
                    try {
                        Imgproc.approxPolyDP(curve, approx, epsilonFactor * peri, true);
                        Point[] points = approx.toArray();
                        MatOfPoint convexCheck = new MatOfPoint(points);
                        try {
                            if (points.length != 4 || !Imgproc.isContourConvex(convexCheck)) {
                                continue;
                            }
                        } finally {
                            convexCheck.release();
                        }
                        double area = Math.abs(Imgproc.contourArea(approx));
                        if (area < minArea) {
                            continue;
                        }
                        Point[] ordered = orderQuadPoints(points);
                        double quality = squareQuality(ordered);
                        if (quality < MIN_SQUARE_QUALITY) {
                            continue;
                        }
                        double score = scoreBoardCandidate(
                                ordered, area, imageWidth, imageHeight, quality);
                        if (score > bestScore) {
                            bestScore = score;
                            bestQuad = ordered;
                        }
                    } finally {
                        approx.release();
                    }
                }
            } finally {
                curve.release();
            }
        }
        return bestQuad;
    }

    /**
     * Prefer a centered, square board. Pure area ranking fails when the page has several grids.
     * Near-full-frame quads (page/photo borders) and edge-hugging corners are heavily downranked
     * so margin content left in the framing crop does not become the “board”.
     */
    @VisibleForTesting
    static double scoreBoardCandidate(
            @NonNull Point[] orderedQuad,
            double area,
            int imageWidth,
            int imageHeight,
            double squareQuality) {
        double imageArea = imageWidth * (double) imageHeight;
        double areaRatio = area / imageArea;
        // Newspaper pages: primary puzzle is mid-sized; tiny solution thumbnails and
        // near-full-page borders score poorly.
        double areaWeight;
        if (areaRatio > 0.88) {
            areaWeight = areaRatio * 0.15;
        } else if (areaRatio > 0.70) {
            areaWeight = areaRatio * 0.35;
        } else if (areaRatio >= 0.08 && areaRatio <= 0.55) {
            areaWeight = areaRatio * 1.35;
        } else if (areaRatio < 0.08) {
            areaWeight = areaRatio * 0.45;
        } else {
            areaWeight = areaRatio;
        }

        double cx = 0;
        double cy = 0;
        for (Point point : orderedQuad) {
            cx += point.x;
            cy += point.y;
        }
        cx /= orderedQuad.length;
        cy /= orderedQuad.length;
        double dx = cx - imageWidth / 2.0;
        double dy = cy - imageHeight / 2.0;
        double maxDist = Math.hypot(imageWidth / 2.0, imageHeight / 2.0);
        double centerScore = 1.0 - Math.min(1.0, Math.hypot(dx, dy) / maxDist);

        double borderPenalty = borderTouchPenalty(orderedQuad, imageWidth, imageHeight);

        return areaWeight
                * (0.35 + 0.65 * squareQuality)
                * (0.40 + 0.60 * centerScore)
                * borderPenalty
                * imageArea;
    }

    /**
     * Quads with several corners on the image edge are usually the photo/page border.
     *
     * @return 1 for a free-floating board, down toward ~0.25 when most corners hug the border
     */
    @VisibleForTesting
    static double borderTouchPenalty(
            @NonNull Point[] orderedQuad, int imageWidth, int imageHeight) {
        double margin = Math.max(4.0, Math.min(imageWidth, imageHeight) * 0.02);
        int touches = 0;
        for (Point point : orderedQuad) {
            if (point.x <= margin
                    || point.y <= margin
                    || point.x >= imageWidth - margin
                    || point.y >= imageHeight - margin) {
                touches++;
            }
        }
        if (touches >= 3) {
            return 0.25;
        }
        if (touches == 2) {
            return 0.55;
        }
        if (touches == 1) {
            return 0.85;
        }
        return 1.0;
    }

    @VisibleForTesting
    static boolean isRoughlySquare(@NonNull Point[] points) {
        return squareQuality(points) >= MIN_SQUARE_QUALITY;
    }

    /** 1 = perfect square, lower = more distorted / rectangular. */
    @VisibleForTesting
    static double squareQuality(@NonNull Point[] points) {
        double[] sides = new double[4];
        for (int i = 0; i < 4; i++) {
            Point a = points[i];
            Point b = points[(i + 1) % 4];
            sides[i] = Math.hypot(a.x - b.x, a.y - b.y);
        }
        double min = Arrays.stream(sides).min().orElse(1);
        double max = Arrays.stream(sides).max().orElse(1);
        if (min < 1) {
            return 0;
        }
        double sideRatio = min / max;
        double diag1 = Math.hypot(points[0].x - points[2].x, points[0].y - points[2].y);
        double diag2 = Math.hypot(points[1].x - points[3].x, points[1].y - points[3].y);
        double diagRatio = Math.min(diag1, diag2) / Math.max(1.0, Math.max(diag1, diag2));
        return sideRatio * diagRatio;
    }

    /**
     * Orders quad corners as top-left, top-right, bottom-right, bottom-left.
     */
    @VisibleForTesting
    @NonNull
    static Point[] orderQuadPoints(@NonNull Point[] points) {
        Point topLeft = points[0];
        Point topRight = points[0];
        Point bottomRight = points[0];
        Point bottomLeft = points[0];
        double minSum = Double.POSITIVE_INFINITY;
        double maxSum = Double.NEGATIVE_INFINITY;
        double minDiff = Double.POSITIVE_INFINITY;
        double maxDiff = Double.NEGATIVE_INFINITY;

        for (Point point : points) {
            double sum = point.x + point.y;
            double diff = point.x - point.y;
            if (sum < minSum) {
                minSum = sum;
                topLeft = point;
            }
            if (sum > maxSum) {
                maxSum = sum;
                bottomRight = point;
            }
            if (diff > maxDiff) {
                maxDiff = diff;
                topRight = point;
            }
            if (diff < minDiff) {
                minDiff = diff;
                bottomLeft = point;
            }
        }
        return new Point[]{topLeft, topRight, bottomRight, bottomLeft};
    }

    @NonNull
    private static Bitmap warpBoard(@NonNull Mat rgba, @NonNull Point[] orderedQuad) {
        MatOfPoint2f src = new MatOfPoint2f(orderedQuad);
        MatOfPoint2f dst = new MatOfPoint2f(
                new Point(0, 0),
                new Point(WARPED_SIZE - 1, 0),
                new Point(WARPED_SIZE - 1, WARPED_SIZE - 1),
                new Point(0, WARPED_SIZE - 1));
        Mat transform = Imgproc.getPerspectiveTransform(src, dst);
        Mat warped = new Mat(WARPED_SIZE, WARPED_SIZE, CvType.CV_8UC4);
        try {
            Imgproc.warpPerspective(rgba, warped, transform, new Size(WARPED_SIZE, WARPED_SIZE));
            Bitmap output = Bitmap.createBitmap(WARPED_SIZE, WARPED_SIZE, Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(warped, output);
            return output;
        } finally {
            src.release();
            dst.release();
            transform.release();
            warped.release();
        }
    }
}
