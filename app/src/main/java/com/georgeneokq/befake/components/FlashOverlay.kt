package com.georgeneokq.befake.components

import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation

class FlashOverlay(private val overlay: View) {
    private val fade: AlphaAnimation = AlphaAnimation(1f, 0f)

    init {
        fade.duration = 600
        fade.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                overlay.visibility = View.GONE
            }
            override fun onAnimationRepeat(animation: Animation) {}
        })
    }

    fun flash() {
        overlay.visibility = View.VISIBLE
        overlay.startAnimation(fade)
    }
}