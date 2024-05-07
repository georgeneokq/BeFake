package com.georgeneokq.befake;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.georgeneokq.befake.util.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

public class CapturePreviewActivity extends AppCompatActivity {

    private ImageView previewImageView;
    private String frontFilePath, backFilePath;

    private ImageButton btnDownload, btnReverse, btnWatermark;

    private final String IMAGE_DIR = Paths.get(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath(),
            "BeFake").toString();

    private boolean reverse;
    private boolean isWatermarked;

    private String watermarkText, watermarkColor, borderColor;
    private int watermarkAlpha, watermarkSize, borderAlpha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_preview);

        SharedPreferences prefs = getSharedPreferences(Globals.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        watermarkText = prefs.getString("watermarkText", "");
        watermarkColor = prefs.getString("watermarkColor", "");
        watermarkAlpha = prefs.getInt("watermarkAlpha", 100);
        watermarkSize = prefs.getInt("watermarkSize", 58);
        borderColor = prefs.getString("borderColor", "");
        borderAlpha = prefs.getInt("borderAlpha", 100);

        previewImageView = findViewById(R.id.previewImageView);
        btnDownload = findViewById(R.id.btnDownload);
        btnReverse = findViewById(R.id.btnReverse);
        btnWatermark = findViewById(R.id.btnWatermark);

        btnReverse.setOnClickListener(v -> {
            Util.vibrateTapLight(this);
            toggleMainPreview();
        });
        btnWatermark.setOnClickListener(v -> {
            Util.vibrateTapLight(this);
            toggleWatermark();
        });

        toggleWatermark();
    }

    private void toggleMainPreview() {
        reverse = !reverse;
        setPreview();
    }

    private void toggleWatermark() {
        isWatermarked = !isWatermarked;
        if(isWatermarked) {
            btnWatermark.setImageDrawable(getDrawable(R.drawable.text_active));
        } else {
            btnWatermark.setImageDrawable(getDrawable(R.drawable.text));
        }
        setPreview();
    }

    private void setPreview() {
        Intent intent = getIntent();
        frontFilePath = intent.getStringExtra("frontFilePath");
        backFilePath = intent.getStringExtra("backFilePath");

        String mainBitmapFilePath, subBitmapFilePath;

        if(!reverse) {
            mainBitmapFilePath = frontFilePath;
            subBitmapFilePath = backFilePath;
        } else {
            mainBitmapFilePath = backFilePath;
            subBitmapFilePath = frontFilePath;
        }

        // Load front bitmap, account for rotation specified in EXIF
        Bitmap mainCameraBitmap = BitmapFactory.decodeFile(mainBitmapFilePath);
        mainCameraBitmap = rotateBitmap(mainCameraBitmap, mainBitmapFilePath);

        // The bitmap that the canvas will draw onto.
        Bitmap canvasBitmap = Bitmap.createBitmap(mainCameraBitmap.getWidth(), mainCameraBitmap.getHeight(), Bitmap.Config.ARGB_8888);

        // Load back bitmap, account for rotation specified in EXIF
        Bitmap subCameraBitmap = BitmapFactory.decodeFile(subBitmapFilePath);
        subCameraBitmap = rotateBitmap(subCameraBitmap, subBitmapFilePath);

        // Create canvas, draw front as main image, back as sub image
        Canvas canvas = new Canvas(canvasBitmap);
        canvas.drawBitmap(mainCameraBitmap, 0, 0, null);

        // Scale down back camera bitmap. Set height to 1/4 of main image.
        // Scale width according to original aspect ratio.
        float subBitmapHeight = mainCameraBitmap.getHeight() / 3.2f;
        float ratio = subBitmapHeight / subCameraBitmap.getHeight();
        float subBitmapWidth = subCameraBitmap.getWidth() * ratio;
        subCameraBitmap = Bitmap.createScaledBitmap(subCameraBitmap,
                Math.round(subBitmapWidth), Math.round(subBitmapHeight), true);

        // Round the corners of the sub camera bitmap
        int color;
        try {
            color = Color.parseColor(borderColor);
        } catch(IllegalArgumentException e) {
            color = 0x000000;
        }
        subCameraBitmap = Util.getRoundedCornerBitmap(subCameraBitmap, 100, 8, color, (int) (borderAlpha / 100.0f * 255));

        canvas.drawBitmap(subCameraBitmap, 45, 45, null);

        if(isWatermarked) {
            try {
                color = Color.parseColor(watermarkColor);
            } catch(IllegalArgumentException e) {
                color = 0xFFFFFF;
            }

            Paint textPaint = new Paint();
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(watermarkSize);
            textPaint.setColor(color);
            textPaint.setAlpha((int) (watermarkAlpha / 100.0f * 255));
            textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            textPaint.setLetterSpacing(0.02f);

            Rect bounds = new Rect();
            textPaint.getTextBounds(watermarkText, 0, watermarkText.length(), bounds);
            int x = canvas.getWidth() / 2;
            int y = canvas.getHeight() - (bounds.height() / 2);
            canvas.drawText(watermarkText, x, y, textPaint);
        }

        previewImageView.setImageBitmap(canvasBitmap);

        mainCameraBitmap.recycle();
        subCameraBitmap.recycle();

        btnDownload.setOnClickListener(v -> {
            Util.vibrateTap(this);
            String filePath = Paths.get(IMAGE_DIR, String.format("BeFake_%d.jpg", System.currentTimeMillis())).toString();
            saveBitmap(canvasBitmap, filePath);
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveBitmap(Bitmap bitmap, String filePath) {
        File file = new File(filePath);

        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(file));

            // Trigger MediaScanner to inform system of new media file
            String[] toScan = new String[] { file.getAbsolutePath() };
            MediaScannerConnection.scanFile(this, toScan, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    private Bitmap rotateBitmap(Bitmap bitmap, String filePath) {
        try {
            ExifInterface exifInterface = new ExifInterface(filePath);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.setRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.setRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.setRotate(270);
                    break;
                default:
                    return bitmap;
            }
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}