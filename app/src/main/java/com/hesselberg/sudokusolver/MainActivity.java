package com.hesselberg.sudokusolver;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Board board = new Board();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Unbinder unbinder = ButterKnife.bind(this);
    }

    public List<TextView> getTextViews(ViewGroup root) {
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
            ch[0] = Board.TEST_BOARD.charAt(i);
            textView.setText(ch, 0, 1);
            if (ch[0] != ' ') {
                textView.setTypeface(null, Typeface.BOLD);
                textView.setTextColor(0xff000000);
            }
        }
    }

    @OnClick(R.id.btn_solve)
    void solveClicked() {
        List<TextView> textViews = getTextViews(this.<LinearLayout>findViewById(R.id.board));
        for (int i = 0; i < Board.DIM * Board.DIM; i++) {
            int textViewIndex = Board.convertToTextViewIndex(i);
            TextView textView = textViews.get(textViewIndex);
            CharSequence text = textView.getText();
            if (text.length() != 0 && text.charAt(0) == ' ') {
                char[] ch = new char[1];
                ch[0] = Integer.toString(i % 9 + 1).charAt(0);
                textView.setText(ch, 0, 1);
            } else {
                Log.d(TAG, "solveClicked: ");
            }
        }
    }
}
