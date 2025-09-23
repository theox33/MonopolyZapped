package com.example.monopolyzapped.ui

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.TextView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.NavKeys
import com.example.monopolyzapped.R
import com.example.monopolyzapped.model.Player

class PlayerCardIntroActivity : AppCompatActivity() {

    private var totalPlayers: Int = 2
    private var currentIndex: Int = 1
    private lateinit var players: ArrayList<Player>

    private fun playClickSound() {
        val mp = MediaPlayer.create(this, R.raw.dooweep03)
        mp.setOnCompletionListener { it.release() } // libère après lecture
        mp.start()
    }

    private fun playBackSound() {
        val mp = MediaPlayer.create(this, R.raw.back_button_press)
        mp.setOnCompletionListener { it.release() } // libère après lecture
        mp.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_card_intro)

        // Récupère les extras
        totalPlayers = intent.getIntExtra(NavKeys.TOTAL_PLAYERS, 2)
        currentIndex = intent.getIntExtra(NavKeys.CURRENT_INDEX, 1)
        players = intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()

        // Bouton "Prêt" -> écran de scan
        val btnReady = findViewById<View>(R.id.btnReady)
        btnReady.bindClickWithPressAndSound {
            startActivity(Intent(this, PlayersSelectActivity::class.java))
            playClickSound()
            val i = Intent(this, PlayerCardScanActivity::class.java).apply {
                putExtra(NavKeys.TOTAL_PLAYERS, totalPlayers)
                putExtra(NavKeys.CURRENT_INDEX, currentIndex)
                putParcelableArrayListExtra(NavKeys.PLAYERS, players)
            }
            startActivity(i)
            finish()
        }

        // Bouton retour
        val btnBack = findViewById<View>(R.id.btnBack)
        btnBack.bindClickBackWithPressAndSound {
            finish()
        }
    }
}
