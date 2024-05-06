package com.georgeneokq.befake;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ConcurrentCamera;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private PreviewView backPreviewView;
    private PreviewView frontPreviewView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backPreviewView = findViewById(R.id.backPreviewView);
        frontPreviewView = findViewById(R.id.frontPreviewView);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Set up preview use case
                Preview backPreview = new Preview.Builder().build();
                backPreview.setSurfaceProvider(backPreviewView.getSurfaceProvider());

                Preview frontPreview = new Preview.Builder().build();
                frontPreview.setSurfaceProvider(frontPreviewView.getSurfaceProvider());

                // Set up image capture use case
                ImageCapture backImageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                ImageCapture frontImageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                // Get front and back camera selectors
                CameraSelector backCameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                CameraSelector frontCameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                cameraProvider.unbindAll();

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

                List<ConcurrentCamera.SingleCameraConfig> configs = new ArrayList<>();
                configs.add(frontCameraConfig);
                configs.add(backCameraConfig);
                cameraProvider.bindToLifecycle(configs);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
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
