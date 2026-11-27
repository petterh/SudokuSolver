package com.hesselberg.sudokusolver;

import android.graphics.Bitmap;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * OCR a photo of a Sudoku grid into the 81-character board format used by {@link Board}
 * (digits {@code '1'}–{@code '9'} or space).
 *
 * <p>Prefers an OpenCV board extract (perspective-warped square). Digits are then mapped
 * onto a fixed 9×9 grid covering that square, so outside page noise never enters OCR.
 */
public final class SudokuBoardRecognizer {

    public interface Callback {
        void onSuccess(@NonNull String board);

        void onFailure(@NonNull Exception error);
    }

    /** A recognized digit and its center in image coordinates. */
    @VisibleForTesting
    static final class DigitHit {
        final char digit;
        final float centerX;
        final float centerY;
        final float area;

        DigitHit(char digit, float centerX, float centerY) {
            this(digit, centerX, centerY, 1f);
        }

        DigitHit(char digit, float centerX, float centerY, float area) {
            this.digit = digit;
            this.centerX = centerX;
            this.centerY = centerY;
            this.area = Math.max(area, 1f);
        }
    }

    private static final class CellCandidate {
        final char digit;
        final float distSq;

        CellCandidate(char digit, float distSq) {
            this.digit = digit;
            this.distSq = distSq;
        }
    }

    private final TextRecognizer textRecognizer =
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    public void recognize(@NonNull Bitmap bitmap, @NonNull Executor executor, @NonNull Callback callback) {
        executor.execute(() -> {
            Bitmap extracted = null;
            try {
                extracted = SudokuBoardExtractor.extractBoard(bitmap);
            } catch (RuntimeException e) {
                callback.onFailure(e);
                return;
            }

            final boolean recycleExtracted = extracted != null;
            final Bitmap ocrInput = recycleExtracted ? extracted : bitmap;
            final boolean useFullFrameMapping = recycleExtracted;

            processBitmap(ocrInput)
                    .addOnSuccessListener(executor, text -> {
                        try {
                            List<DigitHit> hits = filterCellSizedHits(
                                    collectDigitHits(text),
                                    ocrInput.getWidth(),
                                    ocrInput.getHeight());
                            String board = useFullFrameMapping
                                    ? boardFromHitsFullFrame(
                                            hits, ocrInput.getWidth(), ocrInput.getHeight())
                                    : boardFromHits(hits);
                            if (countClues(board) == 0) {
                                callback.onFailure(
                                        new IllegalStateException("No digits recognized in the photo"));
                            } else {
                                callback.onSuccess(board);
                            }
                        } catch (RuntimeException e) {
                            callback.onFailure(e);
                        } finally {
                            if (recycleExtracted && !ocrInput.isRecycled()) {
                                ocrInput.recycle();
                            }
                        }
                    })
                    .addOnFailureListener(executor, error -> {
                        if (recycleExtracted && !ocrInput.isRecycled()) {
                            ocrInput.recycle();
                        }
                        callback.onFailure(error);
                    });
        });
    }

    private Task<Text> processBitmap(@NonNull Bitmap bitmap) {
        return textRecognizer.process(InputImage.fromBitmap(bitmap, 0));
    }

    public void close() {
        textRecognizer.close();
    }

    @VisibleForTesting
    @NonNull
    static String boardFromText(@NonNull Text text) {
        return boardFromHits(collectDigitHits(text));
    }

    /**
     * Map digits using the full image as the Sudoku square (after perspective warp).
     */
    @VisibleForTesting
    @NonNull
    static String boardFromHitsFullFrame(@NonNull List<DigitHit> hits, int width, int height) {
        // Larger inset discards border glyphs that slipped into a slightly oversized warp.
        float insetX = width / (Board.DIM * 12f);
        float insetY = height / (Board.DIM * 12f);
        return mapHitsToBoard(hits, insetX, insetY, width - insetX, height - insetY);
    }

    /**
     * Fallback when board extraction fails: fit the grid to the digit cluster.
     */
    @VisibleForTesting
    @NonNull
    static String boardFromHits(@NonNull List<DigitHit> hits) {
        if (hits.isEmpty()) {
            char[] empty = new char[Board.DIM * Board.DIM];
            Arrays.fill(empty, ' ');
            return new String(empty);
        }

        List<DigitHit> clustered = discardSpatialOutliers(hits);
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (DigitHit hit : clustered) {
            minX = Math.min(minX, hit.centerX);
            maxX = Math.max(maxX, hit.centerX);
            minY = Math.min(minY, hit.centerY);
            maxY = Math.max(maxY, hit.centerY);
        }

        float width = Math.max(maxX - minX, 1f);
        float height = Math.max(maxY - minY, 1f);
        float padX = width / (Board.DIM * 2f);
        float padY = height / (Board.DIM * 2f);
        return mapHitsToBoard(clustered, minX - padX, minY - padY, maxX + padX, maxY + padY);
    }

    /**
     * Keep glyphs whose box is roughly a Sudoku cell digit; drop titles, page numbers, and noise.
     */
    @VisibleForTesting
    @NonNull
    static List<DigitHit> filterCellSizedHits(
            @NonNull List<DigitHit> hits, int imageWidth, int imageHeight) {
        if (hits.isEmpty() || imageWidth <= 0 || imageHeight <= 0) {
            return hits;
        }
        float cell = Math.min(imageWidth, imageHeight) / (float) Board.DIM;
        float minArea = (cell * 0.10f) * (cell * 0.10f);
        float maxArea = (cell * 1.20f) * (cell * 1.20f);
        List<DigitHit> filtered = new ArrayList<>(hits.size());
        for (DigitHit hit : hits) {
            if (hit.area >= minArea && hit.area <= maxArea) {
                filtered.add(hit);
            }
        }
        return filtered.isEmpty() ? hits : filtered;
    }

    /**
     * Drop digits far from the main cluster (margin numbers outside the puzzle).
     */
    @VisibleForTesting
    @NonNull
    static List<DigitHit> discardSpatialOutliers(@NonNull List<DigitHit> hits) {
        if (hits.size() < 6) {
            return hits;
        }
        float cx = 0f;
        float cy = 0f;
        for (DigitHit hit : hits) {
            cx += hit.centerX;
            cy += hit.centerY;
        }
        cx /= hits.size();
        cy /= hits.size();

        float[] distSq = new float[hits.size()];
        for (int i = 0; i < hits.size(); i++) {
            float dx = hits.get(i).centerX - cx;
            float dy = hits.get(i).centerY - cy;
            distSq[i] = dx * dx + dy * dy;
        }
        float[] sorted = distSq.clone();
        Arrays.sort(sorted);
        float cutoff = sorted[(int) (sorted.length * 0.85f)] * 2.25f;

        List<DigitHit> kept = new ArrayList<>(hits.size());
        for (int i = 0; i < hits.size(); i++) {
            if (distSq[i] <= cutoff) {
                kept.add(hits.get(i));
            }
        }
        return kept.size() >= 4 ? kept : hits;
    }

    @NonNull
    private static String mapHitsToBoard(
            @NonNull List<DigitHit> hits, float minX, float minY, float maxX, float maxY) {
        char[] cells = new char[Board.DIM * Board.DIM];
        Arrays.fill(cells, ' ');
        float width = Math.max(maxX - minX, 1f);
        float height = Math.max(maxY - minY, 1f);

        CellCandidate[] best = new CellCandidate[Board.DIM * Board.DIM];
        for (DigitHit hit : hits) {
            int col = clamp((int) ((hit.centerX - minX) / width * Board.DIM), 0, Board.DIM - 1);
            int row = clamp((int) ((hit.centerY - minY) / height * Board.DIM), 0, Board.DIM - 1);
            float cellCenterX = minX + (col + 0.5f) * width / Board.DIM;
            float cellCenterY = minY + (row + 0.5f) * height / Board.DIM;
            float dx = hit.centerX - cellCenterX;
            float dy = hit.centerY - cellCenterY;
            float distSq = dx * dx + dy * dy;
            int index = row * Board.DIM + col;
            if (best[index] == null || distSq < best[index].distSq) {
                best[index] = new CellCandidate(hit.digit, distSq);
            }
        }

        for (int i = 0; i < best.length; i++) {
            if (best[i] != null) {
                cells[i] = best[i].digit;
            }
        }
        return new String(cells);
    }

    @NonNull
    private static List<DigitHit> collectDigitHits(@NonNull Text text) {
        List<DigitHit> hits = new ArrayList<>();
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                for (Text.Element element : line.getElements()) {
                    List<? extends Text.Symbol> symbols = element.getSymbols();
                    if (symbols != null && !symbols.isEmpty()) {
                        for (Text.Symbol symbol : symbols) {
                            addDigitHit(hits, symbol.getText(), symbol.getBoundingBox());
                        }
                    } else {
                        addDigitsFromElement(hits, element.getText(), element.getBoundingBox());
                    }
                }
            }
        }
        return hits;
    }

    private static void addDigitsFromElement(
            @NonNull List<DigitHit> hits, @Nullable String raw, @Nullable Rect box) {
        if (raw == null || box == null) {
            return;
        }
        List<Character> digits = new ArrayList<>();
        for (int i = 0; i < raw.length(); i++) {
            Character digit = normalizeSudokuDigit(String.valueOf(raw.charAt(i)));
            if (digit != null) {
                digits.add(digit);
            }
        }
        if (digits.isEmpty()) {
            return;
        }
        if (digits.size() == 1) {
            addDigitHit(hits, String.valueOf(digits.get(0)), box);
            return;
        }
        float slice = box.width() / (float) digits.size();
        float sliceArea = Math.max(slice * box.height(), 1f);
        for (int i = 0; i < digits.size(); i++) {
            float centerX = box.left + slice * (i + 0.5f);
            float centerY = box.exactCenterY();
            hits.add(new DigitHit(digits.get(i), centerX, centerY, sliceArea));
        }
    }

    private static void addDigitHit(
            @NonNull List<DigitHit> hits, @Nullable String raw, @Nullable Rect box) {
        if (box == null) {
            return;
        }
        Character digit = normalizeSudokuDigit(raw);
        if (digit == null) {
            return;
        }
        float area = Math.max(box.width(), 1) * Math.max(box.height(), 1);
        hits.add(new DigitHit(digit, box.exactCenterX(), box.exactCenterY(), area));
    }

    /**
     * Map OCR glyphs to Sudoku digits. ML Kit often reads {@code 9} as {@code g}/{@code q}.
     */
    @VisibleForTesting
    @Nullable
    static Character normalizeSudokuDigit(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        char ch = trimmed.charAt(0);
        if (ch >= '1' && ch <= '9') {
            return ch;
        }
        int unicodeDigit = Character.digit(ch, 10);
        if (unicodeDigit >= 1 && unicodeDigit <= 9) {
            return (char) ('0' + unicodeDigit);
        }
        return switch (Character.toLowerCase(ch)) {
            case 'g', 'q' -> '9';
            case 's' -> '5';
            case 'b' -> '8';
            case 'z' -> '2';
            case 'l', 'i', '|' -> '1';
            default -> null;
        };
    }

    static int countClues(@NonNull String board) {
        int count = 0;
        for (int i = 0; i < board.length(); i++) {
            char c = board.charAt(i);
            if (c >= '1' && c <= '9') {
                count++;
            }
        }
        return count;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
