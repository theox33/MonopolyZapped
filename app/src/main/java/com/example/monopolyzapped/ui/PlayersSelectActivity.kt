package com.example.monopolyzapped.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.R
import com.example.monopolyzapped.NavKeys
import android.media.MediaPlayer



class PlayersSelectActivity : AppCompatActivity() {

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
        setContentView(R.layout.activity_players_select)

        // Clicks
        val btnBack = findViewById<View>(R.id.btnBack)
        btnBack.bindClickBackWithPressAndSound {
            finish()
        }

        val btn2p = findViewById<View>(R.id.btn2p)
        btn2p.bindClickWithPressAndSound {
            goNext(2)
        }
        findViewById<View>(R.id.btn3p).bindClickWithPressAndSound {
            goNext(3)
        }
        findViewById<View>(R.id.btn4p).bindClickWithPressAndSound {
            goNext(4)
        }
    }

    private fun goNext(players: Int) {
        val intent = Intent(this, PlayerSetupActivity::class.java).apply {
            putExtra(NavKeys.TOTAL_PLAYERS, players)
            putExtra(NavKeys.CURRENT_INDEX, 1)
            putParcelableArrayListExtra(NavKeys.PLAYERS, arrayListOf())
        }
        startActivity(intent)
    }





}
