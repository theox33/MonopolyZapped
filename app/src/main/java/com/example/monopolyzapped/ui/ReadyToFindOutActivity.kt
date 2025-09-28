package com.example.monopolyzapped.ui

import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.isActive
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.monopolyzapped.NavKeys
import com.example.monopolyzapped.R
import com.example.monopolyzapped.model.Player
import com.example.monopolyzapped.util.MoneyFormat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ReadyToFindOutActivity : AppCompatActivity() {

    // --- Views (ids = ton XML, ne pas modifier) ---
    private lateinit var btnNext: ImageView

    private lateinit var firstPlayerToken: ImageView
    private lateinit var secondPlayerToken: ImageView
    private lateinit var thirdPlayerToken: ImageView
    private lateinit var fourthPlayerToken: ImageView

    private lateinit var firstPlayerMoney: TextView
    private lateinit var secondPlayerMoney: TextView
    private lateinit var thirdPlayerMoney: TextView
    private lateinit var fourthPlayerMoney: TextView

    // --- State ---
    private var players = arrayListOf<Player>()
    private var animationJob: Job? = null

    // --- tokens pachinko : mappe robustement (mot-clé ou nom de fichier) ---
    private fun Player.pachinkoTokenDrawable(): Int {
        val key = (token ?: "").lowercase()
        return when {
            "car" in key      -> R.drawable.pachinko_car
            "ship" in key ||
                    "boat" in key     -> R.drawable.pachinko_ship
            "shoe" in key     -> R.drawable.pachinko_shoe
            "hat" in key      -> R.drawable.pachinko_hat
            "iron" in key     -> R.drawable.pachinko_iron
            "dog" in key      -> R.drawable.pachinko_dog
            else              -> R.drawable.pachinko_car // fallback
        }
    }

    private suspend fun playAndAwait(resId: Int) {
        if (!coroutineContext.isActive) return
        val mp = MediaPlayer.create(this, resId)
        try {
            suspendCancellableCoroutine { cont ->
                mp.setOnCompletionListener {
                    if (!cont.isCompleted) cont.resume(Unit)
                }
                mp.setOnErrorListener { _, _, _ ->
                    if (!cont.isCompleted) cont.resume(Unit)
                    true
                }
                mp.start()
                cont.invokeOnCancellation {
                    try { mp.stop() } catch (_: Exception) {}
                    mp.release()
                }
            }
        } finally {
            try { mp.release() } catch (_: Exception) {}
        }
    }

    private fun setInvisible(vararg views: View) {
        views.forEach {
            it.visibility = View.INVISIBLE
            it.alpha = 0f
        }
    }

    private fun fadeIn(view: View, duration: Long = 420L) {
        view.visibility = View.VISIBLE
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun moneyText(k: Int): String = MoneyFormat.fromK(k.toLong())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ready_to_find_out)

        // Bind views
        btnNext = findViewById(R.id.btnNext)

        firstPlayerToken  = findViewById(R.id.firstPlayerToken)
        secondPlayerToken = findViewById(R.id.secondPlayerToken)
        thirdPlayerToken  = findViewById(R.id.thirdPlayerToken)
        fourthPlayerToken = findViewById(R.id.fourthPlayerToken)

        firstPlayerMoney  = findViewById(R.id.firstPlayerMoney)
        secondPlayerMoney = findViewById(R.id.secondPlayerMoney)
        thirdPlayerMoney  = findViewById(R.id.thirdPlayerMoney)
        fourthPlayerMoney = findViewById(R.id.fourthPlayerMoney)

        // Récupère les joueurs
        intent.setExtrasClassLoader(Player::class.java.classLoader)
        players = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(NavKeys.PLAYERS, Player::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
        }

        // Tout invisible au start
        setInvisible(
            firstPlayerToken, firstPlayerMoney,
            secondPlayerToken, secondPlayerMoney,
            thirdPlayerToken, thirdPlayerMoney,
            fourthPlayerToken, fourthPlayerMoney,
            btnNext
        )

        // Bouton Next -> retour menu principal
        btnNext.setOnClickListener {
            val intent = Intent(this, MainMenuActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Lance la séquence d’animation
        startSequence()
    }

    private fun startSequence() {
        // Tri des joueurs par argent (ascendant : le plus pauvre d’abord)
        val sorted = players.sortedBy { it.money }

        // On extraira jusqu’à 4 joueurs (4e -> 1er)
        val size = sorted.size.coerceIn(1, 4)
        val fourth = if (size >= 4) sorted[0] else null
        val third  = if (size >= 3) sorted[if (size == 3) 0 else 1] else null
        val second = if (size >= 2) sorted[if (size == 2) 0 else 2] else null
        val first  = sorted.lastOrNull() // le plus riche

        animationJob?.cancel()
        animationJob = lifecycleScope.launch {
            // Attend 1 seconde avant de commencer
            delay(1000L)

            // 4e (si 4 joueurs)
            fourth?.let { p ->
                fourthPlayerToken.setImageResource(p.pachinkoTokenDrawable())
                fourthPlayerMoney.text = moneyText(p.money)
                fadeIn(fourthPlayerToken)
                fadeIn(fourthPlayerMoney)
                // Son awe
                playAndAwait(R.raw.crowd_awe)
            }

            // 3e (si >= 3 joueurs)
            third?.let { p ->
                thirdPlayerToken.setImageResource(p.pachinkoTokenDrawable())
                thirdPlayerMoney.text = moneyText(p.money)
                fadeIn(thirdPlayerToken)
                fadeIn(thirdPlayerMoney)
                // Applaudissements 03
                playAndAwait(R.raw.applause_03)
            }

            // 2e (si >= 2 joueurs)
            second?.let { p ->
                secondPlayerToken.setImageResource(p.pachinkoTokenDrawable())
                secondPlayerMoney.text = moneyText(p.money)
                fadeIn(secondPlayerToken)
                fadeIn(secondPlayerMoney)
                // Applaudissements 02
                playAndAwait(R.raw.applause_02)
            }

            // 1er (toujours présent si size >= 1)
            first?.let { p ->
                firstPlayerToken.setImageResource(p.pachinkoTokenDrawable())
                firstPlayerMoney.text = moneyText(p.money)
                fadeIn(firstPlayerToken)
                fadeIn(firstPlayerMoney)
                // Applaudissements 01
                playAndAwait(R.raw.applause_01)
            }

            // Montre le bouton Suivant
            fadeIn(btnNext)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        animationJob?.cancel()
    }
}
