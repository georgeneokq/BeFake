package com.georgeneokq.befake

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.georgeneokq.befake.util.Util
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Paths
import kotlin.math.roundToInt

class CapturePreviewActivity : AppCompatActivity() {
    private lateinit var previewImageView: ImageView
    private lateinit var frontFilePath: String
    private lateinit var backFilePath: String
    private lateinit var btnDownload: ImageButton
    private lateinit var btnReverse: ImageButton
    private lateinit var btnWatermark: ImageButton
    private lateinit var watermarkText: String
    private lateinit var watermarkColor: String
    private lateinit var borderColor: String

    private var reverse = false
    private var isWatermarked = false
    private var watermarkAlpha = 0
    private var watermarkSize = 0
    private var borderAlpha = 0

    private val imageDir = Paths.get(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath,
        "BeFake"
    ).toString()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_preview)
        val prefs = getSharedPreferences(Globals.SHARED_PREFERENCES_NAME, MODE_PRIVATE)
        watermarkText = prefs.getString("watermarkText", "")!!
        watermarkColor = prefs.getString("watermarkColor", "")!!
        watermarkAlpha = prefs.getInt("watermarkAlpha", 100)
        watermarkSize = prefs.getInt("watermarkSize", 58)
        borderColor = prefs.getString("borderColor", "")!!
        borderAlpha = prefs.getInt("borderAlpha", 100)
        previewImageView = findViewById(R.id.previewImageView)
        btnDownload = findViewById(R.id.btnDownload)
        btnReverse = findViewById(R.id.btnReverse)
        btnWatermark = findViewById(R.id.btnWatermark)
        btnReverse.setOnClickListener {
            Util.vibrateTapLight(this)
            toggleMainPreview()
        }
        btnWatermark.setOnClickListener {
            Util.vibrateTapLight(this)
            toggleWatermark()
        }
        toggleWatermark()
    }

    private fun toggleMainPreview() {
        reverse = !reverse
        setPreview()
    }

    private fun toggleWatermark() {
        isWatermarked = !isWatermarked
        if (isWatermarked) {
            btnWatermark.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.text_active))
        } else {
            btnWatermark.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.text))
        }
        setPreview()
    }

    private fun setPreview() {
        val intent = intent
        frontFilePath = intent.getStringExtra("frontFilePath")!!
        backFilePath = intent.getStringExtra("backFilePath")!!
        val mainBitmapFilePath: String?
        val subBitmapFilePath: String?
        if (!reverse) {
            mainBitmapFilePath = frontFilePath
            subBitmapFilePath = backFilePath
        } else {
            mainBitmapFilePath = backFilePath
            subBitmapFilePath = frontFilePath
        }

        // Load front bitmap, account for rotation specified in EXIF
        var mainCameraBitmap = BitmapFactory.decodeFile(mainBitmapFilePath)
        mainCameraBitmap = rotateBitmap(mainCameraBitmap, mainBitmapFilePath)

        // The bitmap that the canvas will draw onto.
        val canvasBitmap = Bitmap.createBitmap(
            mainCameraBitmap!!.width, mainCameraBitmap.height, Bitmap.Config.ARGB_8888
        )

        // Load back bitmap, account for rotation specified in EXIF
        var subCameraBitmap = BitmapFactory.decodeFile(subBitmapFilePath)
        subCameraBitmap = rotateBitmap(subCameraBitmap, subBitmapFilePath)

        // Create canvas, draw front as main image, back as sub image
        val canvas = Canvas(canvasBitmap)
        canvas.drawBitmap(mainCameraBitmap, 0f, 0f, null)

        // Scale down back camera bitmap. Set height to 1/4 of main image.
        // Scale width according to original aspect ratio.
        val subBitmapHeight = mainCameraBitmap.height / 3.2f
        val ratio = subBitmapHeight / subCameraBitmap!!.height
        val subBitmapWidth = subCameraBitmap.width * ratio
        subCameraBitmap = Bitmap.createScaledBitmap(
            subCameraBitmap,
            subBitmapWidth.roundToInt(), subBitmapHeight.roundToInt(), true
        )

        // Round the corners of the sub camera bitmap
        var color: Int
        color = try {
            Color.parseColor(borderColor)
        } catch (e: IllegalArgumentException) {
            0x000000
        }
        subCameraBitmap = Util.getRoundedCornerBitmap(
            subCameraBitmap,
            100,
            8,
            color,
            (borderAlpha / 100.0f * 255).toInt()
        )
        canvas.drawBitmap(subCameraBitmap, 45f, 45f, null)
        if (isWatermarked) {
            color = try {
                Color.parseColor(watermarkColor)
            } catch (e: IllegalArgumentException) {
                0xFFFFFF
            }
            val textPaint = Paint()
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = watermarkSize.toFloat()
            textPaint.color = color
            textPaint.alpha = (watermarkAlpha / 100.0f * 255).toInt()
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textPaint.letterSpacing = 0.02f
            val bounds = Rect()
            textPaint.getTextBounds(watermarkText, 0, watermarkText.length, bounds)
            val x = canvas.width / 2
            val y = canvas.height - bounds.height() / 2
            canvas.drawText(watermarkText, x.toFloat(), y.toFloat(), textPaint)
        }
        previewImageView.setImageBitmap(canvasBitmap)
        mainCameraBitmap.recycle()
        subCameraBitmap.recycle()
        btnDownload.setOnClickListener { v: View? ->
            Util.vibrateTap(this)
            val filePath =
                Paths.get(imageDir, String.format("BeFake_%d.jpg", System.currentTimeMillis()))
                    .toString()
            saveBitmap(canvasBitmap, filePath)
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveBitmap(bitmap: Bitmap, filePath: String) {
        val file = File(filePath)
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, FileOutputStream(file))

            // Trigger MediaScanner to inform system of new media file
            val toScan = arrayOf(file.absolutePath)
            MediaScannerConnection.scanFile(this, toScan, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun rotateBitmap(bitmap: Bitmap?, filePath: String?): Bitmap? {
        return try {
            val exifInterface = ExifInterface(filePath!!)
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap!!, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}