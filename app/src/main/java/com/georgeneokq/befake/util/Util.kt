package com.georgeneokq.befake.util

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.DisplayMetrics
import android.view.WindowInsets
import com.georgeneokq.befake.Globals

object Util {
    fun vibrateTap(ctx: Context) {
        val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
    }

    fun vibrateTapLight(ctx: Context) {
        val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
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
    fun getRoundedCornerBitmap(bitmap: Bitmap?, cornerRadius: Int): Bitmap {
        return getRoundedCornerBitmap(bitmap, cornerRadius, 0, 0)
    }

    fun getRoundedCornerBitmap(
        bitmap: Bitmap?,
        cornerRadius: Int,
        borderWidth: Int,
        borderColor: Int
    ): Bitmap {
        return getRoundedCornerBitmap(bitmap, cornerRadius, borderWidth, borderColor)
    }

    fun getRoundedCornerBitmap(
        bitmap: Bitmap,
        cornerRadius: Int,
        borderWidth: Int,
        borderColor: Int,
        borderAlpha: Int
    ): Bitmap {
        val width = bitmap.width + borderWidth * 2
        val height = bitmap.height + borderWidth * 2
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        drawBorder(canvas, width, height, cornerRadius, borderWidth, borderColor, borderAlpha)
        drawBitmap(canvas, bitmap, cornerRadius, borderWidth)
        return output
    }

    private fun drawBorder(
        canvas: Canvas,
        width: Int,
        height: Int,
        cornerRadius: Int,
        borderWidth: Int,
        borderColor: Int,
        borderAlpha: Int = 255
    ) {
        if (borderWidth > 0) {
            val borderPaint = Paint()
            borderPaint.isAntiAlias = true
            borderPaint.color = borderColor
            borderPaint.alpha = borderAlpha
            val borderRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            val borderRadii = floatArrayOf(
                (cornerRadius + borderWidth).toFloat(),
                (cornerRadius + borderWidth).toFloat(),
                (cornerRadius + borderWidth).toFloat(),
                (cornerRadius + borderWidth).toFloat(),
                (cornerRadius + borderWidth).toFloat(),
                (cornerRadius + borderWidth).toFloat(),
                (cornerRadius + borderWidth).toFloat(),
                (cornerRadius + borderWidth).toFloat()
            )
            val borderPath = Path()
            borderPath.addRoundRect(borderRect, borderRadii, Path.Direction.CW)
            canvas.drawPath(borderPath, borderPaint)
        }
    }

    private fun drawBitmap(canvas: Canvas, bitmap: Bitmap, cornerRadius: Int, borderWidth: Int) {
        val bitmapPaint = Paint()
        bitmapPaint.isAntiAlias = true
        bitmapPaint.isFilterBitmap = true
        bitmapPaint.isDither = true
        val bitmapRect = RectF(
            borderWidth.toFloat(),
            borderWidth.toFloat(),
            (canvas.width - borderWidth).toFloat(),
            (canvas.height - borderWidth).toFloat()
        )
        val bitmapRadii = floatArrayOf(
            cornerRadius.toFloat(),
            cornerRadius.toFloat(),
            cornerRadius.toFloat(),
            cornerRadius.toFloat(),
            cornerRadius.toFloat(),
            cornerRadius.toFloat(),
            cornerRadius.toFloat(),
            cornerRadius.toFloat()
        )
        val bitmapPath = Path()
        bitmapPath.addRoundRect(bitmapRect, bitmapRadii, Path.Direction.CW)
        canvas.clipPath(bitmapPath)
        canvas.drawBitmap(bitmap, borderWidth.toFloat(), borderWidth.toFloat(), bitmapPaint)
    }

    fun getScreenHeight(activity: Activity): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = activity.windowManager.currentWindowMetrics
            val insets = windowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            windowMetrics.bounds.height() - insets.top - insets.bottom
        } else {
            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.heightPixels
        }
    }

    fun resetSettings(ctx: Context) {
        val prefs = ctx.getSharedPreferences(Globals.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("watermarkText", "BeFake.")
        editor.putString("watermarkColor", "white")
        editor.putInt("watermarkAlpha", 65)
        editor.putInt("watermarkSize", 58)
        editor.putString("borderColor", "black")
        editor.putInt("borderAlpha", 100)
        editor.apply()
    }

    fun isSettingsInitialized(ctx: Context): Boolean {
        val prefs = ctx.getSharedPreferences(Globals.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        return prefs.contains("watermarkText")
    }
}