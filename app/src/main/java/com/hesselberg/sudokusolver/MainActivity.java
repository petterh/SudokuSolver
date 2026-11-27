package com.hesselberg.sudokusolver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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

    private static final int CAPTURE_MAX_EDGE_PX = 1600;

    private ActivityMainBinding binding;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final SudokuBoardRecognizer boardRecognizer = new SudokuBoardRecognizer();

    /** Puzzle currently shown / solved. */
    private String currentBoard = SampleBoards.SUDOKU_SAMPLE;
    /** True for cells treated as given clues (shown bold). */
    private final boolean[] clueCells = new boolean[Board.DIM * Board.DIM];

    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCamera();
                } else {
                    Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<Intent> captureBoard =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                Uri uri = result.getData().getData();
                if (uri == null) {
                    String uriString = result.getData().getStringExtra(CaptureActivity.EXTRA_PHOTO_URI);
                    if (uriString != null) {
                        uri = Uri.parse(uriString);
                    }
                }
                if (uri == null) {
                    Toast.makeText(this, R.string.capture_failed, Toast.LENGTH_LONG).show();
                    return;
                }
                String path = result.getData().getStringExtra(CaptureActivity.EXTRA_PHOTO_PATH);
                RectF frame = readCaptureFrame(result.getData());
                Bitmap bitmap = loadCaptureBitmap(uri, frame);
                deleteCaptureFile(path);
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
        captureBoard.launch(new Intent(this, CaptureActivity.class));
    }

    @Nullable
    private static RectF readCaptureFrame(@Nullable Intent data) {
        if (data == null
                || !data.hasExtra(CaptureActivity.EXTRA_FRAME_LEFT)
                || !data.hasExtra(CaptureActivity.EXTRA_FRAME_TOP)
                || !data.hasExtra(CaptureActivity.EXTRA_FRAME_RIGHT)
                || !data.hasExtra(CaptureActivity.EXTRA_FRAME_BOTTOM)) {
            return null;
        }
        return new RectF(
                data.getFloatExtra(CaptureActivity.EXTRA_FRAME_LEFT, 0f),
                data.getFloatExtra(CaptureActivity.EXTRA_FRAME_TOP, 0f),
                data.getFloatExtra(CaptureActivity.EXTRA_FRAME_RIGHT, 1f),
                data.getFloatExtra(CaptureActivity.EXTRA_FRAME_BOTTOM, 1f));
    }

    @Nullable
    private Bitmap loadCaptureBitmap(@NonNull Uri uri, @Nullable RectF frameNormalized) {
        try {
            // Camera JPEGs often store orientation in EXIF; BitmapFactory ignores it.
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
            Bitmap oriented = applyExifOrientation(decoded, orientation);
            return CaptureCrop.cropToNormalizedFrame(oriented, frameNormalized);
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

    private void deleteCaptureFile(@Nullable String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        //noinspection ResultOfMethodCallIgnored
        new File(path).delete();
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

    /** Opens a digit keypad so OCR mistakes can be fixed in one tap. */
    private void editCell(int stringIndex) {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_digit_keypad, null, false);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.edit_cell_title)
                .setView(content)
                .setNegativeButton(R.string.capture_cancel, null);
        final AlertDialog dialog = builder.create();

        int[] digitIds = {
                R.id.digit_1, R.id.digit_2, R.id.digit_3,
                R.id.digit_4, R.id.digit_5, R.id.digit_6,
                R.id.digit_7, R.id.digit_8, R.id.digit_9
        };
        for (int i = 0; i < digitIds.length; i++) {
            final char digit = (char) ('1' + i);
            content.findViewById(digitIds[i]).setOnClickListener(v -> {
                setCellValue(stringIndex, digit);
                dialog.dismiss();
            });
        }
        content.findViewById(R.id.digit_clear).setOnClickListener(v -> {
            setCellValue(stringIndex, ' ');
            dialog.dismiss();
        });
        dialog.show();
    }

    private void setCellValue(int stringIndex, char value) {
        currentBoard = withCellValue(currentBoard, stringIndex, value);
        clueCells[stringIndex] = value != ' ';

        List<TextView> textViews = getTextViews();
        int textViewIndex = Board.convertToTextViewIndex(stringIndex);
        applyCellVisual(textViews.get(textViewIndex), value, clueCells[stringIndex]);
    }

    @VisibleForTesting
    @NonNull
    static String withCellValue(@NonNull String board, int stringIndex, char value) {
        char[] chars = board.toCharArray();
        if (stringIndex < 0 || stringIndex >= chars.length) {
            return board;
        }
        chars[stringIndex] = value;
        return new String(chars);
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
