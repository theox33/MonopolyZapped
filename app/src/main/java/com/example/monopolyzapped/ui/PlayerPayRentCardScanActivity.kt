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


class PlayerPayRentCardScanActivity : AppCompatActivity() {

    enum class CardColor { ORANGE, BLEUE, ROSE, VERTE }

    // UI
    private lateinit var root: View
    private lateinit var scanBand: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var ivChosenToken: ImageView
    private lateinit var tvScanStatus: TextView
    private lateinit var tvPlayerName: TextView
    private lateinit var tvPlayerMoney: TextView

    // État
    private var lastColor: CardColor? = null
    private var stableCount = 0
    private val neededStable = 2
    private var hasCompleted = false

    // Sons & handler
    private var successSfx: MediaPlayer? = null
    private var buzzerSfx: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    // Debug
    private lateinit var debugOverlay: DebugOverlayView
    private var debugEnabled = false

    // Données globales
    private var amountKGlobal: Double = 0.0
    private var currentPlayerGlobal: Player? = null
    private var playersGlobal = arrayListOf<Player>()
    private var totalPlayersGlobal = 0
    private var currentIndexGlobal = 1 // 1-based
    private var firstPlayerIndexGlobal = 0 // 0-based

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
        setContentView(R.layout.activity_player_pay_rent_card_scan)

        intent.setExtrasClassLoader(Player::class.java.classLoader)

        // Overlay debug
        debugEnabled = intent.getBooleanExtra(PlayerPayToBankCardScanActivity.EXTRA_DEBUG_OVERLAY, true)
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

        // Bind UI
        root = findViewById(R.id.cardScanRoot)
        scanBand = findViewById(R.id.scanBand)
        btnBack = findViewById(R.id.btnBack)
        ivChosenToken = findViewById(R.id.ivChosenToken)
        tvScanStatus = findViewById(R.id.tvScanStatus)
        tvPlayerName = findViewById(R.id.tvPlayerName)
        tvPlayerMoney = findViewById(R.id.tvPlayerMoney)

        val showBack = intent.getBooleanExtra(PlayerPayToBankCardScanActivity.EXTRA_SHOW_BACK, true)
        btnBack.visibility = if (showBack) View.VISIBLE else View.GONE
        if (showBack) btnBack.bindClickBackWithPressAndSound { finish() }

        // --- Récup nav ---
        // Players (compat SDK 33+)
        val players: ArrayList<Player> = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(NavKeys.PLAYERS, Player::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
        }

        // Indice du tour (0-based) envoyé par PlayerMenuActivity
        val turnIndex0 = intent.getIntExtra(PlayerMenuActivity.EXTRA_TURN_INDEX, -1)

        // Fallback: si jamais l’extra n’est pas là, on retombe sur l’ancien CURRENT_INDEX (1-based)
        val effectiveIndex0 = when {
            turnIndex0 >= 0 -> turnIndex0
            else -> {
                val idx1 = intent.getIntExtra(PlayerSetupActivity.EXTRA_CURRENT_INDEX, 1)
                idx1 - 1
            }
        }.coerceIn(0, (players.size - 1).coerceAtLeast(0))

        // Alimente les globals et le joueur courant
        playersGlobal = players
        totalPlayersGlobal = players.size
        firstPlayerIndexGlobal = effectiveIndex0               // 0-based (attendu par GameMainMenuActivity)
        currentIndexGlobal = effectiveIndex0 + 1               // miroir 1-based si besoin en aval
        val currentPlayer = players.getOrNull(effectiveIndex0)
        currentPlayerGlobal = currentPlayer

        // Montant optionnel (non affiché ici)
        amountKGlobal = intent.getDoubleExtra(PlayerPayToBankCardScanActivity.EXTRA_AMOUNT_K, 0.0)

        // Header
        currentPlayer?.let { p ->
            ivChosenToken.setImageResource(tokenToDrawable(p.token))
            tvPlayerName.text = p.name
            tvPlayerMoney.text = formatMoneyK(p.money.toDouble())
            tvScanStatus.text = "${p.name}, scannez la carte pour payer le loyer."
        } ?: run {
            tvPlayerName.text = ""
            tvPlayerMoney.text = ""
            tvScanStatus.text = "Scannez la carte du joueur pour payer le loyer."
        }


        // Touch: 2 doigts dans coins
        root.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_POINTER_DOWN,
                MotionEvent.ACTION_MOVE -> {
                    if (ev.pointerCount >= 2) {
                        val pA = PointF(ev.getX(0), ev.getY(0))
                        val pB = PointF(ev.getX(1), ev.getY(1))

                        val rects = buildCornerRects()
                        debugOverlay.setCornerRects(rects)

                        if (!pointsInCornerZones(pA, pB)) {
                            return@setOnTouchListener true
                        }

                        debugOverlay.setPoints(pA, pB, null, null, null, "Raw points")
                        classifyAndValidate(pA, pB, currentPlayer)
                    }
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_POINTER_UP -> {
                    if (ev.pointerCount < 2 && !hasCompleted && currentPlayer != null) {
                        tvScanStatus.text = "${currentPlayer.name}, scannez la carte pour payer le loyer."
                    }
                }
            }
            true
        }
    }

    // --- Détection & validation ---

    private data class CardDetection(
        val color: CardColor,
        val distancePx: Float,
        val dx: Float,
        val dy: Float
    )

    private fun classifyAndValidate(p1: PointF, p2: PointF, currentPlayer: Player?) {
        if (hasCompleted || currentPlayer == null) return

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

        // Verrou joueur courant uniquement
        val expected = currentPlayer.card?.uppercase()
        val scanned = detection.color.name.uppercase()

        if (expected.isNullOrEmpty()) {
            tvScanStatus.text = "Aucune carte assignée à ${currentPlayer.name}."
            resetStabilization(currentPlayer)
            return
        }
        if (scanned != expected) {
            tvScanStatus.text = "Ce n’est pas la bonne carte. ${currentPlayer.name} doit scanner sa propre carte."
            wobbleScanBand()
            playBuzzer()
            resetStabilization(currentPlayer)
            return
        }

        // Succès
        hasCompleted = true
        applyBandColor(detection.color)
        bounceScanBand()
        playSuccess()

        handler.postDelayed({
            val intentNext = Intent(this, PlayerPayRentEnterAmountActivity::class.java).apply {
                putParcelableArrayListExtra(NavKeys.PLAYERS, playersGlobal)

                // indices/navigation
                putExtra(GameMainMenuActivity.EXTRA_FIRST_PLAYER_INDEX, firstPlayerIndexGlobal)
                putExtra(PlayerMenuActivity.EXTRA_TURN_INDEX, firstPlayerIndexGlobal)
                putExtra(PlayerMenuActivity.EXTRA_PLAYER_INDEX, firstPlayerIndexGlobal)

                putExtra(PlayerSetupActivity.EXTRA_TOTAL_PLAYERS, totalPlayersGlobal)
                putExtra(PlayerSetupActivity.EXTRA_CURRENT_INDEX, currentIndexGlobal)
            }
            startActivity(intentNext)
            finish()
        }, 1000L)
    }

    private fun classifyCard(
        p1: PointF,
        p2: PointF,
        screenWidthPx: Int,
        distanceThresholdRatio: Float = 0.074f,
        minVerticalRatio: Float = 0.04f   // valeur d’origine (pas plus permissif)
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

    // Zones coins: tailles fixes PX (271*2, 158*2) comme à l’origine
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

    // Anim & sons
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
        } catch (_: Throwable) { }
    }

    private fun playBuzzer() {
        try {
            buzzerSfx?.release()
            buzzerSfx = MediaPlayer.create(this, R.raw.gameshow_buzzer_04).apply {
                setOnCompletionListener { it.release() }
                start()
            }
        } catch (_: Throwable) { }
    }

    private fun colorNameToTransferIndex(colorName: String?): Int = when (colorName) {
        "ORANGE" -> 0
        "BLEUE"  -> 1
        "ROSE"   -> 2
        "VERTE"  -> 3
        else     -> 4 // fallback banque
    }

    // Format K/M
    private fun formatMoneyK(valueK: Double): String {
        return if (abs(valueK) < 1000.0) {
            val rounded = ((valueK * 1000.0).toLong()).toDouble() / 1000.0
            trimZeros("$rounded K")
        } else {
            val m = valueK / 1000.0
            val r = round(m * 100.0) / 100.0
            trimZeros("$r M")
        }
    }
    private fun trimZeros(s: String): String =
        s.replace(Regex("(\\.0+)(\\s*[KM])$")) { it.groupValues[2] }

    private fun resetStabilization(currentPlayer: Player) {
        hasCompleted = false
        stableCount = 0
        lastColor = null
        handler.postDelayed({
            if (!hasCompleted) {
                tvScanStatus.text = "${currentPlayer.name}, scannez la carte pour payer le loyer."
            }
        }, 800L)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        successSfx?.release(); successSfx = null
        buzzerSfx?.release(); buzzerSfx = null
    }
}
