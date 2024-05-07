package com.georgeneokq.befake;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ConcurrentCamera;
import androidx.camera.core.DisplayOrientedMeteringPointFactory;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.georgeneokq.befake.components.FlashOverlay;
import com.georgeneokq.befake.util.Util;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private PreviewView subPreviewView;
    private PreviewView mainPreviewView;

    private Button btnCapture;
    private ImageButton btnSettings;

    private ImageCapture frontImageCapture, backImageCapture;

    private FlashOverlay flashOverlay;

    private Camera frontCamera, backCamera;

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;

    private boolean backFrontSwapped = false;

    private final String IMAGE_DIR = Paths.get(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath(),
            "BeFake").toString();

    private String TMP_DIR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!Util.isSettingsInitialized(this)) {
            Util.resetSettings(this);
        }

        TMP_DIR = Paths.get(this.getCacheDir().getAbsolutePath(), "BeFake").toString();

        // Clear all temp files upon starting this activity.
        deleteTempFiles();

        // Initialize MediaPlayer to play camera sound effect
        mediaPlayer = MediaPlayer.create(this, R.raw.camera);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Initialize sub (smaller) preview view to preview back camera
        subPreviewView = findViewById(R.id.subPreviewView);

        // Initialize main (larger) preview view to preview front camera
        mainPreviewView = findViewById(R.id.mainPreviewView);

        // View for flash effect
        flashOverlay = new FlashOverlay(findViewById(R.id.flashOverlay));

        btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> {
            Util.vibrateTapLight(this);
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        btnCapture = findViewById(R.id.btnCapture);
        btnCapture.setOnClickListener(v -> capture());

        mainPreviewView.setOnTouchListener((v, event) -> {
            v.performClick();
            switch(event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    return true;
                case MotionEvent.ACTION_UP:
                    MeteringPointFactory factory = new DisplayOrientedMeteringPointFactory(
                            getDisplay(), frontCamera.getCameraInfo(),
                            (float) mainPreviewView.getWidth(), (float) mainPreviewView.getHeight()
                    );
                    MeteringPoint autoFocusPoint = factory.createPoint(event.getX(), event.getY());
                    frontCamera.getCameraControl().startFocusAndMetering(
                        new FocusMeteringAction.Builder(autoFocusPoint, FocusMeteringAction.FLAG_AF)
                                .disableAutoCancel()
                                .build()
                    );
            }
            return true;
        });

        subPreviewView.setOnTouchListener((v, event) -> {
            v.performClick();
            switch(event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    return true;
                case MotionEvent.ACTION_UP:
                    MeteringPointFactory factory = new DisplayOrientedMeteringPointFactory(
                            getDisplay(), backCamera.getCameraInfo(),
                            (float) subPreviewView.getWidth(), (float) subPreviewView.getHeight()
                    );
                    MeteringPoint autoFocusPoint = factory.createPoint(event.getX(), event.getY());
                    backCamera.getCameraControl().startFocusAndMetering(
                            new FocusMeteringAction.Builder(autoFocusPoint, FocusMeteringAction.FLAG_AF)
                                    .disableAutoCancel()
                                    .build()
                    );
            }
            return true;
        });

        if(allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );
        }
    }

    @SuppressLint("RestrictedApi")
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview.SurfaceProvider backSurfaceProvider, frontSurfaceProvider;

                if(!backFrontSwapped) {
                    frontSurfaceProvider = mainPreviewView.getSurfaceProvider();
                    backSurfaceProvider = subPreviewView.getSurfaceProvider();
                } else {
                    frontSurfaceProvider = subPreviewView.getSurfaceProvider();
                    backSurfaceProvider = mainPreviewView.getSurfaceProvider();
                }

                // Set up preview use case
                Preview backPreview = new Preview.Builder().build();
                backPreview.setSurfaceProvider(backSurfaceProvider);

                Preview frontPreview = new Preview.Builder().build();
                frontPreview.setSurfaceProvider(frontSurfaceProvider);

                // Set up image capture use case
                backImageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                frontImageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                // Get front and back camera selectors
                CameraSelector backCameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                CameraSelector frontCameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                cameraProvider.unbindAll();

                // Set up camera configs, add preview and image capture use cases
                ConcurrentCamera.SingleCameraConfig frontCameraConfig = new ConcurrentCamera.SingleCameraConfig(
                        frontCameraSelector,
                        new UseCaseGroup.Builder()
                                .addUseCase(frontPreview)
                                .addUseCase(frontImageCapture)
                                .build(),
                        this);

                ConcurrentCamera.SingleCameraConfig backCameraConfig = new ConcurrentCamera.SingleCameraConfig(
                        backCameraSelector,
                        new UseCaseGroup.Builder()
                                .addUseCase(backPreview)
                                .addUseCase(backImageCapture)
                                .build(),
                        this);

                // Bind configs to lifecycle
                List<ConcurrentCamera.SingleCameraConfig> configs = new ArrayList<>();
                configs.add(frontCameraConfig);
                configs.add(backCameraConfig);
                ConcurrentCamera concurrentCamera = cameraProvider.bindToLifecycle(configs);

                for(Camera camera : concurrentCamera.getCameras()) {
                    CameraSelector cameraSelector = camera.getCameraInfo().getCameraSelector();
                    if(cameraSelector.getLensFacing() == CameraSelector.LENS_FACING_FRONT)
                        frontCamera = camera;
                    else
                        backCamera = camera;
                }

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void swapCameraPreview() {
        backFrontSwapped = !backFrontSwapped;
    }

    // To keep track of when both captures are complete
    int imageCount = 0;

    private synchronized void incrementImageCount() {
        imageCount++;
    }

    private void onCapture(String frontFilePath, String backFilePath) {
        playSoundEffect();
        flashOverlay.flash();

        Intent intent = new Intent(this, CapturePreviewActivity.class);
        intent.putExtra("frontFilePath", frontFilePath);
        intent.putExtra("backFilePath", backFilePath);
        startActivity(intent);
    }

    private void capture() {
        Util.vibrateTap(this);

        ensureDirectoriesExist();

        // When image count hits 2, that means both cameras have completed capture operation
        imageCount = 0;

        String frontFilePath = Paths.get(TMP_DIR, String.format("%d_front.png", System.currentTimeMillis())).toString();
        ImageCapture.OutputFileOptions frontOutputFile = new ImageCapture.OutputFileOptions.Builder(
                new File(frontFilePath)
        ).build();

        String backFilePath = Paths.get(TMP_DIR, String.format("%d_back.png", System.currentTimeMillis())).toString();
        ImageCapture.OutputFileOptions backOutputFile = new ImageCapture.OutputFileOptions.Builder(
                new File(backFilePath)
        ).build();

        frontImageCapture.takePicture(frontOutputFile, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                incrementImageCount();
                if(imageCount == 2) {
                    onCapture(frontFilePath, backFilePath);
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                exception.printStackTrace();
            }
        });
        backImageCapture.takePicture(backOutputFile, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                incrementImageCount();
                if(imageCount == 2) {
                    onCapture(frontFilePath, backFilePath);
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                exception.printStackTrace();
            }
        });
    }

    private void playSoundEffect() {
        if(audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL && mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Release MediaPlayer resources
        if(mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void ensureDirectoriesExist() {
        Path[] directories = new Path[] {
                Paths.get(IMAGE_DIR),
                Paths.get(TMP_DIR)
        };

        for(Path dir : directories) {
            try {
                Files.createDirectory(dir);
            } catch (IOException e) {}
        }
    }

    private void deleteTempFiles() {
        File tmpDir = new File(TMP_DIR);
        if(!tmpDir.exists())
            return;

        File[] files = tmpDir.listFiles();
        for(File file : files) {
            file.delete();
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to use the camera", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
