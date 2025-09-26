package com.example.monopolyzapped.ui

import android.content.Intent
import android.graphics.PointF
import android.graphics.Rect
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.NavKeys
import com.example.monopolyzapped.R
import com.example.monopolyzapped.model.Player
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.round

class PlayerReceiveRentCardScanActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SHOW_BACK = "show_back"
        const val EXTRA_DEBUG_OVERLAY = "debug_overlay"
        /** Montant saisi dans l’écran précédent, en K (base de calcul du projet) */
        const val EXTRA_AMOUNT_K = "amount_k"

        /** Résultat renvoyé à l’activité appelante */
        const val RESULT_CARD = "result_card"
        private const val TAG = "ReceiveRentCardScan"
    }

    enum class CardColor { ORANGE, BLEUE, ROSE, VERTE }

    private lateinit var root: View
    private lateinit var scanBand: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var ivChosenToken: ImageView
    private lateinit var tvScanStatus: TextView

    private lateinit var tvPlayerName: TextView
    private lateinit var tvPlayerMoney: TextView
    private lateinit var tvAmountToPay: TextView

    private var lastColor: CardColor? = null
    private var stableCount = 0
    private val neededStable = 2
    private var hasCompleted = false
    private var successSfx: MediaPlayer? = null
    private var buzzerSfx: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var debugOverlay: DebugOverlayView
    private var debugEnabled = false

    // mémoriser pour re-use lors du succès
    private var amountKGlobal: Double = 0.0
    private var currentPlayerGlobal: Player? = null

    private var playersGlobal = arrayListOf<Player>()
    private var totalPlayersGlobal = 0
    private var currentIndexGlobal = 1
    private var firstPlayerIndexGlobal = 0 // <- index 0-based attendu par GameMainMenuActivity

    private fun tokenToDrawable(token: String): Int = when (token) {
        "dog"  -> R.drawable.hasbro_token_dog
        "hat"  -> R.drawable.hasbro_token_hat
        "car"  -> R.drawable.hasbro_token_car
        "iron" -> R.drawable.hasbro_token_iron
        "ship" -> R.drawable.hasbro_token_ship
        "shoe" -> R.drawable.hasbro_token_shoe
        else   -> R.drawable.hasbro_token_car
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_receive_rent_card_scan)

        // --- Sécurise le ClassLoader pour les Parcelables ---
        intent.setExtrasClassLoader(Player::class.java.classLoader)

        // --- overlay debug optionnel ---
        debugEnabled = intent.getBooleanExtra(EXTRA_DEBUG_OVERLAY, true)
        debugOverlay = DebugOverlayView(this).apply {
            isClickable = false
            isFocusable = false
            visibility = if (debugEnabled) View.VISIBLE else View.GONE
        }
        findViewById<ViewGroup>(android.R.id.content).addView(
            debugOverlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // --- UI ---
        root = findViewById(R.id.cardScanRoot)
        scanBand = findViewById(R.id.scanBand)
        btnBack = findViewById(R.id.btnBack)
        ivChosenToken = findViewById(R.id.ivChosenToken)
        tvScanStatus = findViewById(R.id.tvScanStatus)

        tvPlayerName = findViewById(R.id.tvPlayerName)
        tvPlayerMoney = findViewById(R.id.tvPlayerMoney)
        tvAmountToPay = findViewById(R.id.tvAmountToPay)

        val showBack = intent.getBooleanExtra(EXTRA_SHOW_BACK, true)
        btnBack.visibility = if (showBack) View.VISIBLE else View.GONE
        if (showBack) btnBack.bindClickBackWithPressAndSound { finish() }

        // --- Récup navigation + montant en K (compat SDK 33+) ---
        val players: ArrayList<Player> = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(NavKeys.PLAYERS, Player::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
        }
        val totalPlayers = intent.getIntExtra(PlayerSetupActivity.EXTRA_TOTAL_PLAYERS, 2)
        val currentIndex = intent.getIntExtra(PlayerSetupActivity.EXTRA_CURRENT_INDEX, 1)
        val currentPlayer = players.getOrNull(currentIndex - 1)
        val amountK = intent.getDoubleExtra(EXTRA_AMOUNT_K, 0.0)

        // stock pour usage ultérieur
        amountKGlobal = amountK
        currentPlayerGlobal = currentPlayer

        // stocke aussi players + index
        playersGlobal = players
        totalPlayersGlobal = totalPlayers
        currentIndexGlobal = currentIndex
        firstPlayerIndexGlobal = (currentIndex - 1).coerceIn(0, (players.size - 1).coerceAtLeast(0))

        // --- État initial de l'UI ---
        // Ces trois vues doivent être invisibles tant qu’aucun receveur n’est scanné
        ivChosenToken.visibility = View.INVISIBLE
        tvPlayerName.visibility = View.INVISIBLE
        tvPlayerMoney.visibility = View.INVISIBLE

        // Montant affiché en positif (on perçoit)
        tvAmountToPay.text = "+ ${formatMoneyK(amountK)}"
        tvScanStatus.text = "Scannez votre carte pour percevoir le loyer."

        // Touch: ≥ 2 points dans coins
        root.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_POINTER_DOWN,
                MotionEvent.ACTION_MOVE -> {
                    if (ev.pointerCount >= 2) {
                        val pA = PointF(ev.getX(0), ev.getY(0))
                        val pB = PointF(ev.getX(1), ev.getY(1))

                        if (!pointsInCornerZones(pA, pB)) {
                            return@setOnTouchListener true
                        }

                        val rects = buildCornerRects()
                        debugOverlay.setCornerRects(rects)
                        debugOverlay.setPoints(pA, pB, null, null, null, "Raw points")

                        classifyAndValidate(pA, pB, currentPlayer)
                    }
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_POINTER_UP -> {
                    if (ev.pointerCount < 2 && !hasCompleted) {
                        tvScanStatus.text = "Scannez votre carte pour percevoir le loyer."
                    }
                }
            }
            true
        }
    }

    // --- Validation spécifique "recevoir loyer" ---
    private fun classifyAndValidate(p1: PointF, p2: PointF, currentPlayer: Player?) {
        if (hasCompleted) return
        if (currentPlayer == null) return

        val w = resources.displayMetrics.widthPixels
        val detection = classifyCard(p1, p2, w) ?: return

        debugOverlay.setPoints(
            p1, p2, detection.dx, detection.dy, detection.distancePx,
            "Candidate: ${detection.color}"
        )

        if (detection.color == lastColor) {
            stableCount++
            tvScanStatus.text = "Vérification..."
        } else {
            lastColor = detection.color
            stableCount = 1
        }
        if (stableCount < neededStable) return

        val scanned = detection.color.name       // couleur scannée
        val expectedPayer = currentPlayer.card   // couleur du joueur courant (payeur)

        if (expectedPayer.isNullOrEmpty()) {
            tvScanStatus.text = "Aucune carte assignée au joueur courant. Réessayez après configuration."
            resetStabilization()
            return
        }

        // Cas interdit: le payeur essaie de se payer lui-même
        if (scanned == expectedPayer) {
            tvScanStatus.text = "Vous ne pouvez pas vous payer vous-même!"
            wobbleScanBand()
            playBuzzer()
            resetStabilization()
            return
        }

        // Chercher le "receiver" parmi les joueurs par couleur
        val receiver = playersGlobal.firstOrNull { it.card == scanned }
        if (receiver == null) {
            tvScanStatus.text = "Cette carte n'est attribuée à aucun joueur."
            wobbleScanBand()
            playBuzzer()
            resetStabilization()
            return
        }

        // --- Succès: on a identifié le destinataire (≠ joueur courant) ---
        hasCompleted = true
        applyBandColor(detection.color)
        bounceScanBand()
        playSuccess()

        // ➜ Affiche le bandeau du RECEVEUR maintenant
        ivChosenToken.setImageResource(tokenToDrawable(receiver.token))
        tvPlayerName.text = receiver.name
        tvPlayerMoney.text = formatMoneyK(receiver.money.toDouble())
        ivChosenToken.visibility = View.VISIBLE
        tvPlayerName.visibility = View.VISIBLE
        tvPlayerMoney.visibility = View.VISIBLE
        tvScanStatus.text = "${receiver.name} va percevoir le loyer."

        handler.postDelayed({
            val payerActor = colorNameToTransferIndex(expectedPayer)
            val receiverActor = colorNameToTransferIndex(scanned)
            val amountFormatted = formatMoneyK(amountKGlobal)

            val intentAnim = Intent(this, PlayerTransactionAnimationActivity::class.java).apply {
                putExtra(PlayerTransactionAnimationActivity.EXTRA_AMOUNT_LABEL, "Transfert... $amountFormatted")
                putExtra(PlayerTransactionAnimationActivity.EXTRA_AMOUNT_K, amountKGlobal)
                putExtra(PlayerTransactionAnimationActivity.EXTRA_PAYER_ACTOR, payerActor)
                putExtra(PlayerTransactionAnimationActivity.EXTRA_RECEIVER_ACTOR, receiverActor)

                // ➜ Propager la navigation (et l’index attendu par GameMainMenuActivity)
                putParcelableArrayListExtra(NavKeys.PLAYERS, playersGlobal)
                putExtra(GameMainMenuActivity.EXTRA_FIRST_PLAYER_INDEX, firstPlayerIndexGlobal)

                // Optionnel
                putExtra(PlayerSetupActivity.EXTRA_TOTAL_PLAYERS, totalPlayersGlobal)
                putExtra(PlayerSetupActivity.EXTRA_CURRENT_INDEX, currentIndexGlobal)
            }
            startActivity(intentAnim)
            finish()
        }, 1000L)
    }

    private fun resetStabilization() {
        hasCompleted = false
        stableCount = 0
        lastColor = null
        handler.postDelayed({
            if (!hasCompleted) {
                tvScanStatus.text = "Scannez votre carte pour percevoir le loyer."
            }
        }, 800L)
    }

    private fun wobbleScanBand() {
        scanBand.animate().rotation(2f).setDuration(60).withEndAction {
            scanBand.animate().rotation(-2f).setDuration(60).withEndAction {
                scanBand.animate().rotation(0f).setDuration(50).start()
            }.start()
        }.start()
    }

    private fun bounceScanBand() {
        scanBand.animate().scaleX(1.03f).scaleY(1.03f)
            .setDuration(90).withEndAction {
                scanBand.animate().scaleX(1f).scaleY(1f)
                    .setDuration(110).setInterpolator(OvershootInterpolator(1.6f)).start()
            }.start()
    }

    private fun applyBandColor(color: CardColor) {
        when (color) {
            CardColor.ORANGE -> scanBand.setImageResource(R.drawable.cardscan_bg0)
            CardColor.BLEUE  -> scanBand.setImageResource(R.drawable.cardscan_bg1)
            CardColor.ROSE   -> scanBand.setImageResource(R.drawable.cardscan_bg2)
            CardColor.VERTE  -> scanBand.setImageResource(R.drawable.cardscan_bg3)
        }
    }

    private fun playSuccess() {
        try {
            successSfx = MediaPlayer.create(this, R.raw.cash_register_open_03).apply {
                setOnCompletionListener { it.release() }
                start()
            }
        } catch (_: Throwable) { /* noop */ }
    }

    private fun playBuzzer() {
        try {
            buzzerSfx?.release()
            buzzerSfx = MediaPlayer.create(this, R.raw.gameshow_buzzer_04).apply {
                setOnCompletionListener { it.release() }
                start()
            }
        } catch (_: Throwable) { /* noop */ }
    }

    // --- Détection / aides (copié depuis PlayerCardScanActivity) ---
    private data class CardDetection(
        val color: CardColor,
        val distancePx: Float,
        val dx: Float,
        val dy: Float
    )

    private fun classifyCard(
        p1: PointF,
        p2: PointF,
        screenWidthPx: Int,
        distanceThresholdRatio: Float = 0.074f,
        minVerticalRatio: Float = 0.04f
    ): CardDetection? {
        val top = if (p1.y <= p2.y) p1 else p2
        val bottom = if (p1.y <= p2.y) p2 else p1

        val dx = bottom.x - top.x
        val dy = bottom.y - top.y
        val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()

        if (dy < screenWidthPx * minVerticalRatio) return null

        val smallMotif = (dist / screenWidthPx) < distanceThresholdRatio
        val color = when {
            smallMotif && dx > 0  -> CardColor.ORANGE
            smallMotif && dx <= 0 -> CardColor.BLEUE
            !smallMotif && dx > 0 -> CardColor.ROSE
            else                  -> CardColor.VERTE
        }
        return CardDetection(color, dist, dx, dy)
    }

    private fun buildCornerRects(): List<Rect> {
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        val zoneW = 271 * 2
        val zoneH = 158 * 2
        return listOf(
            Rect(0, 0, zoneW, zoneH),
            Rect(screenW - zoneW, 0, screenW, zoneH),
            Rect(0, screenH - zoneH, zoneW, screenH),
            Rect(screenW - zoneW, screenH - zoneH, screenW, screenH)
        )
    }

    private fun pointsInCornerZones(p1: PointF, p2: PointF): Boolean {
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        val zoneW = 271 * 2
        val zoneH = 158 * 2

        data class RectPx(val left: Int, val top: Int, val right: Int, val bottom: Int) {
            fun contains(p: PointF): Boolean =
                p.x >= left && p.x <= right && p.y >= top && p.y <= bottom
        }

        val rects = listOf(
            RectPx(0, 0, zoneW, zoneH),
            RectPx(screenW - zoneW, 0, screenW, zoneH),
            RectPx(0, screenH - zoneH, zoneW, screenH),
            RectPx(screenW - zoneW, screenH - zoneH, screenW, screenH)
        )

        return rects.any { it.contains(p1) } && rects.any { it.contains(p2) }
    }

    private fun colorNameToTransferIndex(colorName: String?): Int = when (colorName) {
        "ORANGE" -> 0
        "BLEUE"  -> 1
        "ROSE"   -> 2
        "VERTE"  -> 3
        else     -> 4 // par défaut banque
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        successSfx?.release()
        successSfx = null
        buzzerSfx?.release()
        buzzerSfx = null
    }

    // --- Format d'affichage K/M (logique projet) ---
    /** valueK est en K (milliers d’unités). Affiche "xxx K" si < 1000, sinon "x.xx M". */
    private fun formatMoneyK(valueK: Double): String {
        return if (abs(valueK) < 1000.0) {
            val rounded = ((valueK * 1000.0).toLong()).toDouble() / 1000.0
            trimZeros("$rounded K")
        } else {
            val m = valueK / 1000.0
            val r = round(m * 100.0) / 100.0 // 2 décimales
            trimZeros("$r M")
        }
    }

    private fun trimZeros(s: String): String {
        return s.replace(Regex("(\\.0+)(\\s*[KM])$")) { it.groupValues[2] }
    }
}
