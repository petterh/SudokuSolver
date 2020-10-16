package com.hesselberg.sudokusolver;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

    public static List<TextView> getTextViews(ViewGroup root) {
        final ArrayList<TextView> result = new ArrayList<>();
        int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = root.getChildAt(i);
            if (child instanceof ViewGroup) {
                result.addAll(getTextViews((ViewGroup) child));
            } else if (child instanceof TextView) {
                result.add((TextView) child);
            }
        }

        return result;
    }

    @OnClick(R.id.btn_capture)
    void captureClicked() {
        LinearLayout view = findViewById(R.id.board);
        List<TextView> textViews = getTextViews(view);
        int i = 0;
        for (TextView textView : textViews) {
            try {
                char[] ch = new char[1];
                ch[0] = Board.TEST_BOARD.charAt(i++);
                textView.setText(ch, 0, 1);
            } catch (Throwable t) {
                Log.e(TAG, "captureClicked: ", t);
            }
        }
    }
}
