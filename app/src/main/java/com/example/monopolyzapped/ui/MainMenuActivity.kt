package com.example.monopolyzapped.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.R
import android.media.MediaPlayer


class MainMenuActivity : AppCompatActivity() {

    private fun View.fadeIn(delay: Long, dy: Float = 0f, duration: Long = 450) {
        alpha = 0f
        translationY = dy
        animate().alpha(1f).translationY(0f)
            .setStartDelay(delay).setDuration(duration).start()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        val logo = findViewById<View>(R.id.logo)
        val mr = findViewById<View>(R.id.mr)
        val left = findViewById<View>(R.id.leftColumn)
        val grid = findViewById<View>(R.id.rightGrid)

        // fait apparaître les éléments (qui sont alpha=0 dans le XML)
        logo.fadeIn(delay = 0, dy = -80f)
        mr.fadeIn(delay = 500)
        left.fadeIn(delay = 800)
        grid.fadeIn(delay = 1000)

        val btnStart = findViewById<View>(R.id.btnStart)
        btnStart.bindClickWithPressAndSound {
            startActivity(Intent(this, PlayersSelectActivity::class.java))
        }

    }
}
