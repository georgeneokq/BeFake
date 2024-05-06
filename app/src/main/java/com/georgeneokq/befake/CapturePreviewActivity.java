package com.georgeneokq.befake;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_preview);

        previewImageView = findViewById(R.id.previewImageView);
        btnDownload = findViewById(R.id.btnDownload);
        btnReverse = findViewById(R.id.btnReverse);
        btnWatermark = findViewById(R.id.btnWatermark);

        btnReverse.setOnClickListener(v -> toggleMainPreview());
        btnWatermark.setOnClickListener(v -> toggleWatermark());

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
        Bitmap canvasBitmap = Bitmap.createBitmap(mainCameraBitmap.getWidth(), mainCameraBitmap.getHeight(), Bitmap.Config.RGB_565);

        // Load back bitmap, account for rotation specified in EXIF
        Bitmap subCameraBitmap = BitmapFactory.decodeFile(subBitmapFilePath);
        subCameraBitmap = rotateBitmap(subCameraBitmap, subBitmapFilePath);

        // Create canvas, draw front as main image, back as sub image
        Canvas canvas = new Canvas(canvasBitmap);
        canvas.drawBitmap(mainCameraBitmap, 0, 0, null);

        // Scale down back camera bitmap. Set height to 1/4 of main image.
        // Scale width according to original aspect ratio.
        int backHeight = mainCameraBitmap.getHeight() / 3;
        float ratio = (float) backHeight / subCameraBitmap.getHeight();
        int backWidth = Math.round(subCameraBitmap.getWidth() * ratio);
        subCameraBitmap = Bitmap.createScaledBitmap(subCameraBitmap, backWidth, backHeight, true);
        subCameraBitmap = addBorder(subCameraBitmap, 2);
        canvas.drawBitmap(subCameraBitmap, 30, 30, null);

        if(isWatermarked) {
            Paint textPaint = new Paint();
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(58);
            textPaint.setColor(Color.WHITE);
            textPaint.setAlpha(200);
            textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            textPaint.setLetterSpacing(0.02f);
            int x = canvas.getWidth() / 2;
            int y = (int) ((canvas.getHeight() - 50) - ((textPaint.descent() + textPaint.ascent()) / 2));
            canvas.drawText("BeFake.", x, y, textPaint);
        }

        previewImageView.setImageBitmap(canvasBitmap);

        mainCameraBitmap.recycle();
        subCameraBitmap.recycle();

        btnDownload.setOnClickListener(v -> {
            Util.vibrateTapLight(this);
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

    private Bitmap addBorder(Bitmap bmp, int borderSize) {
        Bitmap bmpWithBorder = Bitmap.createBitmap(bmp.getWidth() + borderSize * 2, bmp.getHeight() + borderSize * 2, bmp.getConfig());
        Canvas canvas = new Canvas(bmpWithBorder);
        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(bmp, borderSize, borderSize, null);
        return bmpWithBorder;
    }
}