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
    private final ExecutorService solverExecutor = Executors.newSingleThreadExecutor();

    /** Puzzle currently shown / solved. Capture confirm still loads {@link #TEST_BOARD}. */
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
        solverExecutor.shutdownNow();
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
                .setPositiveButton(R.string.capture_confirm, (dialog, which) -> applyCapturedPuzzlePlaceholder())
                .setNeutralButton(R.string.capture_retake, (dialog, which) -> launchCamera())
                .setNegativeButton(R.string.capture_cancel, null)
                .show();
    }

    /**
     * Milestone 1: after the user confirms a photo, load the hard-coded puzzle.
     * Later milestones will OCR the bitmap into an 81-character board string instead.
     */
    private void applyCapturedPuzzlePlaceholder() {
        currentBoard = TEST_BOARD;
        showBoard(currentBoard, true);
    }

    private void solveBoard() {
        enableButtons(false);
        final String boardToSolve = currentBoard;
        solverExecutor.execute(() -> {
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
            }
        }
    }
}
