package com.example.monopolyzapped.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.NavKeys
import com.example.monopolyzapped.R
import com.example.monopolyzapped.model.Player
import kotlin.math.max

class FinalInstructionActivity : AppCompatActivity() {

    private var players = arrayListOf<Player>() // <-- on maintient la liste ici

    // Map family -> drawable
    private val FAMILY_DRAWABLE = intArrayOf(
        R.drawable.hl_card0, R.drawable.hl_card1, R.drawable.hl_card2,
        R.drawable.hl_card3, R.drawable.hl_card4, R.drawable.hl_card5,
        R.drawable.hl_card6, R.drawable.hl_card7, R.drawable.hl_card8
    )

    // dp helper bound to this Activity's resources
    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun populateFamilyStripWithPx(
        deck: List<Int>,
        boxPx: Int,
        overlapSamePx: Int,
        overlapBetweenPx: Int
    ) {
        val column = findViewById<LinearLayout>(R.id.familyStrip)
        column.removeAllViews()
        column.clipChildren = false
        column.clipToPadding = false

        val grouped = deck.groupBy { it }.toSortedMap()

        var isFirstCard = true
        var lastFamily: Int? = null

        grouped.forEach { (family, cards) ->
            cards.forEach { _ ->
                val iv = ImageView(this).apply {
                    setImageResource(FAMILY_DRAWABLE[family])
                    rotation = -90f
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    adjustViewBounds = true
                }

                val lp = LinearLayout.LayoutParams(boxPx, boxPx)
                if (!isFirstCard) {
                    val sameFamily = (lastFamily == family)
                    val ov = if (sameFamily) overlapSamePx else overlapBetweenPx
                    lp.setMargins(0, -ov, 0, 0)
                }
                iv.layoutParams = lp

                column.addView(iv)
                isFirstCard = false
                lastFamily = family
            }
        }
    }

    private fun populateFamilyStripFit(
        deck: List<Int>,
        baseCardBoxDp: Int = 54,
        baseOverlapSameDp: Int = 44,
        baseOverlapBetweenDp: Int = 33
    ) {
        val column = findViewById<LinearLayout>(R.id.familyStrip)

        column.post {
            val targetH = column.height
            if (targetH <= 0) return@post

            val baseBoxPx = baseCardBoxDp.dp()
            val baseSamePx = baseOverlapSameDp.dp()
            val baseBetweenPx = baseOverlapBetweenDp.dp()

            val grouped = deck.groupBy { it }.toSortedMap()
            val seqFamilies = mutableListOf<Int>()
            grouped.forEach { (fam, cards) -> repeat(cards.size) { seqFamilies.add(fam) } }

            val n = seqFamilies.size
            if (n == 0) {
                column.removeAllViews()
                return@post
            }

            var sumOverlapsBase = 0
            for (i in 1 until n) {
                sumOverlapsBase += if (seqFamilies[i] == seqFamilies[i - 1]) baseSamePx else baseBetweenPx
            }
            val baseTotalPx = n * baseBoxPx - sumOverlapsBase
            if (baseTotalPx <= 0) {
                populateFamilyStripWithPx(deck, baseBoxPx, baseSamePx, baseBetweenPx)
                return@post
            }

            val s = targetH.toFloat() / baseTotalPx.toFloat()

            val boxPx = max(16, (baseBoxPx * s).toInt())
            val samePx = max(1, (baseSamePx * s).toInt())
            val betweenPx = max(1, (baseBetweenPx * s).toInt())

            populateFamilyStripWithPx(deck, boxPx, samePx, betweenPx)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_final_instruction)

        // --- Récupérer players depuis l’Intent (API 33+ compatible)
        players = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(NavKeys.PLAYERS, Player::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
        }
        if (players.isEmpty()) {
            Toast.makeText(this, "Aucun joueur reçu dans FinalInstructionActivity.", Toast.LENGTH_SHORT).show()
        }

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnNext = findViewById<ImageView>(R.id.btnNext)
        val hand    = findViewById<ImageView>(R.id.handDevice)

// Back : son "retour"
        btnBack.bindClickBackWithPressAndSound {
            onBackPressedDispatcher.onBackPressed()
        }

// Next : son “clic” standard
        btnNext.bindClickWithPressAndSound {
            startActivity(Intent(this, PlayerChooseSideActivity::class.java).apply {
                if (Build.VERSION.SDK_INT >= 33) {
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                } else {
                    @Suppress("DEPRECATION")
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                }
                putExtra(PlayerChooseSideActivity.EXTRA_CURRENT_INDEX, 1)
            })
        }



        // Slide-in animation from left
        hand.post {
            hand.translationX = -hand.width * 2f
            hand.alpha = 0f
            hand.animate()
                .translationX(1350f)
                .alpha(1f)
                .setDuration(2000L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        // Exemple: construit la pile pour remplir la colonne blanche
        val sampleDeck = listOf(
            0,0,
            1,1,1,
            2,2,2,
            3,3,3,
            4,4,4,
            5,5,5,
            6,6,6,
            7,7,
            8,8,8,8
        )

        populateFamilyStripFit(
            deck = sampleDeck,
            baseCardBoxDp = 54,
            baseOverlapSameDp = 44,
            baseOverlapBetweenDp = 33
        )
    }
}
