package com.example.monopolyzapped.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
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
import com.example.monopolyzapped.util.MoneyFormat
import com.example.monopolyzapped.util.TransactionHistory
import com.example.monopolyzapped.util.TransactionRecord
import kotlin.math.roundToLong

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

    // Header
    private lateinit var header: ImageView
    private lateinit var headerText: TextView

    // Panneaux payeur / receveur
    private lateinit var ivPayer: ImageView
    private lateinit var ivReceiver: ImageView

    // Infos payeur
    private lateinit var payerName: TextView
    private lateinit var payerToken: ImageView
    private lateinit var payerMoney: TextView

    // Infos receveur
    private lateinit var receiverName: TextView
    private lateinit var receiverToken: ImageView
    private lateinit var receiverMoney: TextView

    // Billets animés
    private lateinit var note1: ImageView
    private lateinit var note2: ImageView
    private lateinit var note3: ImageView
    private lateinit var note4: ImageView

    // État navigation
    private var playersGlobal = arrayListOf<Player>()
    private var firstPlayerIndexGlobal = 0

    // Audio
    private var sfx: MediaPlayer? = null

    // Entrées animation/transaction
    private var amountK: Double = 0.0
    private var payerActor: Int = ACTOR_BANK
    private var receiverActor: Int = ACTOR_BANK

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

        payerName = findViewById(R.id.payerName)
        payerToken = findViewById(R.id.payerToken)
        payerMoney = findViewById(R.id.payerMoney)

        receiverName = findViewById(R.id.receiverName)
        receiverToken = findViewById(R.id.receiverToken)
        receiverMoney = findViewById(R.id.receiverMoney)

        note1 = findViewById(R.id.money1)
        note2 = findViewById(R.id.money2)
        note3 = findViewById(R.id.money3)
        note4 = findViewById(R.id.money4)

        // Extras animation
        val label = intent.getStringExtra(EXTRA_AMOUNT_LABEL) ?: "Transfert..."
        amountK = intent.getDoubleExtra(EXTRA_AMOUNT_K, 0.0)
        payerActor = intent.getIntExtra(EXTRA_PAYER_ACTOR, ACTOR_BANK)
        receiverActor = intent.getIntExtra(EXTRA_RECEIVER_ACTOR, ACTOR_BANK)

        // ➜ Récup navigation propagée (celle que GameMainMenuActivity attend)
        playersGlobal = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(NavKeys.PLAYERS, Player::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
        }
        firstPlayerIndexGlobal = intent.getIntExtra(GameMainMenuActivity.EXTRA_FIRST_PLAYER_INDEX, 0)

        // Header
        header.setImageResource(R.drawable.header_half)
        headerText.text = label

        // Images de fond transferX
        ivPayer.setImageResource(actorToDrawable(payerActor))
        ivReceiver.setImageResource(actorToDrawable(receiverActor))

        // Remplir les infos des panneaux
        val payerPlayer = playerForActor(payerActor)
        val receiverPlayer = playerForActor(receiverActor)
        bindPanelForActor(payerActor, payerPlayer, payerName, payerToken, payerMoney)
        bindPanelForActor(receiverActor, receiverPlayer, receiverName, receiverToken, receiverMoney)

        // Son au début (mettre le fichier en res/raw/money_exchange.mp3)
        try { sfx = MediaPlayer.create(this, R.raw.money_exchange).apply { start() } } catch (_: Throwable) {}

        // Lancer l’animation une fois le layout mesuré
        val notes = listOf(note1, note2, note3, note4)
        val totalDurationNotes = 3000L  // 3s
        val perNote = totalDurationNotes / notes.size

        // Animations de solde (3s) – payeur diminue, receveur augmente
        animatePayerMoney(payerPlayer, amountK, durationMs = totalDurationNotes)
        animateReceiverMoney(receiverPlayer, amountK, durationMs = totalDurationNotes)

        window.decorView.doOnLayout {
            val (startX, startY) = centerOf(ivPayer, notes.first())
            notes.forEach { note ->
                note.x = startX
                note.y = startY
                note.visibility = View.VISIBLE
            }

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

    /** Lie le nom / pion / argent d’un “actor” (joueur ou banque) au panneau. */
    private fun bindPanelForActor(
        actor: Int,
        player: Player?,
        nameTv: TextView,
        tokenIv: ImageView,
        moneyTv: TextView
    ) {
        if (actor == ACTOR_BANK || player == null) {
            nameTv.text = "Banque"
            tokenIv.setImageResource(R.drawable.bank_half)
            moneyTv.visibility = View.GONE
        } else {
            nameTv.text = player.name
            tokenIv.setImageResource(tokenToDrawable(player.token))
            moneyTv.visibility = View.VISIBLE
            moneyTv.text = MoneyFormat.fromK(player.money.toLong())
        }
    }

    /** Anime la baisse du solde du payeur sur `durationMs`. */
    private fun animatePayerMoney(payer: Player?, amountK: Double, durationMs: Long) {
        if (payer == null) return
        if (payerActor == ACTOR_BANK) return // pas d’argent affiché pour la banque

        val startK = payer.money.toLong()
        val deltaK = amountK.roundToLong()
        val endK = (startK - deltaK).coerceAtLeast(0L)

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { va ->
                val t = va.animatedFraction
                val curK = startK - (deltaK * t).roundToLong()
                payerMoney.text = MoneyFormat.fromK(curK)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    payerMoney.text = MoneyFormat.fromK(endK)
                }
            })
        }.start()
    }

    /** Anime la hausse du solde du receveur sur `durationMs`. */
    private fun animateReceiverMoney(receiver: Player?, amountK: Double, durationMs: Long) {
        if (receiver == null) return
        if (receiverActor == ACTOR_BANK) return // pas d’argent affiché pour la banque

        val startK = receiver.money.toLong()
        val deltaK = amountK.roundToLong()
        val endK = (startK + deltaK).coerceAtLeast(0L)

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { va ->
                val t = va.animatedFraction
                val curK = startK + (deltaK * t).roundToLong()
                receiverMoney.text = MoneyFormat.fromK(curK)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    receiverMoney.text = MoneyFormat.fromK(endK)
                }
            })
        }.start()
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

        // Mettre à jour les soldes players (en K)
        val deltaK = amountK.roundToLong()
        val payerPlayer = playerForActor(payer)
        val receiverPlayer = playerForActor(receiver)

        payerPlayer?.let { it.money = (it.money.toLong() - deltaK).coerceAtLeast(0L).toInt() }
        receiverPlayer?.let { it.money = (it.money.toLong() + deltaK).coerceAtLeast(0L).toInt() }

        // Mise à jour visuelle finale (sécurité)
        payerPlayer?.let { if (payerActor != ACTOR_BANK) payerMoney.text = MoneyFormat.fromK(it.money.toLong()) }
        receiverPlayer?.let { if (receiverActor != ACTOR_BANK) receiverMoney.text = MoneyFormat.fromK(it.money.toLong()) }

        // Retour au menu principal AVEC les extras attendus
        val i = Intent(this, GameMainMenuActivity::class.java).apply {
            putParcelableArrayListExtra(NavKeys.PLAYERS, playersGlobal)
            putExtra(GameMainMenuActivity.EXTRA_FIRST_PLAYER_INDEX, firstPlayerIndexGlobal)
        }
        startActivity(i)
        finish()
    }

    // --- Utils mapping / assets ---
    private fun actorToDrawable(actor: Int): Int = when (actor) {
        ACTOR_ORANGE -> R.drawable.transfer0
        ACTOR_BLEUE  -> R.drawable.transfer1
        ACTOR_ROSE   -> R.drawable.transfer2
        ACTOR_VERTE  -> R.drawable.transfer3
        else         -> R.drawable.transfer4 // banque
    }

    private fun playerForActor(actor: Int): Player? {
        if (actor == ACTOR_BANK) return null
        val color = when (actor) {
            ACTOR_ORANGE -> "ORANGE"
            ACTOR_BLEUE  -> "BLEUE"
            ACTOR_ROSE   -> "ROSE"
            ACTOR_VERTE  -> "VERTE"
            else         -> null
        } ?: return null
        return playersGlobal.firstOrNull { it.card == color }
    }

    private fun tokenToDrawable(token: String): Int = when (token) {
        "dog"  -> R.drawable.hasbro_token_dog
        "hat"  -> R.drawable.hasbro_token_hat
        "car"  -> R.drawable.hasbro_token_car
        "iron" -> R.drawable.hasbro_token_iron
        "ship" -> R.drawable.hasbro_token_ship
        "shoe" -> R.drawable.hasbro_token_shoe
        else   -> R.drawable.hasbro_token_car
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
