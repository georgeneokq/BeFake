package com.georgeneokq.befake.util;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class Util {
    public static void vibrateTap(Context ctx) {
        Vibrator vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK));
    }

    public static void vibrateTapLight(Context ctx) {
        Vibrator vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
    }
}
