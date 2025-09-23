package com.example.monopolyzapped.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.NavKeys
import com.example.monopolyzapped.R
import com.example.monopolyzapped.model.Player

class GamePrepDealActivity : AppCompatActivity() {

    private var players = arrayListOf<Player>() // <-- on déclare players ici

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_prep_deal)

        // Récupération des joueurs depuis l'intent
        players = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(NavKeys.PLAYERS, Player::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
        }

        if (players.isEmpty()) {
            Toast.makeText(this, "Aucun joueur reçu dans GamePrepDealActivity.", Toast.LENGTH_SHORT).show()
            // Optionnel: finish() si cet écran n’a pas de sens sans joueurs
            // finish(); return
        }

        val btnReady = findViewById<View>(R.id.btnReady)
        btnReady.bindClickWithPressAndSound {
            startActivity(Intent(this, FinalInstructionActivity::class.java).apply {
                putParcelableArrayListExtra(NavKeys.PLAYERS, players) // <-- on relaie bien
                putExtra(PlayerChooseSideActivity.EXTRA_CURRENT_INDEX, 1)
            })
        }
    }
}
