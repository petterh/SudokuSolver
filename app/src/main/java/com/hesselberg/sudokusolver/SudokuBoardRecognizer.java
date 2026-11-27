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
 * Milestone 2: OCR a photo of a Sudoku grid into the 81-character board format
 * used by {@link Board} (digits {@code '1'}–{@code '9'} or space).
 *
 * <p>Assumes a fairly straight-on photo. Digits are mapped into a 9×9 grid using
 * their bounding-box centers, fitted to the bounds of all detected digits.
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

        DigitHit(char digit, float centerX, float centerY) {
            this.digit = digit;
            this.centerX = centerX;
            this.centerY = centerY;
        }
    }

    private final TextRecognizer textRecognizer =
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

    public void recognize(@NonNull Bitmap bitmap, @NonNull Executor executor, @NonNull Callback callback) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        Task<Text> task = textRecognizer.process(image);
        task.addOnSuccessListener(executor, text -> {
            try {
                String board = boardFromText(text);
                if (countClues(board) == 0) {
                    callback.onFailure(new IllegalStateException("No digits recognized in the photo"));
                    return;
                }
                callback.onSuccess(board);
            } catch (RuntimeException e) {
                callback.onFailure(e);
            }
        });
        task.addOnFailureListener(executor, callback::onFailure);
    }

    public void close() {
        textRecognizer.close();
    }

    @VisibleForTesting
    @NonNull
    static String boardFromText(@NonNull Text text) {
        List<DigitHit> hits = collectDigitHits(text);
        return boardFromHits(hits);
    }

    @VisibleForTesting
    @NonNull
    static String boardFromHits(@NonNull List<DigitHit> hits) {
        char[] cells = new char[Board.DIM * Board.DIM];
        Arrays.fill(cells, ' ');
        if (hits.isEmpty()) {
            return new String(cells);
        }

        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (DigitHit hit : hits) {
            minX = Math.min(minX, hit.centerX);
            maxX = Math.max(maxX, hit.centerX);
            minY = Math.min(minY, hit.centerY);
            maxY = Math.max(maxY, hit.centerY);
        }

        // Pad so edge digits don't clamp into a single border row/column.
        float width = Math.max(maxX - minX, 1f);
        float height = Math.max(maxY - minY, 1f);
        float padX = width / (Board.DIM * 2f);
        float padY = height / (Board.DIM * 2f);
        minX -= padX;
        maxX += padX;
        minY -= padY;
        maxY += padY;
        width = maxX - minX;
        height = maxY - minY;

        for (DigitHit hit : hits) {
            int col = clamp((int) ((hit.centerX - minX) / width * Board.DIM), 0, Board.DIM - 1);
            int row = clamp((int) ((hit.centerY - minY) / height * Board.DIM), 0, Board.DIM - 1);
            cells[row * Board.DIM + col] = hit.digit;
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
        String digits = raw.replaceAll("[^1-9]", "");
        if (digits.isEmpty()) {
            return;
        }
        if (digits.length() == 1) {
            addDigitHit(hits, digits, box);
            return;
        }
        // Split a multi-digit element evenly across its bounding box.
        float slice = box.width() / (float) digits.length();
        for (int i = 0; i < digits.length(); i++) {
            float centerX = box.left + slice * (i + 0.5f);
            float centerY = box.exactCenterY();
            hits.add(new DigitHit(digits.charAt(i), centerX, centerY));
        }
    }

    private static void addDigitHit(
            @NonNull List<DigitHit> hits, @Nullable String raw, @Nullable Rect box) {
        if (raw == null || box == null || raw.isEmpty()) {
            return;
        }
        char ch = raw.charAt(0);
        if (ch >= '1' && ch <= '9') {
            hits.add(new DigitHit(ch, box.exactCenterX(), box.exactCenterY()));
        }
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
