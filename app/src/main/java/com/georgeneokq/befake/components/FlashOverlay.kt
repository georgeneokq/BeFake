package com.georgeneokq.befake.components

import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation

class FlashOverlay(private val overlay: View) {
    private val fade: AlphaAnimation = AlphaAnimation(1f, 0f)

    fun flash(animationEnd: (() -> Unit)? = null) {
        fade.duration = 600
        fade.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                overlay.visibility = View.GONE
                animationEnd?.invoke()
            }
            override fun onAnimationRepeat(animation: Animation) {}
        })
        overlay.visibility = View.VISIBLE
        overlay.startAnimation(fade)
    }
}