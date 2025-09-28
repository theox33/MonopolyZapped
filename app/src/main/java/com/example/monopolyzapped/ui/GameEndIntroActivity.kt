// GameEndIntroActivity.kt
package com.example.monopolyzapped.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.NavKeys
import com.example.monopolyzapped.R
import com.example.monopolyzapped.model.Player

class GameEndIntroActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLAYER_INDEX = "player_index"
        const val EXTRA_TURN_INDEX   = "turn_index"
    }

    private var players = arrayListOf<Player>()
    private var playerIndex = 0
    private var currentTurnIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_game_end_intro)

        intent.setExtrasClassLoader(Player::class.java.classLoader)
        playerIndex = intent.getIntExtra(EXTRA_PLAYER_INDEX, 0)
        currentTurnIndex = intent.getIntExtra(EXTRA_TURN_INDEX, playerIndex)
        players = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(NavKeys.PLAYERS, Player::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
        }

        // Utilise requireViewById pour crasher tôt avec un message clair si l'id manque
        val btnNext: ImageView = findViewById(R.id.btnNext)

        btnNext.setOnClickListener {
            // ➜ on enchaîne vers WhoOwnsWhatActivity (comme demandé)
            val intent = Intent(this, WhoOwnsWhatActivity::class.java).apply {
                putExtra(WhoOwnsWhatActivity.EXTRA_TURN_INDEX, currentTurnIndex)
                if (Build.VERSION.SDK_INT >= 33) {
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                } else {
                    @Suppress("DEPRECATION")
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                }
            }
            startActivity(intent)
        }
    }
}
