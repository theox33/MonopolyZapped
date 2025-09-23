package com.example.monopolyzapped.ui

import android.media.MediaPlayer
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.example.monopolyzapped.R

/**
 * Animation “shrink on press” générique pour n'importe quel View.
 * - scaleDown : facteur de réduction au press
 * - downDuration / upDuration : vitesses d’anim
 */
fun View.applyPressAnimation(
    scaleDown: Float = 0.96f,
    downDuration: Long = 70L,
    upDuration: Long = 90L
) {
    isClickable = true // important si ImageView “bouton”
    setOnTouchListener { v, ev ->
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                v.animate()
                    .scaleX(scaleDown).scaleY(scaleDown)
                    .setDuration(downDuration)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(upDuration)
                    .setInterpolator(OvershootInterpolator(2f))
                    .start()
            }
        }
        // On ne consomme pas l’évènement pour laisser le clic fonctionner normalement
        false
    }
}

/**
 * Click listener avec anti double-clic.
 */
fun View.setDebouncedClickListener(minIntervalMs: Long = 350L, onClick: (View) -> Unit) {
    var lastClickTime = 0L
    setOnClickListener { v ->
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime >= minIntervalMs) {
            lastClickTime = now
            onClick(v)
        }
    }
}

/**
 * Bind complet : animation de pression + son de clic + action.
 * Utilise dooweep03.wav dans res/raw/.
 */
fun View.bindClickWithPressAndSound(
    soundResId: Int = R.raw.dooweep03,
    onClick: (View) -> Unit
) {
    applyPressAnimation()
    setDebouncedClickListener {
        // son court, relâché automatiquement en fin de lecture
        MediaPlayer.create(context, soundResId).apply {
            setOnCompletionListener { mp -> mp.release() }
            start()
        }
        onClick(it)
    }
}


fun View.bindClickBackWithPressAndSound(
    soundResId: Int = R.raw.back_button_press,
    onClick: (View) -> Unit
) {
    applyPressAnimation()
    setDebouncedClickListener {
        // son court, relâché automatiquement en fin de lecture
        MediaPlayer.create(context, soundResId).apply {
            setOnCompletionListener { mp -> mp.release() }
            start()
        }
        onClick(it)
    }
}