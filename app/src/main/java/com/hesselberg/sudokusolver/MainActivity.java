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
        LinearLayout view = findViewById(R.id.board);
        List<TextView> textViews = getTextViews(view);
        int i = 0;
        for (TextView textView : textViews) {
            char[] ch = new char[1];
            ch[0] = Board.TEST_BOARD.charAt(i++);
            textView.setText(ch, 0, 1);
            if (ch[0] != ' ') {
                Typeface typeface = textView.getTypeface();
                textView.setTypeface(null, Typeface.BOLD);
                textView.setTextColor(0xff000000);
            }
        }
    }

    @OnClick(R.id.btn_solve)
    void solveClicked() {
        LinearLayout view = findViewById(R.id.board);
        List<TextView> textViews = getTextViews(view);
        int i = 0;
        for (TextView textView : textViews) {
            CharSequence text = textView.getText();
            if (text.length() != 0 && text.charAt(0) == ' ') {
                char[] ch = new char[1];
                int value = i++ % 9 + 1;
                ch[0] = Integer.toString(value).charAt(0);
                textView.setText(ch, 0, 1);
            } else {
                Log.d(TAG, "solveClicked: ");
            }
        }
    }
}