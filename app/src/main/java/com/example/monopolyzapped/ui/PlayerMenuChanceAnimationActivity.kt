package com.example.monopolyzapped.ui

import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.NavKeys
import com.example.monopolyzapped.R
import kotlin.random.Random

// imports à ajouter
import android.os.Build
import com.example.monopolyzapped.model.Player

class PlayerMenuChanceAnimationActivity : AppCompatActivity() {

    companion object { const val EXTRA_RANDOM_CHANCE_ID = "extra_random_chance_id" }

    private var launchedNext = false

    // 🔧 AJOUTER ces 3 propriétés
    private var players = arrayListOf<Player>()
    private var playerIndex = 0
    private var currentTurnIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_menu_chance_animation)

        // 🔧 LIRE les extras entrants
        intent.setExtrasClassLoader(Player::class.java.classLoader)
        players = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(NavKeys.PLAYERS, Player::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
        }
        playerIndex = intent.getIntExtra(PlayerMenuActivity.EXTRA_PLAYER_INDEX, 0)          // 0-based
        currentTurnIndex = intent.getIntExtra(PlayerMenuActivity.EXTRA_TURN_INDEX, playerIndex) // 0-based

        val ivBg = findViewById<ImageView>(R.id.ivChanceBg)
        val ivCard = findViewById<ImageView>(R.id.ivChanceCard)
        ivBg.setImageResource(R.drawable.instruction_background_chance)
        ivCard.setImageResource(R.drawable.instruction_card_chance)

        ivCard.post {
            val currentX = ivCard.x
            val cardWidth = ivCard.width.toFloat()
            val offscreenLeft = -(currentX + cardWidth + 16.dp())

            ivCard.animate()
                .translationX(offscreenLeft)
                .setInterpolator(AccelerateInterpolator())
                .setDuration(900L)
                .withEndAction {
                    if (!launchedNext) {
                        launchedNext = true
                        val randomId = Random.nextInt(1, 21)

                        val next = Intent(this, PlayerMenuChanceCardInstructionActivity::class.java).apply {
                            putExtra(EXTRA_RANDOM_CHANCE_ID, randomId)
                            // 🔧 PROPAGER le contexte
                            putExtra(PlayerMenuActivity.EXTRA_PLAYER_INDEX, playerIndex)
                            putExtra(PlayerMenuActivity.EXTRA_TURN_INDEX, currentTurnIndex)
                            putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                        }
                        startActivity(next)
                        finish()
                    }
                }
                .start()
        }
    }

    private fun Int.dp(): Float = this * resources.displayMetrics.density
}
