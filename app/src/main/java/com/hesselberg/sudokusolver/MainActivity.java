package com.hesselberg.sudokusolver;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hesselberg.sudokusolver.databinding.ActivityMainBinding;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TEST_BOARD =
            "     3 18" +
            "6     425" +
            "   5     " +
            "  2 3   4" +
            " 91  8 6 " +
            "         " +
            "     463 " +
            "95   2   " +
            "2       1";

    private static final int CAPTURE_MAX_EDGE_PX = 1600;

    private ActivityMainBinding binding;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final SudokuBoardRecognizer boardRecognizer = new SudokuBoardRecognizer();

    /** Puzzle currently shown / solved. */
    private String currentBoard = SampleBoards.SUDOKU_SAMPLE;
    /** True for cells treated as given clues (shown bold). */
    private final boolean[] clueCells = new boolean[Board.DIM * Board.DIM];

    private Uri captureUri;
    private File captureFile;

    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCamera();
                } else {
                    Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<Uri> takePicture =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (!success || captureUri == null) {
                    return;
                }
                Bitmap bitmap = loadCaptureBitmap(captureUri);
                deleteCaptureFile();
                if (bitmap != null) {
                    showCaptureConfirmDialog(bitmap);
                } else {
                    Toast.makeText(this, R.string.capture_failed, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnCapture.setOnClickListener(v -> startCapture());
        binding.btnSolve.setOnClickListener(v -> solveBoard());
        // Warm up native OpenCV so the first capture does not pay load cost mid-OCR.
        backgroundExecutor.execute(SudokuBoardExtractor::ensureOpenCvLoaded);

        markCluesFromBoard(currentBoard);
        List<TextView> textViews = getTextViews();
        int count = 0;
        for (TextView textView : textViews) {
            final int stringIndex = Board.convertToStringIndex(count++);
            textView.setOnClickListener(v -> editCell(stringIndex));
        }
    }

    @Override
    protected void onDestroy() {
        backgroundExecutor.shutdownNow();
        boardRecognizer.close();
        deleteCaptureFile();
        super.onDestroy();
    }

    private void startCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            captureUri = createCaptureUri();
            takePicture.launch(captureUri);
        } catch (Exception e) {
            Toast.makeText(this, R.string.camera_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    @NonNull
    private Uri createCaptureUri() throws IOException {
        File dir = new File(getCacheDir(), "capture");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Unable to create capture directory");
        }
        captureFile = File.createTempFile("sudoku_", ".jpg", dir);
        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", captureFile);
    }

    @Nullable
    private Bitmap loadCaptureBitmap(@NonNull Uri uri) {
        try {
            // Camera JPEGs often store orientation in EXIF; BitmapFactory ignores it.
            // TakePicturePreview returned an upright bitmap, which is why OCR was better before.
            int orientation = ExifInterface.ORIENTATION_NORMAL;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in != null) {
                    ExifInterface exif = new ExifInterface(in);
                    orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                }
            }

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) {
                    return null;
                }
                BitmapFactory.decodeStream(in, null, bounds);
            }

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = calculateInSampleSize(bounds, CAPTURE_MAX_EDGE_PX, CAPTURE_MAX_EDGE_PX);
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap decoded;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) {
                    return null;
                }
                decoded = BitmapFactory.decodeStream(in, null, opts);
            }
            if (decoded == null) {
                return null;
            }
            return applyExifOrientation(decoded, orientation);
        } catch (IOException e) {
            return null;
        }
    }

    @NonNull
    private static Bitmap applyExifOrientation(@NonNull Bitmap bitmap, int orientation) {
        int rotationDegrees = 0;
        boolean flipX = false;
        boolean flipY = false;
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                flipX = true;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotationDegrees = 180;
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                flipY = true;
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                rotationDegrees = 90;
                flipX = true;
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotationDegrees = 90;
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                rotationDegrees = 270;
                flipX = true;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotationDegrees = 270;
                break;
            case ExifInterface.ORIENTATION_NORMAL:
            case ExifInterface.ORIENTATION_UNDEFINED:
            default:
                return bitmap;
        }

        Matrix matrix = new Matrix();
        matrix.postScale(flipX ? -1f : 1f, flipY ? -1f : 1f);
        if (rotationDegrees != 0) {
            matrix.postRotate(rotationDegrees);
        }
        Bitmap oriented = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (oriented != bitmap) {
            bitmap.recycle();
        }
        return oriented;
    }

    private static int calculateInSampleSize(
            @NonNull BitmapFactory.Options options, int maxWidth, int maxHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        while ((height / inSampleSize) > maxHeight || (width / inSampleSize) > maxWidth) {
            inSampleSize *= 2;
        }
        return Math.max(1, inSampleSize);
    }

    private void deleteCaptureFile() {
        if (captureFile != null) {
            //noinspection ResultOfMethodCallIgnored
            captureFile.delete();
            captureFile = null;
        }
        captureUri = null;
    }

    private void showCaptureConfirmDialog(@NonNull Bitmap bitmap) {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_capture_confirm, null, false);
        ImageView preview = content.findViewById(R.id.capture_preview);
        preview.setImageBitmap(bitmap);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.capture_preview_title)
                .setView(content)
                .setPositiveButton(R.string.capture_confirm, (dialog, which) -> recognizeCapturedBoard(bitmap))
                .setNeutralButton(R.string.capture_retake, (dialog, which) -> launchCamera())
                .setNegativeButton(R.string.capture_cancel, null)
                .show();
    }

    private void recognizeCapturedBoard(@NonNull Bitmap bitmap) {
        enableButtons(false);
        Toast.makeText(this, R.string.recognizing_board, Toast.LENGTH_SHORT).show();

        boardRecognizer.recognize(bitmap, backgroundExecutor, new SudokuBoardRecognizer.Callback() {
            @Override
            public void onSuccess(@NonNull String board) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    applyRecognizedBoard(board);
                    enableButtons(true);
                });
            }

            @Override
            public void onFailure(@NonNull Exception error) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    Toast.makeText(MainActivity.this, R.string.recognition_failed, Toast.LENGTH_LONG).show();
                    enableButtons(true);
                });
            }
        });
    }

    private void applyRecognizedBoard(@NonNull String board) {
        currentBoard = board;
        markCluesFromBoard(board);
        showBoard(currentBoard);

        int clues = SudokuBoardRecognizer.countClues(board);
        Toast.makeText(this, getString(R.string.recognition_success, clues), Toast.LENGTH_SHORT).show();

        if (!new Board(board).cluesAreConsistent()) {
            Toast.makeText(this, R.string.recognition_invalid, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Milestone 3: tap a cell to cycle blank → 1..9 → blank and update {@link #currentBoard}.
     */
    private void editCell(int stringIndex) {
        char[] chars = currentBoard.toCharArray();
        chars[stringIndex] = nextClueValue(chars[stringIndex]);
        currentBoard = new String(chars);
        clueCells[stringIndex] = chars[stringIndex] != ' ';

        List<TextView> textViews = getTextViews();
        int textViewIndex = Board.convertToTextViewIndex(stringIndex);
        applyCellVisual(textViews.get(textViewIndex), chars[stringIndex], clueCells[stringIndex]);
    }

    @VisibleForTesting
    static char nextClueValue(char current) {
        if (current < '1' || current > '9') {
            return '1';
        }
        if (current == '9') {
            return ' ';
        }
        return (char) (current + 1);
    }

    private void markCluesFromBoard(@NonNull String board) {
        for (int i = 0; i < clueCells.length; i++) {
            char c = i < board.length() ? board.charAt(i) : ' ';
            clueCells[i] = c >= '1' && c <= '9';
        }
    }

    private void clearClueMarks() {
        for (int i = 0; i < clueCells.length; i++) {
            clueCells[i] = false;
        }
    }

    private void solveBoard() {
        enableButtons(false);
        final String boardToSolve = currentBoard;
        backgroundExecutor.execute(() -> {
            List<String> solutions = new ArrayList<>(1);
            new Board(boardToSolve).solve(solutions);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (!solutions.isEmpty()) {
                    currentBoard = solutions.get(0);
                    clearClueMarks();
                    showBoard(currentBoard);
                } else {
                    Toast.makeText(this, "No solution found", Toast.LENGTH_SHORT).show();
                }
                enableButtons(true);
            });
        });
    }

    @NonNull
    private List<TextView> getTextViews() {
        return getTextViews(binding.board);
    }

    @NonNull
    private List<TextView> getTextViews(ViewGroup root) {
        List<TextView> textViews = new ArrayList<>();
        int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = root.getChildAt(i);
            if (child instanceof ViewGroup) {
                textViews.addAll(getTextViews((ViewGroup) child));
            } else if (child instanceof TextView) {
                textViews.add((TextView) child);
            }
        }

        return textViews;
    }

    private void enableButtons(boolean enable) {
        binding.btnCapture.setEnabled(enable);
        binding.btnSolve.setEnabled(enable);
    }

    private void showBoard(@NonNull String board) {
        List<TextView> textViews = getTextViews();
        for (int i = 0; i < Board.DIM * Board.DIM; i++) {
            int textViewIndex = Board.convertToTextViewIndex(i);
            applyCellVisual(textViews.get(textViewIndex), board.charAt(i), clueCells[i]);
        }
    }

    private void applyCellVisual(@NonNull TextView textView, char value, boolean clue) {
        char[] ch = new char[]{value};
        textView.setText(ch, 0, 1);
        if (clue && value != ' ') {
            textView.setTypeface(null, Typeface.BOLD);
            textView.setTextColor(0xff000000);
        } else {
            textView.setTypeface(null, Typeface.NORMAL);
        }
    }
}
