package com.georgeneokq.befake.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.WindowInsets;
import android.view.WindowMetrics;

import androidx.annotation.NonNull;

import com.georgeneokq.befake.Globals;

public class Util {
    public static void vibrateTap(Context ctx) {
        Vibrator vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK));
    }

    public static void vibrateTapLight(Context ctx) {
        Vibrator vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
    }

//    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int cornerRadius) {
//        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(output);
//
//        final Paint paint = new Paint();
//        final RectF rect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
//        final float[] radii = {cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius};
//
//        final Path path = new Path();
//        path.addRoundRect(rect, radii, Path.Direction.CW);
//
//        canvas.clipPath(path);
//
//        canvas.drawBitmap(bitmap, 0, 0, paint);
//
//        return output;
//    }
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int cornerRadius) {
        return getRoundedCornerBitmap(bitmap, cornerRadius, 0, 0);
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int cornerRadius, int borderWidth, int borderColor) {
        return getRoundedCornerBitmap(bitmap, cornerRadius, borderWidth, borderColor);
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int cornerRadius, int borderWidth, int borderColor, int borderAlpha) {
        int width = bitmap.getWidth() + borderWidth * 2;
        int height = bitmap.getHeight() + borderWidth * 2;

        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        drawBorder(canvas, width, height, cornerRadius, borderWidth, borderColor, borderAlpha);
        drawBitmap(canvas, bitmap, cornerRadius, borderWidth);

        return output;
    }

    private static void drawBorder(Canvas canvas, int width, int height, int cornerRadius, int borderWidth, int borderColor) {
        drawBorder(canvas, width, height, cornerRadius, borderWidth, borderColor, 255);
    }

    private static void drawBorder(Canvas canvas, int width, int height, int cornerRadius, int borderWidth, int borderColor, int borderAlpha) {
        if (borderWidth > 0) {
            final Paint borderPaint = new Paint();
            borderPaint.setAntiAlias(true);
            borderPaint.setColor(borderColor);
            borderPaint.setAlpha(borderAlpha);

            final RectF borderRect = new RectF(0, 0, width, height);
            final float[] borderRadii = {cornerRadius + borderWidth, cornerRadius + borderWidth, cornerRadius + borderWidth, cornerRadius + borderWidth, cornerRadius + borderWidth, cornerRadius + borderWidth, cornerRadius + borderWidth, cornerRadius + borderWidth};
            final Path borderPath = new Path();
            borderPath.addRoundRect(borderRect, borderRadii, Path.Direction.CW);

            canvas.drawPath(borderPath, borderPaint);
        }
    }

    private static void drawBitmap(Canvas canvas, Bitmap bitmap, int cornerRadius, int borderWidth) {
        final Paint bitmapPaint = new Paint();
        bitmapPaint.setAntiAlias(true);
        bitmapPaint.setFilterBitmap(true);
        bitmapPaint.setDither(true);

        final RectF bitmapRect = new RectF(borderWidth, borderWidth, canvas.getWidth() - borderWidth, canvas.getHeight() - borderWidth);
        final float[] bitmapRadii = {cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius};
        final Path bitmapPath = new Path();
        bitmapPath.addRoundRect(bitmapRect, bitmapRadii, Path.Direction.CW);

        canvas.clipPath(bitmapPath);
        canvas.drawBitmap(bitmap, borderWidth, borderWidth, bitmapPaint);
    }

    public static int getScreenHeight(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = activity.getWindowManager().getCurrentWindowMetrics();
            Insets insets = windowMetrics.getWindowInsets()
                    .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
            return windowMetrics.getBounds().height() - insets.top - insets.bottom;
        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            return displayMetrics.heightPixels;
        }
    }

    public static void resetSettings(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(Globals.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("watermarkText", "BeFake.");
        editor.putString("watermarkColor", "white");
        editor.putInt("watermarkAlpha", 60);
        editor.putString("borderColor", "black");
        editor.putInt("borderAlpha", 100);
        editor.apply();
    }

    public static boolean isSettingsInitialized(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(Globals.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.contains("watermarkText");
    }
}
