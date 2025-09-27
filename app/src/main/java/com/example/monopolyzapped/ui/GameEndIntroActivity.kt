package com.example.monopolyzapped.ui

import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.NavKeys
import com.example.monopolyzapped.R
import com.example.monopolyzapped.model.Player

class GameEndIntroActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLAYER_INDEX = "player_index"
        const val EXTRA_TURN_INDEX = "turn_index"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Layout ultra simple (voir XML ci-dessous)
        setContentView(R.layout.activity_game_end_intro)

        // Récup données (au cas où tu en aies besoin plus tard)
        val playerIndex = intent.getIntExtra(EXTRA_PLAYER_INDEX, 0)
        val currentTurnIndex = intent.getIntExtra(EXTRA_TURN_INDEX, playerIndex)
        val players = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(NavKeys.PLAYERS, Player::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
        }

        findViewById<TextView>(R.id.tvEndSummary).text =
            "Écran fin de partie (placeholder)\n\n" +
                    "Players: ${players.size}\n" +
                    "Current turn index: $currentTurnIndex\n" +
                    "Player index: $playerIndex"
    }
}
