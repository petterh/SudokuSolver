package com.hesselberg.sudokusolver;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hesselberg.sudokusolver.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TEST_BOARD =
            "       1 " +
            "8        " +
            "    67  2" +
            " 6  82  3" +
            "9  4 5   " +
            "     3 81" +
            "3  5     " +
            "  5   1 6" +
            "4  2  73 ";

    private ActivityMainBinding binding;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final SudokuBoardRecognizer boardRecognizer = new SudokuBoardRecognizer();

    /** Puzzle currently shown / solved. */
    private String currentBoard = TEST_BOARD;

    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCamera();
                } else {
                    Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<Void> takePicturePreview =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    showCaptureConfirmDialog(bitmap);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnCapture.setOnClickListener(v -> startCapture());
        binding.btnSolve.setOnClickListener(v -> solveBoard());

        List<TextView> textViews = getTextViews();
        int count = 0;
        for (TextView textView : textViews) {
            final int i = Board.convertToStringIndex(count++);
            textView.setOnClickListener(v ->
                    Toast.makeText(MainActivity.this, "Edit " + i, Toast.LENGTH_SHORT).show());
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
        try {
            takePicturePreview.launch(null);
        } catch (Exception e) {
            Toast.makeText(this, R.string.camera_unavailable, Toast.LENGTH_LONG).show();
        }
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

    /**
     * Milestone 2: OCR the confirmed photo into an 81-character board and display it.
     */
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
        showBoard(currentBoard, true);

        int clues = SudokuBoardRecognizer.countClues(board);
        Toast.makeText(this, getString(R.string.recognition_success, clues), Toast.LENGTH_SHORT).show();

        if (!new Board(board).cluesAreConsistent()) {
            Toast.makeText(this, R.string.recognition_invalid, Toast.LENGTH_LONG).show();
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
                    showBoard(currentBoard, false);
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

    private void showBoard(@NonNull String solution, boolean setBold) {
        List<TextView> textViews = getTextViews();
        for (int i = 0; i < Board.DIM * Board.DIM; i++) {
            int textViewIndex = Board.convertToTextViewIndex(i);
            TextView textView = textViews.get(textViewIndex);
            char[] ch = new char[1];
            ch[0] = solution.charAt(i);
            textView.setText(ch, 0, 1);
            if (setBold && ch[0] != ' ') {
                textView.setTypeface(null, Typeface.BOLD);
                textView.setTextColor(0xff000000);
            } else if (!setBold) {
                textView.setTypeface(null, Typeface.NORMAL);
            }
        }
    }
}
