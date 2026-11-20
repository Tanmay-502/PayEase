package com.example.payease.utils

import android.annotation.SuppressLint
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

object AnimationUtils {

    @SuppressLint("ClickableViewAccessibility")
    fun applyTouchScale(view: View) {
        val springX = SpringAnimation(view, DynamicAnimation.SCALE_X).apply {
            spring = SpringForce().apply {
                stiffness = SpringForce.STIFFNESS_MEDIUM
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
            }
        }

        val springY = SpringAnimation(view, DynamicAnimation.SCALE_Y).apply {
            spring = SpringForce().apply {
                stiffness = SpringForce.STIFFNESS_MEDIUM
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
            }
        }

        view.setOnTouchListener { v, event ->
            if (!areAnimationsEnabled(v)) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    springX.animateToFinalPosition(0.96f)
                    springY.animateToFinalPosition(0.96f)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    springX.animateToFinalPosition(1.0f)
                    springY.animateToFinalPosition(1.0f)
                }
            }
            false
        }
    }

    fun animateViewScalePunch(view: View) {
        if (!areAnimationsEnabled(view)) return

        view.scaleX = 0.85f
        view.scaleY = 0.85f

        SpringAnimation(view, DynamicAnimation.SCALE_X, 1.0f).apply {
            spring = SpringForce().apply {
                stiffness = SpringForce.STIFFNESS_MEDIUM
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
            }
            start()
        }

        SpringAnimation(view, DynamicAnimation.SCALE_Y, 1.0f).apply {
            spring = SpringForce().apply {
                stiffness = SpringForce.STIFFNESS_MEDIUM
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
            }
            start()
        }
    }

    fun fadeInView(view: View, durationMs: Long = 250L) {
        if (!areAnimationsEnabled(view)) {
            view.alpha = 1f
            view.visibility = View.VISIBLE
            return
        }

        view.alpha = 0f
        view.visibility = View.VISIBLE
        view.animate()
            .alpha(1f)
            .setDuration(durationMs)
            .start()
    }

    private fun areAnimationsEnabled(view: View): Boolean {
        return try {
            val scale = Settings.Global.getFloat(
                view.context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale > 0f
        } catch (_: Exception) {
            true
        }
    }
}
