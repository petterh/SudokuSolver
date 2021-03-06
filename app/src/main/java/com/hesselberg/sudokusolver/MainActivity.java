package com.hesselberg.sudokusolver;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.AsyncTask;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnCapture.setOnClickListener(v -> showBoard(TEST_BOARD, true));
        binding.btnSolve.setOnClickListener(v -> new SolveTask().execute(TEST_BOARD));

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
        super.onDestroy();
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

    @SuppressLint("StaticFieldLeak")
    private class SolveTask extends AsyncTask<String, String, List<String>> {
        @Override
        protected void onPreExecute() {
            enableButtons(false);
        }

        @Override
        protected List<String> doInBackground(String... strings) {
            List<String> solutions = new ArrayList<>(1);
            new Board(TEST_BOARD).solve(solutions);
            return solutions;
        }

        @Override
        protected void onPostExecute(List<String> solutions) {
            showBoard(solutions.get(0), false);
            enableButtons(true);
        }
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
