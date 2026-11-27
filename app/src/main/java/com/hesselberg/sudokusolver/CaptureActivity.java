package com.hesselberg.sudokusolver;

import android.content.Intent;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.common.util.concurrent.ListenableFuture;
import com.hesselberg.sudokusolver.databinding.ActivityCaptureBinding;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * In-app CameraX capture with a square framing overlay for Sudoku boards.
 * Returns a content {@link Uri} plus normalized overlay bounds for cropping.
 */
public final class CaptureActivity extends AppCompatActivity {

    public static final String EXTRA_PHOTO_URI = "photo_uri";
    public static final String EXTRA_PHOTO_PATH = "photo_path";
    public static final String EXTRA_FRAME_LEFT = "frame_left";
    public static final String EXTRA_FRAME_TOP = "frame_top";
    public static final String EXTRA_FRAME_RIGHT = "frame_right";
    public static final String EXTRA_FRAME_BOTTOM = "frame_bottom";

    private static final String TAG = "CaptureActivity";

    private ActivityCaptureBinding binding;
    @Nullable
    private ImageCapture imageCapture;
    @Nullable
    private File captureFile;
    private boolean capturing;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCaptureBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnClose.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        binding.btnShutter.setOnClickListener(v -> takePhoto());

        // ViewPort is only available after PreviewView has been measured.
        binding.previewView.post(this::startCamera);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera provider failed", e);
                Toast.makeText(this, R.string.camera_unavailable, Toast.LENGTH_LONG).show();
                setResult(RESULT_CANCELED);
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        cameraProvider.unbindAll();

        ViewPort viewPort = binding.previewView.getViewPort();
        if (viewPort != null) {
            UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                    .setViewPort(viewPort)
                    .addUseCase(preview)
                    .addUseCase(imageCapture)
                    .build();
            cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, useCaseGroup);
        } else {
            cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture);
        }
    }

    private void takePhoto() {
        ImageCapture capture = imageCapture;
        if (capture == null || capturing) {
            return;
        }
        capturing = true;
        binding.btnShutter.setEnabled(false);

        final RectF frameNormalized = CaptureCrop.normalizedFrameFromView(
                binding.frameOverlay.getFrameRect(),
                binding.previewView.getWidth(),
                binding.previewView.getHeight());

        try {
            captureFile = createCaptureFile();
        } catch (IOException e) {
            capturing = false;
            binding.btnShutter.setEnabled(true);
            Toast.makeText(this, R.string.capture_failed, Toast.LENGTH_LONG).show();
            return;
        }

        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(captureFile).build();
        capture.takePicture(
                options,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Uri uri = FileProvider.getUriForFile(
                                CaptureActivity.this,
                                getPackageName() + ".fileprovider",
                                captureFile);
                        Intent data = new Intent().setData(uri);
                        data.putExtra(EXTRA_PHOTO_URI, uri.toString());
                        data.putExtra(EXTRA_PHOTO_PATH, captureFile.getAbsolutePath());
                        data.putExtra(EXTRA_FRAME_LEFT, frameNormalized.left);
                        data.putExtra(EXTRA_FRAME_TOP, frameNormalized.top);
                        data.putExtra(EXTRA_FRAME_RIGHT, frameNormalized.right);
                        data.putExtra(EXTRA_FRAME_BOTTOM, frameNormalized.bottom);
                        data.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        setResult(RESULT_OK, data);
                        finish();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        capturing = false;
                        binding.btnShutter.setEnabled(true);
                        if (captureFile != null) {
                            //noinspection ResultOfMethodCallIgnored
                            captureFile.delete();
                            captureFile = null;
                        }
                        Toast.makeText(
                                        CaptureActivity.this,
                                        R.string.capture_failed,
                                        Toast.LENGTH_LONG)
                                .show();
                    }
                });
    }

    @NonNull
    private File createCaptureFile() throws IOException {
        File dir = new File(getCacheDir(), "capture");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Unable to create capture directory");
        }
        return File.createTempFile("sudoku_", ".jpg", dir);
    }
}
