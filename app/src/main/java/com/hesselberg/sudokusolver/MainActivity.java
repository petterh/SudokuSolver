package com.hesselberg.sudokusolver;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnCapture.setOnClickListener(v -> showBoard(TEST_BOARD, true));
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

    private void solveBoard() {
        enableButtons(false);
        solverExecutor.execute(() -> {
            List<String> solutions = new ArrayList<>(1);
            new Board(TEST_BOARD).solve(solutions);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                showBoard(solutions.get(0), false);
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
