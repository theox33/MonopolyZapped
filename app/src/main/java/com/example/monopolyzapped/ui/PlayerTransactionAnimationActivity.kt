package com.example.monopolyzapped.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import com.example.monopolyzapped.NavKeys
import com.example.monopolyzapped.R
import com.example.monopolyzapped.model.Player
import com.example.monopolyzapped.util.TransactionHistory
import com.example.monopolyzapped.util.TransactionRecord

class PlayerTransactionAnimationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_AMOUNT_LABEL = "amount_label"   // ex: "Transfert... 250 K"
        const val EXTRA_AMOUNT_K = "amount_k"           // Double en K
        const val EXTRA_PAYER_ACTOR = "payer_actor"     // 0..4 (0O,1B,2R,3V,4Bank)
        const val EXTRA_RECEIVER_ACTOR = "receiver_actor"

        // mapping transferX.png
        const val ACTOR_ORANGE = 0
        const val ACTOR_BLEUE  = 1
        const val ACTOR_ROSE   = 2
        const val ACTOR_VERTE  = 3
        const val ACTOR_BANK   = 4
    }

    private lateinit var header: ImageView
    private lateinit var headerText: TextView
    private lateinit var ivPayer: ImageView
    private lateinit var ivReceiver: ImageView
    private lateinit var note1: ImageView
    private lateinit var note2: ImageView
    private lateinit var note3: ImageView
    private lateinit var note4: ImageView

    private var playersGlobal = arrayListOf<Player>()
    private var firstPlayerIndexGlobal = 0

    private var sfx: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_transaction_animation)

        // Sécurise les parcelables
        intent.setExtrasClassLoader(Player::class.java.classLoader)

        // Views
        header = findViewById(R.id.headerImg)
        headerText = findViewById(R.id.headerText)
        ivPayer = findViewById(R.id.ivPayer)
        ivReceiver = findViewById(R.id.ivReceiver)
        note1 = findViewById(R.id.money1)
        note2 = findViewById(R.id.money2)
        note3 = findViewById(R.id.money3)
        note4 = findViewById(R.id.money4)

        // Extras animation
        val label = intent.getStringExtra(EXTRA_AMOUNT_LABEL) ?: "Transfert..."
        val amountK = intent.getDoubleExtra(EXTRA_AMOUNT_K, 0.0)
        val payerActor = intent.getIntExtra(EXTRA_PAYER_ACTOR, ACTOR_BANK)
        val receiverActor = intent.getIntExtra(EXTRA_RECEIVER_ACTOR, ACTOR_BANK)

        // ➜ Récup navigation propagée (celle que GameMainMenuActivity attend)
        playersGlobal = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(NavKeys.PLAYERS, Player::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
        }
        firstPlayerIndexGlobal = intent.getIntExtra(GameMainMenuActivity.EXTRA_FIRST_PLAYER_INDEX, 0)

        header.setImageResource(R.drawable.header_half)
        headerText.text = label

        ivPayer.setImageResource(actorToDrawable(payerActor))
        ivReceiver.setImageResource(actorToDrawable(receiverActor))

        // Son au début (mettre le fichier en res/raw/money_exchange.mp3)
        try {
            sfx = MediaPlayer.create(this, R.raw.money_exchange).apply { start() }
        } catch (_: Throwable) {}

        // Lancer l’animation une fois le layout mesuré
        val notes = listOf(note1, note2, note3, note4)
        val totalDuration = 3000L
        val perNote = totalDuration / notes.size

        // Placer les billets "sous" l'image du payeur (même centre) avant d'animer
        window.decorView.doOnLayout {
            val (startX, startY) = centerOf(ivPayer, notes.first())
            notes.forEach { note ->
                note.x = startX
                note.y = startY
                note.visibility = View.VISIBLE
            }

            // Build animations séquentielles
            fun animFor(note: ImageView, idx: Int): ObjectAnimator {
                val (sx, sy) = centerOf(ivPayer, note)
                val (tx, ty) = centerOf(ivReceiver, note)
                note.x = sx; note.y = sy
                val px = PropertyValuesHolder.ofFloat(View.X, sx, tx)
                val py = PropertyValuesHolder.ofFloat(View.Y, sy, ty)
                return ObjectAnimator.ofPropertyValuesHolder(note, px, py).apply {
                    duration = perNote
                    interpolator = AccelerateDecelerateInterpolator()
                    startDelay = idx * perNote
                }
            }

            val anims = notes.mapIndexed { i, v -> animFor(v, i) }
            var completed = 0
            anims.forEach { a ->
                a.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        completed++
                        if (completed == anims.size) {
                            onTransferFinished(amountK, payerActor, receiverActor)
                        }
                    }
                })
                a.start()
            }
        }
    }

    private fun onTransferFinished(amountK: Double, payer: Int, receiver: Int) {
        // Enregistrer la transaction (pile)
        TransactionHistory.push(
            TransactionRecord(
                amountK = amountK,
                payerActor = payer,
                receiverActor = receiver,
                timestamp = System.currentTimeMillis()
            )
        )

        // Retour au menu principal AVEC les extras attendus
        val i = Intent(this, GameMainMenuActivity::class.java).apply {
            putParcelableArrayListExtra(NavKeys.PLAYERS, playersGlobal)
            putExtra(GameMainMenuActivity.EXTRA_FIRST_PLAYER_INDEX, firstPlayerIndexGlobal)
        }
        startActivity(i)
        finish()
    }

    private fun actorToDrawable(actor: Int): Int = when (actor) {
        ACTOR_ORANGE -> R.drawable.transfer0
        ACTOR_BLEUE  -> R.drawable.transfer1
        ACTOR_ROSE   -> R.drawable.transfer2
        ACTOR_VERTE  -> R.drawable.transfer3
        else         -> R.drawable.transfer4 // banque
    }

    /** Centre `child` sur `anchor` et renvoie (x,y). */
    private fun centerOf(anchor: View, child: View): Pair<Float, Float> {
        val ax = anchor.x
        val ay = anchor.y
        val cx = ax + (anchor.width - child.width) / 2f
        val cy = ay + (anchor.height - child.height) / 2f
        return cx to cy
    }

    override fun onDestroy() {
        super.onDestroy()
        try { sfx?.release() } catch (_: Throwable) {}
        sfx = null
    }
}
