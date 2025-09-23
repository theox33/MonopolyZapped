package com.example.monopolyzapped.ui

import android.content.Intent
import android.graphics.PointF
import android.graphics.Rect
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import kotlin.math.hypot

class PlayerCardScanActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SHOW_BACK = "show_back"
        const val RESULT_CARD = "result_card"
        const val EXTRA_DEBUG_OVERLAY = "debug_overlay"
        private const val TAG = "CardScan"
    }

    enum class CardColor { ORANGE, BLEUE, ROSE, VERTE }

    private lateinit var scanBand: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var root: View

    private lateinit var ivChosenToken: ImageView
    private lateinit var tvScanStatus: TextView

    private var lastColor: CardColor? = null
    private var stableCount = 0
    private val neededStable = 2

    private var hasCompleted = false
    private var successSfx: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var debugOverlay: DebugOverlayView
    private var debugEnabled = false

    // --- Helpers ---

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
        setContentView(R.layout.activity_card_scan)

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
        tvScanStatus  = findViewById(R.id.tvScanStatus)

        val showBack = intent.getBooleanExtra(EXTRA_SHOW_BACK, true)
        btnBack.visibility = if (showBack) View.VISIBLE else View.GONE
        if (showBack) btnBack.bindClickBackWithPressAndSound { finish() }

        // Récup infos navigation
        val players      = intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
        val totalPlayers = intent.getIntExtra(PlayerSetupActivity.EXTRA_TOTAL_PLAYERS, 2)
        val currentIndex = intent.getIntExtra(PlayerSetupActivity.EXTRA_CURRENT_INDEX, 1)
        val currentPlayer = players.getOrNull(currentIndex - 1)

        // Affiche le pion choisi en haut + texte d’attente
        currentPlayer?.let { p ->
            ivChosenToken.setImageResource(tokenToDrawable(p.token))
            tvScanStatus.text = "${p.name}, choisissez votre carte bancaire et placez-la sur l’écran."
        } ?: run {
            // fallback si pas de joueur (ne devrait pas arriver)
            tvScanStatus.text = "Choisissez votre carte bancaire et placez-la sur l’écran."
        }

        // Touch: attend ≥ 2 points
        root.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_POINTER_DOWN,
                MotionEvent.ACTION_MOVE -> {
                    if (ev.pointerCount >= 2) {
                        val pA = PointF(ev.getX(0), ev.getY(0))
                        val pB = PointF(ev.getX(1), ev.getY(1))
                        Log.d(TAG, "Points détectés : A=(${pA.x},${pA.y}), B=(${pB.x},${pB.y})")

                        if (!pointsInCornerZones(pA, pB)) {
                            Log.d(TAG, "Points hors zones de détection -> ignorés")
                            return@setOnTouchListener true
                        }

                        // Debug overlay: zones + points bruts
                        val rects = buildCornerRects()
                        debugOverlay.setCornerRects(rects)
                        debugOverlay.setPoints(pA, pB, null, null, null, "Raw points")

                        classifyAndApply(pA, pB)
                    }
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_POINTER_UP -> {
                    // Optionnel: si moins de 2 points, revenir à l’état d’attente
                    if (ev.pointerCount < 2 && !hasCompleted && currentPlayer != null) {
                        tvScanStatus.text = "${currentPlayer.name}, choisissez votre carte bancaire et placez-la sur l’écran."
                    }
                }
            }
            true
        }
    }

    // Confirmation + enchaînement
    private fun classifyAndApply(p1: PointF, p2: PointF) {
        if (hasCompleted) return

        val w = resources.displayMetrics.widthPixels
        val detection = classifyCard(p1, p2, w) ?: run {
            Log.d(TAG, "Classification impossible (trop horizontal ou incohérent)")
            return
        }

        // Debug overlay avec mesures
        debugOverlay.setPoints(
            p1, p2,
            detection.dx, detection.dy, detection.distancePx,
            "Candidate: ${detection.color}"
        )

        Log.d(TAG, "Carte détectée: ${detection.color}, dist=${detection.distancePx}, dx=${detection.dx}, dy=${detection.dy}")
        // Stabilisation
        if (detection.color == lastColor) {
            stableCount++
            tvScanStatus.text = "Scan ..."
            Log.d(TAG, "Stabilisation: ${detection.color} (compteur=$stableCount)")
        } else {
            lastColor = detection.color
            stableCount = 1
            Log.d(TAG, "Nouveau candidat: ${detection.color}, reset compteur")
        }
        if (stableCount < neededStable) return

        // Confirmé
        tvScanStatus.text = "Merci!"
        hasCompleted = true
        debugOverlay.setPoints(
            p1, p2, detection.dx, detection.dy, detection.distancePx, "CONFIRMED: ${detection.color}"
        )

        // 1) Bandeau coloré + anim
        when (detection.color) {
            CardColor.ORANGE -> scanBand.setImageResource(R.drawable.cardscan_bg0)
            CardColor.BLEUE  -> scanBand.setImageResource(R.drawable.cardscan_bg1)
            CardColor.ROSE   -> scanBand.setImageResource(R.drawable.cardscan_bg2)
            CardColor.VERTE  -> scanBand.setImageResource(R.drawable.cardscan_bg3)
        }
        scanBand.animate().scaleX(1.03f).scaleY(1.03f)
            .setDuration(90).withEndAction {
                scanBand.animate().scaleX(1f).scaleY(1f)
                    .setDuration(110).setInterpolator(OvershootInterpolator(1.6f)).start()
            }.start()

        tvScanStatus.text = "Merci!"

        // 3) Enregistre la carte pour le joueur courant et enchaîne
        val players = intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
        val totalPlayers = intent.getIntExtra(PlayerSetupActivity.EXTRA_TOTAL_PLAYERS, 2)
        val currentIndex = intent.getIntExtra(PlayerSetupActivity.EXTRA_CURRENT_INDEX, 1)
        val colorName = detection.color.name

        // Refuser carte déjà utilisée
        if (players.any { it.card == colorName }) {
            tvScanStatus.text = "Cette est déjà utilisée par un autre joueur, essayez-en une autre."
            Log.d(TAG, "Carte $colorName déjà prise, annulation")
            // Réautoriser une nouvelle détection
            hasCompleted = false
            stableCount = 0
            lastColor = null
            tvScanStatus.text = "${players[currentIndex - 1].name}, choisissez votre carte bancaire et placez-la sur l’écran."
            return
        }

        // Assigner la carte au joueur courant (index = currentIndex - 1)
        val idx = currentIndex - 1
        if (idx in players.indices) {
            players[idx].card = colorName
        }

        // Son de succès
        try {
            successSfx = MediaPlayer.create(this, R.raw.congrats).apply {
                setOnCompletionListener { it.release() }
                start()
            }
        } catch (_: Throwable) { /* noop */ }

        // After 2.5s: next player or the next screen of the game
        handler.postDelayed({
            val players = intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
            val totalPlayers = intent.getIntExtra(PlayerSetupActivity.EXTRA_TOTAL_PLAYERS, 2)
            val currentIndex = intent.getIntExtra(PlayerSetupActivity.EXTRA_CURRENT_INDEX, 1)
            val nextIndex = currentIndex + 1

            if (nextIndex <= totalPlayers) {
                Log.d(TAG, "Next player: $nextIndex")
                Log.d(TAG, "Players: $players")
                // Continue the setup with the next player
                val i = Intent(this, PlayerSetupActivity::class.java).apply {
                    putExtra(PlayerSetupActivity.EXTRA_TOTAL_PLAYERS, totalPlayers)
                    putExtra(PlayerSetupActivity.EXTRA_CURRENT_INDEX, nextIndex)
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                }
                startActivity(i)
            } else {
                Log.d(TAG, "All players configured, starting final instructions")
                Log.d(TAG, "Players: $players")
                // All players configured -> move forward
                val i = Intent(this, GamePrepDealActivity::class.java).apply {
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                }
                startActivity(i)

                startActivity(i)
            }

            // Close the scan activity so Back won’t come back here
            finish()
        }, 2500L)


    }

    private data class CardDetection(
        val color: CardColor,
        val distancePx: Float,
        val dx: Float,
        val dy: Float
    )

    // Petits motifs = ORANGE/BLEUE ; grands motifs = ROSE/VERTE ; signe de dx sépare les miroirs
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

        Log.d(TAG, "dx=$dx, dy=$dy, dist=$dist")

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
            Rect(0, 0, zoneW, zoneH),  // HG
            Rect(screenW - zoneW, 0, screenW, zoneH), // HD
            Rect(0, screenH - zoneH, zoneW, screenH), // BG
            Rect(screenW - zoneW, screenH - zoneH, screenW, screenH) // BD
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

        val res = rects.any { it.contains(p1) } && rects.any { it.contains(p2) }
        Log.d(TAG, "pointsInCornerZones=$res")
        return res
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        successSfx?.release()
        successSfx = null
    }
}