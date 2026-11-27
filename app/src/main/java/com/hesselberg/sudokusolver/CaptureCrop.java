package com.hesselberg.sudokusolver;

import android.graphics.Bitmap;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Crops a capture to the framing overlay, using normalized preview coordinates (0–1).
 * With a shared CameraX {@code ViewPort}, those fractions match the visible preview FOV.
 */
public final class CaptureCrop {

    /** Pull crop slightly inside the guide so edge margin outside the board is trimmed. */
    static final float FRAME_INSET_FRACTION = 0.045f;

    private CaptureCrop() {
    }

    /**
     * @param frameNormalized overlay rect as fractions of preview width/height
     * @return cropped bitmap (may recycle {@code source} when a new bitmap is created)
     */
    @Nullable
    static Bitmap cropToNormalizedFrame(@Nullable Bitmap source, @Nullable RectF frameNormalized) {
        if (source == null || frameNormalized == null || source.getWidth() <= 0 || source.getHeight() <= 0) {
            return source;
        }
        float[] inset = insetNormalizedFrame(
                frameNormalized.left,
                frameNormalized.top,
                frameNormalized.right,
                frameNormalized.bottom,
                FRAME_INSET_FRACTION);
        int[] crop = normalizedFrameToPixelRect(
                source.getWidth(),
                source.getHeight(),
                inset[0],
                inset[1],
                inset[2],
                inset[3]);
        if (crop == null) {
            return source;
        }
        Bitmap cropped = Bitmap.createBitmap(source, crop[0], crop[1], crop[2], crop[3]);
        if (cropped != source) {
            source.recycle();
        }
        return cropped;
    }

    @NonNull
    static RectF normalizedFrameFromView(@NonNull RectF framePx, int viewWidth, int viewHeight) {
        float[] n = normalizeViewFrame(
                framePx.left, framePx.top, framePx.right, framePx.bottom, viewWidth, viewHeight);
        return new RectF(n[0], n[1], n[2], n[3]);
    }

    /**
     * @return {@code float[]{left, top, right, bottom}} in 0–1 preview space
     */
    @VisibleForTesting
    @NonNull
    static float[] normalizeViewFrame(
            float leftPx,
            float topPx,
            float rightPx,
            float bottomPx,
            int viewWidth,
            int viewHeight) {
        if (viewWidth <= 0 || viewHeight <= 0) {
            return new float[]{0f, 0f, 1f, 1f};
        }
        return new float[]{
                leftPx / viewWidth,
                topPx / viewHeight,
                rightPx / viewWidth,
                bottomPx / viewHeight
        };
    }

    /**
     * Shrink a normalized frame toward its center by {@code insetFraction} of its size.
     */
    @VisibleForTesting
    @NonNull
    static float[] insetNormalizedFrame(
            float left, float top, float right, float bottom, float insetFraction) {
        float inset = Math.max(0f, Math.min(0.45f, insetFraction));
        float width = right - left;
        float height = bottom - top;
        if (width <= 0f || height <= 0f) {
            return new float[]{left, top, right, bottom};
        }
        float dx = width * inset;
        float dy = height * inset;
        return new float[]{left + dx, top + dy, right - dx, bottom - dy};
    }

    /**
     * @return {@code int[]{x, y, width, height}} or null if the frame is unusable
     */
    @VisibleForTesting
    @Nullable
    static int[] normalizedFrameToPixelRect(
            int imageWidth,
            int imageHeight,
            float left,
            float top,
            float right,
            float bottom) {
        if (imageWidth <= 0 || imageHeight <= 0) {
            return null;
        }
        left = clamp01(left);
        top = clamp01(top);
        right = clamp01(right);
        bottom = clamp01(bottom);
        if (right <= left || bottom <= top) {
            return null;
        }

        int x = Math.round(left * imageWidth);
        int y = Math.round(top * imageHeight);
        int x2 = Math.round(right * imageWidth);
        int y2 = Math.round(bottom * imageHeight);
        x = clamp(x, 0, imageWidth - 1);
        y = clamp(y, 0, imageHeight - 1);
        x2 = clamp(x2, x + 1, imageWidth);
        y2 = clamp(y2, y + 1, imageHeight);
        return new int[]{x, y, x2 - x, y2 - y};
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
