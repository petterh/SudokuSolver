package com.hesselberg.sudokusolver;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

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
 * End-to-end OCR against {@code assets/SudokuSample.jpg}, matching {@link SampleBoards#SUDOKU_SAMPLE}.
 */
@RunWith(AndroidJUnit4.class)
public class SudokuBoardRecognizerInstrumentedTest {

    private SudokuBoardRecognizer recognizer;
    private ExecutorService executor;

    @Before
    public void setUp() {
        assertTrue("OpenCV failed to load", SudokuBoardExtractor.ensureOpenCvLoaded());
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
    public void recognize_sudokuSample_matchesExpectedBoard() throws Exception {
        Bitmap bitmap = loadSampleBitmap();
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

            assertTrue("OCR timed out", latch.await(60, TimeUnit.SECONDS));
        } finally {
            bitmap.recycle();
        }

        Exception failure = error.get();
        assertNull(failure == null ? null : failure.toString(), failure);
        assertEquals(SampleBoards.SUDOKU_SAMPLE, board.get());
    }

    private static Bitmap loadSampleBitmap() throws IOException {
        Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
        try (InputStream in = testContext.getAssets().open("SudokuSample.jpg")) {
            return BitmapFactory.decodeStream(in);
        }
    }
}
