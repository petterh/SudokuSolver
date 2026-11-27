package com.hesselberg.sudokusolver;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;
import java.io.InputStream;

/**
 * Decodes capture/gallery photos the same way the UI does: EXIF-correct, optionally
 * downscale, then optional framing crop. Shared so instrumented tests match production.
 */
public final class CaptureImageLoader {

    /** Longest edge kept after decode; high enough to keep newspaper-page boards detectable. */
    public static final int MAX_EDGE_PX = 2560;

    private CaptureImageLoader() {
    }

    @Nullable
    public static Bitmap load(
            @NonNull ContentResolver resolver,
            @NonNull Uri uri,
            @Nullable RectF frameNormalized) {
        try {
            int orientation = ExifInterface.ORIENTATION_NORMAL;
            try (InputStream in = resolver.openInputStream(uri)) {
                if (in != null) {
                    ExifInterface exif = new ExifInterface(in);
                    orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                }
            }

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = resolver.openInputStream(uri)) {
                if (in == null) {
                    return null;
                }
                BitmapFactory.decodeStream(in, null, bounds);
            }

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = calculateInSampleSize(bounds, MAX_EDGE_PX, MAX_EDGE_PX);
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap decoded;
            try (InputStream in = resolver.openInputStream(uri)) {
                if (in == null) {
                    return null;
                }
                decoded = BitmapFactory.decodeStream(in, null, opts);
            }
            if (decoded == null) {
                return null;
            }
            Bitmap oriented = applyExifOrientation(decoded, orientation);
            return CaptureCrop.cropToNormalizedFrame(oriented, frameNormalized);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Decode a local JPEG the same way gallery photos are loaded (for instrumented tests).
     */
    @VisibleForTesting
    @Nullable
    static Bitmap loadFromStream(@NonNull InputStream boundsStream, @NonNull InputStream decodeStream)
            throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(boundsStream, null, bounds);

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = calculateInSampleSize(bounds, MAX_EDGE_PX, MAX_EDGE_PX);
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeStream(decodeStream, null, opts);
    }

    @VisibleForTesting
    static int calculateInSampleSize(
            @NonNull BitmapFactory.Options options, int maxWidth, int maxHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        while ((height / inSampleSize) > maxHeight || (width / inSampleSize) > maxWidth) {
            inSampleSize *= 2;
        }
        return Math.max(1, inSampleSize);
    }

    @NonNull
    private static Bitmap applyExifOrientation(@NonNull Bitmap bitmap, int orientation) {
        int rotationDegrees = 0;
        boolean flipX = false;
        boolean flipY = false;
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                flipX = true;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotationDegrees = 180;
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                flipY = true;
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                rotationDegrees = 90;
                flipX = true;
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotationDegrees = 90;
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                rotationDegrees = 270;
                flipX = true;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotationDegrees = 270;
                break;
            case ExifInterface.ORIENTATION_NORMAL:
            case ExifInterface.ORIENTATION_UNDEFINED:
            default:
                return bitmap;
        }

        Matrix matrix = new Matrix();
        matrix.postScale(flipX ? -1f : 1f, flipY ? -1f : 1f);
        if (rotationDegrees != 0) {
            matrix.postRotate(rotationDegrees);
        }
        Bitmap oriented = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (oriented != bitmap) {
            bitmap.recycle();
        }
        return oriented;
    }
}
