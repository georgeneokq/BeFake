package com.georgeneokq.befake;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

public class CapturePreviewActivity extends AppCompatActivity {

    private ImageView previewImageView;
    private String frontFilePath, backFilePath;

    private final static String IMAGE_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/BeFake";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_preview);

        previewImageView = findViewById(R.id.previewImageView);

        Intent intent = getIntent();
        frontFilePath = intent.getStringExtra("frontFilePath");
        backFilePath = intent.getStringExtra("backFilePath");

        // Load front bitmap, account for rotation specified in EXIF
        Bitmap frontCameraBitmap = BitmapFactory.decodeFile(frontFilePath);
        frontCameraBitmap = rotateBitmap(frontCameraBitmap, frontFilePath);

        // The bitmap that the canvas will draw onto.
        Bitmap canvasBitmap = Bitmap.createBitmap(frontCameraBitmap.getWidth(), frontCameraBitmap.getHeight(), Bitmap.Config.RGB_565);

        // Load back bitmap, account for rotation specified in EXIF
        Bitmap backCameraBitmap = BitmapFactory.decodeFile(backFilePath);
        backCameraBitmap = rotateBitmap(backCameraBitmap, backFilePath);

        // Create canvas, draw front as main image, back as sub image
        Canvas canvas = new Canvas(canvasBitmap);
        canvas.drawBitmap(frontCameraBitmap, 0, 0, null);

        // Scale down back camera bitmap. Set height to 1/4 of main image.
        // Scale width according to original aspect ratio.
        int backHeight = frontCameraBitmap.getHeight() / 4;
        float ratio = (float) backHeight / backCameraBitmap.getHeight();
        int backWidth = Math.round(backCameraBitmap.getWidth() * ratio);
        backCameraBitmap = Bitmap.createScaledBitmap(backCameraBitmap, backWidth, backHeight, true);
        canvas.drawBitmap(backCameraBitmap, 15, 15, null);

        previewImageView.setImageBitmap(canvasBitmap);

        // TODO: Create buttons to save image / go back to camera
        String filePath = Paths.get(IMAGE_DIR, String.format("BeFake_%d.jpg", System.currentTimeMillis())).toString();
        saveBitmap(canvasBitmap, filePath);
    }

    private void saveBitmap(Bitmap bitmap, String filePath){
        File file = new File(filePath);

        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(file));
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