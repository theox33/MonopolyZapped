package com.example.monopolyzapped.ui

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.NavKeys
import com.example.monopolyzapped.R
import com.example.monopolyzapped.model.Player

class PlayerSetupActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TOTAL_PLAYERS = "total_players"
        const val EXTRA_CURRENT_INDEX = "current_index" // 1, 2, 3...
    }

    private lateinit var inputName: EditText
    private var totalPlayers = 2
    private var currentIndex = 1
    private val players = arrayListOf<Player>()

    private fun playClickSound() {
        val mp = MediaPlayer.create(this, R.raw.dooweep03)
        mp.setOnCompletionListener { it.release() }
        mp.start()
    }

    private fun playBackSound() {
        val mp = MediaPlayer.create(this, R.raw.back_button_press)
        mp.setOnCompletionListener { it.release() }
        mp.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_setup)

        // Récupère l’état
        totalPlayers = intent.getIntExtra(EXTRA_TOTAL_PLAYERS, 2)
        currentIndex = intent.getIntExtra(EXTRA_CURRENT_INDEX, 1)
        intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS)?.let { players.addAll(it) }

        // UI
        inputName = findViewById(R.id.inputName)
        val title = findViewById<android.widget.TextView>(R.id.title)
        title.text = "Nom joueur $currentIndex :"

        // Désactiver les pions déjà pris
        val taken = players.map { it.token }.toSet()
        disableTakenToken(R.id.tokenCar,  "car"  in taken)
        disableTakenToken(R.id.tokenDog,  "dog"  in taken)
        disableTakenToken(R.id.tokenHat,  "hat"  in taken)
        disableTakenToken(R.id.tokenIron, "iron" in taken)
        disableTakenToken(R.id.tokenShip, "ship" in taken)
        disableTakenToken(R.id.tokenShoe, "shoe" in taken)

        // Cliques sur tokens
        setTokenClick(R.id.tokenCar,  "car")
        setTokenClick(R.id.tokenDog,  "dog")
        setTokenClick(R.id.tokenHat,  "hat")
        setTokenClick(R.id.tokenIron, "iron")
        setTokenClick(R.id.tokenShip, "ship")
        setTokenClick(R.id.tokenShoe, "shoe")

        // Bouton retour
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            playBackSound()
            finish()
        }
        // Animation + son si tu utilises les extensions
        findViewById<View>(R.id.btnBack).bindClickBackWithPressAndSound { finish() }
    }

    private fun disableTakenToken(viewId: Int, isTaken: Boolean) {
        val iv = findViewById<ImageView>(viewId)
        iv.isEnabled = !isTaken
        iv.alpha = if (isTaken) 0.35f else 1f
        if (isTaken) iv.setOnClickListener(null)
    }

    private fun setTokenClick(viewId: Int, tokenCode: String) {
        findViewById<ImageView>(viewId).bindClickWithPressAndSound {
            val name = inputName.text.toString().trim()
            if (name.isEmpty()) {
                AlertDialog.Builder(this)
                    .setMessage("Veuillez entrer un nom.")
                    .setPositiveButton("OK", null)
                    .show()
                return@bindClickWithPressAndSound
            }

            // Crée et ajoute le joueur courant (carte = null pour l’instant)
            players.add(Player(name, tokenCode)) // card=null

            // Enchaîne : intro puis scan de la carte pour CE joueur
            val i = Intent(this, PlayerCardIntroActivity::class.java).apply {
                putExtra(NavKeys.TOTAL_PLAYERS, totalPlayers)
                putExtra(NavKeys.CURRENT_INDEX, currentIndex)
                putParcelableArrayListExtra(NavKeys.PLAYERS, players)
            }
            startActivity(i)
            finish()
        }
    }
}