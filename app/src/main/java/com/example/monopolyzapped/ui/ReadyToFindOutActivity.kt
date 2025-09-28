// app/src/main/java/com/example/monopolyzapped/ui/ReadyToFindOutActivity.kt
package com.example.monopolyzapped.ui

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.NavKeys
import com.example.monopolyzapped.R
import com.example.monopolyzapped.model.Player

class ReadyToFindOutActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TURN_INDEX = "turn_index"
    }

    private var players = arrayListOf<Player>()
    private var currentTurnIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ready_to_find_out) // ⚠️ ce layout doit exister

        intent.setExtrasClassLoader(Player::class.java.classLoader)
        currentTurnIndex = intent.getIntExtra(EXTRA_TURN_INDEX, 0)
        players = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(NavKeys.PLAYERS, Player::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
        }

        findViewById<TextView>(R.id.tvReady).text =
            "Prêt à découvrir le classement ? (placeholder)\n" +
                    "Joueurs: ${players.size}, tour courant: $currentTurnIndex"

        // Optionnel : bouton pour continuer plus tard
        findViewById<Button?>(R.id.btnContinue)?.setOnClickListener {
            // TODO: enchaîner vers l’écran final quand il sera prêt
            finish()
        }
    }
}
