package com.georgeneokq.befake

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.georgeneokq.befake.components.FlashOverlay
import com.georgeneokq.befake.util.Util
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

class MainActivity : AppCompatActivity() {

    private val requestCameraPermissionCode = 1

    private lateinit var subPreviewView: PreviewView
    private lateinit var mainPreviewView: PreviewView

    private lateinit var btnCapture: Button
    private lateinit var btnSettings: ImageButton

    private lateinit var frontImageCapture: ImageCapture
    private lateinit var backImageCapture: ImageCapture

    private lateinit var flashOverlay: FlashOverlay

    private lateinit var frontCamera: Camera
    private lateinit var backCamera: Camera

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var audioManager: AudioManager

    private var backFrontSwapped = false

    // For dragging of subPreviewView
    private var dX = 0f
    private var dY = 0f

    private val imageDir = Paths.get(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath,
        "BeFake"
    ).toString()

    private var tmpDir: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!Util.isSettingsInitialized(this)) {
            Util.resetSettings(this)
        }

        tmpDir = Paths.get(cacheDir.absolutePath, "BeFake").toString()

        // Clear all temp files upon starting this activity.
        deleteTempFiles()

        // Initialize MediaPlayer to play camera sound effect
        mediaPlayer = MediaPlayer.create(this, R.raw.camera)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // Initialize sub (smaller) preview view to preview back camera
        subPreviewView = findViewById(R.id.subPreviewView)

        // Initialize main (larger) preview view to preview front camera
        mainPreviewView = findViewById(R.id.mainPreviewView)

        // View for flash effect
        flashOverlay = FlashOverlay(findViewById(R.id.flashOverlay))

        btnSettings = findViewById(R.id.btnSettings)

        btnSettings.setOnClickListener {
            Util.vibrateTapLight(this)
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnCapture = findViewById(R.id.btnCapture)
        btnCapture.setOnClickListener { capture() }
        mainPreviewView.setOnTouchListener { v, event ->
            v.performClick()
            when(event.action) {
                MotionEvent.ACTION_UP -> {
                    val factory = DisplayOrientedMeteringPointFactory(
                        this.display!!, frontCamera.cameraInfo,
                        mainPreviewView.width.toFloat(), mainPreviewView.height.toFloat()
                    )
                    val autoFocusPoint = factory.createPoint(event.x, event.y)
                    frontCamera.cameraControl.startFocusAndMetering(
                        FocusMeteringAction.Builder(autoFocusPoint, FocusMeteringAction.FLAG_AF)
                            .disableAutoCancel()
                            .build()
                    )
                }
            }
            true
        }

        subPreviewView.setOnTouchListener { v, event ->
            v.performClick()
            val x = event.rawX
            val y = event.rawY
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Get the initial position of the touch
                    dX = v.x - y
                    dY = v.y - y
                }
                MotionEvent.ACTION_MOVE -> {
                    v.animate()
                        .x(x + dX)
                        .y(y + dY)
                        .setDuration(0)
                        .start()
                }
                MotionEvent.ACTION_UP -> {
                    val factory = DisplayOrientedMeteringPointFactory(
                        this.display!!, backCamera.cameraInfo,
                        subPreviewView.width.toFloat(), subPreviewView.height.toFloat()
                    )
                    val autoFocusPoint = factory.createPoint(event.x, event.y)
                    backCamera.cameraControl.startFocusAndMetering(
                        FocusMeteringAction.Builder(autoFocusPoint, FocusMeteringAction.FLAG_AF)
                            .disableAutoCancel()
                            .build()
                    )
                }
            }
            true
        }

        if(allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                requestCameraPermissionCode
            )
        }
    }

    @SuppressLint("RestrictedApi")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
             try {
                 val cameraProvider = cameraProviderFuture.get()
                 val backSurfaceProvider: Preview.SurfaceProvider
                 val frontSurfaceProvider: Preview.SurfaceProvider

                 if (!backFrontSwapped) {
                     frontSurfaceProvider = mainPreviewView.surfaceProvider
                     backSurfaceProvider = subPreviewView.surfaceProvider
                 } else {
                     frontSurfaceProvider = subPreviewView.surfaceProvider
                     backSurfaceProvider = mainPreviewView.surfaceProvider
                 }

                 // Set up preview use case
                 val backPreview = Preview.Builder().build()
                 backPreview.setSurfaceProvider(backSurfaceProvider)

                 val frontPreview = Preview.Builder().build()
                 frontPreview.setSurfaceProvider(frontSurfaceProvider)

                 // Set up image capture use case
                 backImageCapture = ImageCapture.Builder()
                     .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                     .build()

                 frontImageCapture = ImageCapture.Builder()
                     .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                     .build()

                 // Get front and back camera selectors
                 val backCameraSelector = CameraSelector.Builder()
                     .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                     .build()

                 val frontCameraSelector = CameraSelector.Builder()
                     .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                     .build()

                 cameraProvider.unbindAll()

                 val backCameraConfig = ConcurrentCamera.SingleCameraConfig(
                     backCameraSelector,
                     UseCaseGroup.Builder()
                         .addUseCase(backPreview)
                         .addUseCase(backImageCapture)
                         .build(),
                     this)

                 val frontCameraConfig = ConcurrentCamera.SingleCameraConfig(
                     frontCameraSelector,
                     UseCaseGroup.Builder()
                         .addUseCase(frontPreview)
                         .addUseCase(frontImageCapture)
                         .build(),
                     this)

                 val configs = listOf(frontCameraConfig, backCameraConfig)
                 val concurrentCamera = cameraProvider.bindToLifecycle(configs)

                 for(camera in concurrentCamera.cameras) {
                     val cameraSelector = camera.cameraInfo.cameraSelector
                     if(cameraSelector.lensFacing == CameraSelector.LENS_FACING_FRONT)
                         frontCamera = camera
                     else
                         backCamera = camera
                 }

             } catch(exception: Exception) {
                 exception.printStackTrace()
             }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun swapCameraPreview() {
        backFrontSwapped = !backFrontSwapped
    }

    // To keep track of when both captures are complete
    var imageCount = 0

    private fun incrementImageCount() {
        imageCount++
    }

    private fun onCapture(frontFilePath: String, backFilePath: String) {
        flashOverlay.flash()

        val intent = Intent(this, CapturePreviewActivity::class.java)
        intent.putExtra("frontFilePath", frontFilePath)
        intent.putExtra("backFilePath", backFilePath)
        startActivity(intent)
    }

    private fun capture() {
        Util.vibrateTap(this)

        ensureDirectoriesExist()

        // When image count hits 2, that means both cameras have completed capture operation
        imageCount = 0

        val frontFilePath = Paths.get(tmpDir, "${System.currentTimeMillis()}_front.png").toString()
        val frontOutputFile = ImageCapture.OutputFileOptions.Builder(
            File(frontFilePath)
        ).build()

        val backFilePath = Paths.get(tmpDir, "${System.currentTimeMillis()}_back.png").toString()
        val backOutputFile = ImageCapture.OutputFileOptions.Builder(
            File(backFilePath)
        ).build()

        frontImageCapture.takePicture(frontOutputFile, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                incrementImageCount()
                if (imageCount == 2) {
                    onCapture(frontFilePath, backFilePath)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }
        })
        backImageCapture.takePicture(backOutputFile, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                incrementImageCount()
                if (imageCount == 2) {
                    onCapture(frontFilePath, backFilePath)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }
        })
    }

    // Temporarily not used.
    private fun playSoundEffect() {
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            mediaPlayer.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Release MediaPlayer resources
        mediaPlayer.release()
    }

    private fun ensureDirectoriesExist() {
        val directories = arrayOf(
            Paths.get(imageDir),
            Paths.get(tmpDir)
        )

        for (dir in directories) {
            try {
                Files.createDirectory(dir)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun deleteTempFiles() {
        val tmpDir = File(tmpDir)
        if (!tmpDir.exists())
            return

        val files = tmpDir.listFiles()
        if(files?.isNotEmpty() == true) {
            for (file in files) {
                file.delete()
            }
        }
    }

    private fun allPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCameraPermissionCode) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to use the camera", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}