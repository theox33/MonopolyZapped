package com.example.monopolyzapped.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.ui.MainMenuActivity
import com.example.monopolyzapped.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val txt = findViewById<View>(R.id.splashText)

        // point de départ : invisible
        txt.alpha = 0f

        // Fade-in (1.2s) -> pause (1s) -> Fade-out (0.8s) -> lancer MainActivity
        txt.animate().alpha(1f).setDuration(1200).withEndAction {
            txt.animate().setStartDelay(1000).alpha(0f).setDuration(800).withEndAction {
                startActivity(Intent(this, MainMenuActivity::class.java))
                finish()
            }.start()
        }.start()
    }
}
