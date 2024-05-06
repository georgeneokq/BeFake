package com.georgeneokq.befake.components;

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

public class FlashOverlay {

    private View overlay;

    private AlphaAnimation fade;

    public FlashOverlay(View view) {
        this.overlay = view;

        fade = new AlphaAnimation(1, 0);
        fade.setDuration(600);
        fade.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                overlay.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    public void flash() {
        overlay.setVisibility(View.VISIBLE);
        overlay.startAnimation(fade);
    }
}
