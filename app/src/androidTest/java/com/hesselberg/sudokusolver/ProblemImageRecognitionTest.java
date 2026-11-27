package com.hesselberg.sudokusolver;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Uses the same decode downscale as gallery/camera ({@link CaptureImageLoader#MAX_EDGE_PX}).
 */
@RunWith(AndroidJUnit4.class)
public class ProblemImageRecognitionTest {

    private static final String TAG = "ProblemImageOCR";

    private SudokuBoardRecognizer recognizer;
    private ExecutorService executor;

    @Before
    public void setUp() {
        assertTrue(SudokuBoardExtractor.ensureOpenCvLoaded());
        recognizer = new SudokuBoardRecognizer();
        executor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() {
        if (recognizer != null) {
            recognizer.close();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    public void recognize_goodImage_matchesProblemBoard() throws Exception {
        assertEquals(SampleBoards.PROBLEM_BOARD, recognizeAsset("good-image.jpg"));
    }

    @Test
    public void recognize_problemImage_matchesProblemBoard() throws Exception {
        String actual = recognizeAsset("problem-image.jpg");
        Log.e(TAG, "expected=\n" + formatBoard(SampleBoards.PROBLEM_BOARD));
        Log.e(TAG, "actual=\n" + formatBoard(actual));
        assertEquals(SampleBoards.PROBLEM_BOARD, actual);
    }

    @Test
    public void extract_problemImage_findsBoardAtAppScale() throws Exception {
        Bitmap bitmap = loadBitmapLikeApp("problem-image.jpg");
        assertNotNull(bitmap);
        Log.e(TAG, "decoded size=" + bitmap.getWidth() + "x" + bitmap.getHeight());
        Bitmap extracted = SudokuBoardExtractor.extractBoard(bitmap);
        assertNotNull("extract must work at gallery decode scale", extracted);
        extracted.recycle();
        bitmap.recycle();
    }

    @NonNull
    private String recognizeAsset(@NonNull String assetName) throws Exception {
        Bitmap bitmap = loadBitmapLikeApp(assetName);
        assertNotNull(bitmap);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> board = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();
        try {
            recognizer.recognize(bitmap, executor, new SudokuBoardRecognizer.Callback() {
                @Override
                public void onSuccess(String result) {
                    board.set(result);
                    latch.countDown();
                }

                @Override
                public void onFailure(Exception e) {
                    error.set(e);
                    latch.countDown();
                }
            });
            assertTrue(latch.await(90, TimeUnit.SECONDS));
        } finally {
            bitmap.recycle();
        }

        Exception failure = error.get();
        assertNull(failure == null ? null : failure.toString(), failure);
        assertNotNull(board.get());
        return board.get();
    }

    private static Bitmap loadBitmapLikeApp(String assetName) throws IOException {
        Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
        try (InputStream boundsIn = testContext.getAssets().open(assetName);
             InputStream decodeIn = testContext.getAssets().open(assetName)) {
            return CaptureImageLoader.loadFromStream(boundsIn, decodeIn);
        }
    }

    private static String formatBoard(String board) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 9; r++) {
            sb.append('[').append(board, r * 9, r * 9 + 9).append("]\n");
        }
        return sb.toString();
    }
}
