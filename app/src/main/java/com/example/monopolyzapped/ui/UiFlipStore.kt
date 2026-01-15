package com.example.monopolyzapped.ui

import android.content.Context

/**
 * Stockage simple du mode "écran retourné" (rotation 180°).
 *
 * Règle produit:
 * - Décidé UNIQUEMENT depuis [GameMainMenuActivity] au clic sur TL/TR.
 * - S'applique à toutes les autres activités (sauf [GameMainMenuActivity]).
 * - Revient à la normale uniquement quand on revient sur [GameMainMenuActivity] (reset).
 */
object UiFlipStore {
    private const val PREFS_NAME = "ui_flip_prefs"
    private const val KEY_IS_FLIPPED = "is_flipped"

    fun isFlipped(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_FLIPPED, false)

    fun setFlipped(context: Context, flipped: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_FLIPPED, flipped)
            .apply()
    }

    fun reset(context: Context) = setFlipped(context, false)
}

