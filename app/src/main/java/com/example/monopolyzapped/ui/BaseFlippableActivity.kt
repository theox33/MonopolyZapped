package com.example.monopolyzapped.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

open class BaseFlippableActivity : AppCompatActivity() {

    /**
     * Opt-out: certaines activités ne doivent jamais être retournées.
     * Par défaut: on applique le flip global défini dans [UiFlipStore].
     */
    protected open fun shouldApplyUiFlip(): Boolean = true

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        applyFlipIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        applyFlipIfNeeded()
    }

    private fun applyFlipIfNeeded() {
        if (!shouldApplyUiFlip()) return

        val root = window.decorView.findViewById<View>(android.R.id.content) ?: return
        root.rotation = if (UiFlipStore.isFlipped(this)) 180f else 0f
    }
}
