package com.example.monopolyzapped.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.NavKeys
import com.example.monopolyzapped.R
import com.example.monopolyzapped.model.Player

class PlayerMenuCommunityAnimationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RANDOM_CC_ID = "extra_random_cc_id" // 1..20
    }

    private var launchedNext = false

    // Contexte propagé
    private var players = arrayListOf<Player>()
    private var playerIndex = 0              // 0-based
    private var currentTurnIndex = 0         // 0-based

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_menu_community_animation)

        // Récup extras entrants
        intent.setExtrasClassLoader(Player::class.java.classLoader)
        players = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(NavKeys.PLAYERS, Player::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
        }
        playerIndex = intent.getIntExtra(PlayerMenuActivity.EXTRA_PLAYER_INDEX, 0)
        currentTurnIndex = intent.getIntExtra(PlayerMenuActivity.EXTRA_TURN_INDEX, playerIndex)

        val ivBg = findViewById<ImageView>(R.id.ivCcBg)
        val ivCard = findViewById<ImageView>(R.id.ivCcCard)

        // (Laisse les src définis dans le XML si tu as des assets dédiés ; pas obligatoire ici)

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
                        val randomId = kotlin.random.Random.nextInt(1, 21) // 1..20

                        val next = Intent(this, PlayerMenuCommunityCardInstructionActivity::class.java).apply {
                            putExtra(EXTRA_RANDOM_CC_ID, randomId)
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
