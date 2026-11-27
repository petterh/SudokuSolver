package com.hesselberg.sudokusolver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Dims the preview outside a centered square and draws a light 9×9 guide grid
 * so the puzzle can be framed for capture / OCR.
 */
public final class BoardFrameOverlay extends View {

    private static final float FRAME_FRACTION = 0.86f;

    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF frame = new RectF();

    public BoardFrameOverlay(@NonNull Context context) {
        super(context);
        init();
    }

    public BoardFrameOverlay(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BoardFrameOverlay(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_HARDWARE, null);
        dimPaint.setColor(0x99000000);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dp(2.5f));
        borderPaint.setColor(0xEEFFFFFF);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(1f));
        gridPaint.setColor(0x66FFFFFF);
    }

    /** Centered square used as the framing guide, in view coordinates. */
    @NonNull
    public RectF getFrameRect() {
        return new RectF(frame);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float size = Math.min(w, h) * FRAME_FRACTION;
        float left = (w - size) / 2f;
        float top = (h - size) / 2f;
        frame.set(left, top, left + size, top + size);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), dimPaint);
        canvas.drawRect(frame, clearPaint);
        canvas.drawRect(frame, borderPaint);

        float cell = frame.width() / Board.DIM;
        for (int i = 1; i < Board.DIM; i++) {
            float x = frame.left + i * cell;
            float y = frame.top + i * cell;
            boolean thick = i % 3 == 0;
            gridPaint.setStrokeWidth(thick ? dp(1.8f) : dp(1f));
            gridPaint.setColor(thick ? 0xAAFFFFFF : 0x55FFFFFF);
            canvas.drawLine(x, frame.top, x, frame.bottom, gridPaint);
            canvas.drawLine(frame.left, y, frame.right, y, gridPaint);
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
