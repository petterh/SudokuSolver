package com.hesselberg.sudokusolver;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

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

    private Unbinder unbinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        unbinder = ButterKnife.bind(this);
    }

    @Override
    protected void onDestroy() {
        unbinder.unbind();
        super.onDestroy();
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

    @OnClick(R.id.btn_capture)
    void captureClicked() {
        List<TextView> textViews = getTextViews(this.<LinearLayout>findViewById(R.id.board));
        for (int i = 0; i < Board.DIM * Board.DIM; i++) {
            int textViewIndex = Board.convertToTextViewIndex(i);
            TextView textView = textViews.get(textViewIndex);
            char[] ch = new char[1];
            ch[0] = TEST_BOARD.charAt(i);
            textView.setText(ch, 0, 1);
            if (ch[0] != ' ') {
                textView.setTypeface(null, Typeface.BOLD);
                textView.setTextColor(0xff000000);
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class SolveTask extends AsyncTask<String, String, List<String>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
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
            super.onPostExecute(solutions);
            String solution = solutions.get(0);
            List<TextView> textViews = getTextViews(findViewById(R.id.board));
            for (int i = 0; i < Board.DIM * Board.DIM; i++) {
                int textViewIndex = Board.convertToTextViewIndex(i);
                TextView textView = textViews.get(textViewIndex);
                char[] ch = new char[1];
                ch[0] = solution.charAt(i);
                textView.setText(ch, 0, 1);
            }

            enableButtons(true);
        }

        private void enableButtons(boolean enable) {
            findViewById(R.id.btn_capture).setEnabled(enable);
            findViewById(R.id.btn_solve).setEnabled(enable);
        }
    }

    @OnClick(R.id.btn_solve)
    void solveClicked() {
        new SolveTask().execute(TEST_BOARD);
    }
}
