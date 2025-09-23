package com.example.monopolyzapped.ui

import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.NavKeys
import com.example.monopolyzapped.R
import com.example.monopolyzapped.model.Player
import kotlin.math.atan2
import kotlin.math.exp
import android.content.Intent
import android.animation.ObjectAnimator
import kotlin.random.Random
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import android.widget.FrameLayout


class PlayerChooseSideActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CURRENT_INDEX = "current_index" // 1-based
    }

    private enum class Corner(val key: String) { TL("tl"), TR("tr"), BL("bl"), BR("br") }

    // Center
    private lateinit var quadInfo: ImageView
    private lateinit var quadText: TextView

    // Tap tokens
    private lateinit var tokenTapTL: ImageView
    private lateinit var tokenTapTR: ImageView
    private lateinit var tokenTapBL: ImageView
    private lateinit var tokenTapBR: ImageView

    // Tiles + content
    private lateinit var tileTL: View; private lateinit var tileTR: View
    private lateinit var tileBL: View; private lateinit var tileBR: View

    private lateinit var tileTokenTL: ImageView; private lateinit var tileTokenTR: ImageView
    private lateinit var tileTokenBL: ImageView; private lateinit var tileTokenBR: ImageView

    private lateinit var tileNameTL: TextView; private lateinit var tileNameTR: TextView
    private lateinit var tileNameBL: TextView; private lateinit var tileNameBR: TextView

    private lateinit var tileBottomTL: ImageView; private lateinit var tileBottomTR: ImageView
    private lateinit var tileBottomBL: ImageView; private lateinit var tileBottomBR: ImageView

    private lateinit var tileMoneyTL: TextView; private lateinit var tileMoneyTR: TextView
    private lateinit var tileMoneyBL: TextView; private lateinit var tileMoneyBR: TextView

    // Arrow (tirage)
    private lateinit var arrowSpin: ImageView
    private var spinPhase = false
    private var spinDone = false

    // --- Flèche (phase de tirage) ---
    private var ivArrow: ImageView? = null
    private var spinRunning = false
    private var winnerIndex = -1

    private var predeterminedWinnerAngle = 0f
    private var hasPredeterminedWinner = false
    private var userHasInteracted = false


    // Décalage selon l’orientation du PNG de la flèche.
    // Si ton asset pointe VERS LA DROITE, laisse 0f.
    // S’il pointe vers le HAUT, mets -90f (Android 0° = vers la droite).
    private val ARROW_ZERO_OFFSET_DEG = 90f


    // centre de la flèche en coordonnées écran (pour calcul d’angle)
    private var centerX = 0f
    private var centerY = 0f

    // tracking du geste
    private var lastAngle = 0f
    private var lastTime = 0L
    private var angularVelocity = 0f // deg/s

    // State
    private var players = arrayListOf<Player>()
    private var currentIndex = 1  // 1-based

    // --- drawables helpers ---
    private fun Player.tokenDrawableBg(): Int = when (token) {
        "dog"  -> R.drawable.hasbro_token_dog_bg
        "hat"  -> R.drawable.hasbro_token_hat_bg
        "car"  -> R.drawable.hasbro_token_car_bg
        "iron" -> R.drawable.hasbro_token_iron_bg
        "ship" -> R.drawable.hasbro_token_ship_bg
        "shoe" -> R.drawable.hasbro_token_shoe_bg
        else   -> R.drawable.hasbro_token_car_bg
    }
    private fun Player.tokenDrawableNoBg(): Int = when (token) {
        "dog"  -> R.drawable.hasbro_token_dog
        "hat"  -> R.drawable.hasbro_token_hat
        "car"  -> R.drawable.hasbro_token_car
        "iron" -> R.drawable.hasbro_token_iron
        "ship" -> R.drawable.hasbro_token_ship
        "shoe" -> R.drawable.hasbro_token_shoe
        else   -> R.drawable.hasbro_token_car
    }
    private fun Player.tileBgDrawable(): Int = when (card) {
        "VERTE"  -> R.drawable.rg_card_green
        "ORANGE" -> R.drawable.rg_card_orange
        "ROSE"   -> R.drawable.rg_card_pink
        "BLEUE"  -> R.drawable.rg_card_blue
        else     -> R.drawable.rg_card_green
    }
    private fun Player.stripDrawable(): Int = when (card) {
        "ORANGE" -> R.drawable.rg_card_bg_bottom_0
        "BLEUE"  -> R.drawable.rg_card_bg_bottom_1
        "ROSE"   -> R.drawable.rg_card_bg_bottom_2
        "VERTE"  -> R.drawable.rg_card_bg_bottom_3
        else     -> R.drawable.rg_card_bg_bottom_0
    }

    private fun enterImmersiveMode() {
        // Permet au contenu d'aller sous les barres système
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun isTaken(c: Corner) = players.any { it.corner == c.key }

    // --- small helpers ---
    private fun View.fadeIn(dur: Long = 300L, end: (() -> Unit)? = null) {
        if (visibility != View.VISIBLE) {
            alpha = 0f
            visibility = View.VISIBLE
        }
        animate().alpha(1f).setDuration(dur).withEndAction { end?.invoke() }.start()
    }
    private fun View.fadeOut(dur: Long = 300L, end: (() -> Unit)? = null) {
        animate().alpha(0f).setDuration(dur).withEndAction {
            visibility = View.GONE
            end?.invoke()
        }.start()
    }
    private fun postDelayed(ms: Long, block: () -> Unit) {
        window?.decorView?.postDelayed(block, ms)
    }
    private fun normalize360(a: Float): Float {
        var r = a % 360f
        if (r < 0) r += 360f
        return r
    }
    private fun angleAt(rawX: Float, rawY: Float): Float {
        val dx = rawX - centerX
        val dy = rawY - centerY
        var deg = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
        if (deg < 0) deg += 360f
        return deg
    }

    // --- binding ---
    private fun bindViews() {
        quadInfo = findViewById(R.id.quadInfo)
        quadText = findViewById(R.id.quadText)

        tokenTapTL = findViewById(R.id.tokenTapTL)
        tokenTapTR = findViewById(R.id.tokenTapTR)
        tokenTapBL = findViewById(R.id.tokenTapBL)
        tokenTapBR = findViewById(R.id.tokenTapBR)

        tileTL = findViewById(R.id.tileTL)
        tileTR = findViewById(R.id.tileTR)
        tileBL = findViewById(R.id.tileBL)
        tileBR = findViewById(R.id.tileBR)

        tileTokenTL = findViewById(R.id.tileTokenTL)
        tileTokenTR = findViewById(R.id.tileTokenTR)
        tileTokenBL = findViewById(R.id.tileTokenBL)
        tileTokenBR = findViewById(R.id.tileTokenBR)

        tileNameTL = findViewById(R.id.tileNameTL)
        tileNameTR = findViewById(R.id.tileNameTR)
        tileNameBL = findViewById(R.id.tileNameBL)
        tileNameBR = findViewById(R.id.tileNameBR)

        tileBottomTL = findViewById(R.id.tileBottomTL)
        tileBottomTR = findViewById(R.id.tileBottomTR)
        tileBottomBL = findViewById(R.id.tileBottomBL)
        tileBottomBR = findViewById(R.id.tileBottomBR)

        tileMoneyTL = findViewById(R.id.tileMoneyTL)
        tileMoneyTR = findViewById(R.id.tileMoneyTR)
        tileMoneyBL = findViewById(R.id.tileMoneyBL)
        tileMoneyBR = findViewById(R.id.tileMoneyBR)

        arrowSpin = findViewById(R.id.arrowSpin)
    }

    private fun cornerFromKey(key: String?): Corner? = when (key) {
        Corner.TL.key -> Corner.TL
        Corner.TR.key -> Corner.TR
        Corner.BL.key -> Corner.BL
        Corner.BR.key -> Corner.BR
        else -> null
    }

    /**
     * 0° Android = vers la droite, sens horaire.
     * On pointe approximativement vers le milieu de chaque quadrant.
     * BR ~ 45°, BL ~ 135°, TL ~ 225°, TR ~ 315°.
     */
    private fun angleForCorner(c: Corner): Float = when (c) {
        Corner.BR ->  45f + ARROW_ZERO_OFFSET_DEG
        Corner.BL -> 135f + ARROW_ZERO_OFFSET_DEG
        Corner.TL -> 225f + ARROW_ZERO_OFFSET_DEG
        Corner.TR -> 315f + ARROW_ZERO_OFFSET_DEG
    }

    /** Normalise un angle en [0, 360) */
    private fun norm360(a: Float): Float {
        var x = a % 360f
        if (x < 0f) x += 360f
        return x
    }

    private fun ensureArrowView(): ImageView {
        ivArrow?.let { return it }

        val root = findViewById<ConstraintLayout>(R.id.root)
        val arrow = ImageView(this).apply {
            id = View.generateViewId()
            setImageResource(R.drawable.rgadvancearrow_half)
            alpha = 0f
            // taille raisonnable; ajuste si besoin
            layoutParams = ConstraintLayout.LayoutParams(220, 220)
            scaleType = ImageView.ScaleType.FIT_CENTER
            isClickable = false
        }
        root.addView(arrow)

        // Centrage par contraintes
        ConstraintSet().apply {
            clone(root)
            connect(arrow.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(arrow.id, ConstraintSet.END,   ConstraintSet.PARENT_ID, ConstraintSet.END)
            connect(arrow.id, ConstraintSet.TOP,   ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            connect(arrow.id, ConstraintSet.BOTTOM,ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            setHorizontalBias(arrow.id, 0.5f)
            setVerticalBias(arrow.id, 0.5f)
            applyTo(root)
        }

        ivArrow = arrow
        return arrow
    }


    private fun bindTileForCorner(corner: Corner, p: Player) {
        val (tile, tokenIv, nameTv, moneyTv, stripIv) = when (corner) {
            Corner.TL -> arrayOf(tileTL, tileTokenTL, tileNameTL, tileMoneyTL, tileBottomTL)
            Corner.TR -> arrayOf(tileTR, tileTokenTR, tileNameTR, tileMoneyTR, tileBottomTR)
            Corner.BL -> arrayOf(tileBL, tileTokenBL, tileNameBL, tileMoneyBL, tileBottomBL)
            Corner.BR -> arrayOf(tileBR, tileTokenBR, tileNameBR, tileMoneyBR, tileBottomBR)
        }
        (tile as View).apply {
            setBackgroundResource(p.tileBgDrawable())
            visibility = View.VISIBLE
            alpha = 0f
            animate().alpha(1f).setDuration(180).setInterpolator(DecelerateInterpolator()).start()
        }
        (tokenIv as ImageView).setImageResource(p.tokenDrawableNoBg())
        (nameTv as TextView).text = p.name
        (moneyTv as TextView).text = "${p.money}M"
        (stripIv as ImageView).setImageResource(p.stripDrawable())
    }

    // --- interactions ---
    private fun onCornerChosen(corner: Corner) {
        val idx = currentIndex - 1
        val p = players.getOrNull(idx) ?: return

        // 1) Mémorise le corner dans le modèle
        val updated = p.copy(corner = corner.key)
        players[idx] = updated

        // 2) Affiche immédiatement la tuile du joueur qui vient d’appuyer
        bindTileForCorner(corner, updated)

        // 3) Cache les 4 icônes de sélection
        listOf(tokenTapTL, tokenTapTR, tokenTapBL, tokenTapBR).forEach { iv ->
            if (iv.visibility == View.VISIBLE) {
                iv.animate().alpha(0f).setDuration(120).withEndAction { iv.visibility = View.GONE }.start()
            } else {
                iv.visibility = View.GONE
            }
        }

        // 4) Passe au joueur suivant ou lance la phase flèche
        val nextIndex = currentIndex + 1
        if (nextIndex <= players.size) {
            // Relance la même activité pour le joueur suivant (on conserve l’état via players.corner)
            val newIntent = intent.apply {
                setExtrasClassLoader(Player::class.java.classLoader)
                putExtra(EXTRA_CURRENT_INDEX, nextIndex)
                if (Build.VERSION.SDK_INT >= 33) {
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                } else {
                    @Suppress("DEPRECATION")
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                }
            }
            startActivity(android.content.Intent(this, PlayerChooseSideActivity::class.java).putExtras(newIntent.extras ?: Bundle()))
            finish()
        } else {
            // Tous placés → la tuile du dernier est déjà visible, on peut lancer la phase flèche
            enterSpinPhase()
        }
    }


    private fun setupRoundUI() {
        // 1) Reconstruit les tiles déjà attribués
        players.forEach { p ->
            when (p.corner) {
                Corner.TL.key -> bindTileForCorner(Corner.TL, p)
                Corner.TR.key -> bindTileForCorner(Corner.TR, p)
                Corner.BL.key -> bindTileForCorner(Corner.BL, p)
                Corner.BR.key -> bindTileForCorner(Corner.BR, p)
            }
        }

        // 2) Texte central
        val current = players.getOrNull(currentIndex - 1)
        quadText.text = current?.let { "${it.name}, choisissez le coin le plus proche de vous." }
            ?: "Choisissez le coin le plus proche de vous."

        // 3) Affiche les tokens à cliquer uniquement sur coins libres
        fun refreshToken(iv: ImageView, corner: Corner) {
            if (!isTaken(corner) && !spinDone) {
                iv.visibility = View.VISIBLE
                iv.alpha = 1f
                iv.setImageResource(current?.tokenDrawableBg() ?: R.drawable.hasbro_token_car_bg)
            } else {
                iv.visibility = View.GONE
            }
        }
        refreshToken(tokenTapTL, Corner.TL)
        refreshToken(tokenTapTR, Corner.TR)
        refreshToken(tokenTapBL, Corner.BL)
        refreshToken(tokenTapBR, Corner.BR)
    }

    private fun onSpinComplete(firstPlayerIndex: Int) {
        // On lance l'écran principal de la partie avec l'ordre de jeu
        val intent = Intent(this, GameMainMenuActivity::class.java).apply {
            putExtra(GameMainMenuActivity.EXTRA_FIRST_PLAYER_INDEX, firstPlayerIndex)
            if (Build.VERSION.SDK_INT >= 33) {
                putParcelableArrayListExtra(NavKeys.PLAYERS, players)
            } else {
                @Suppress("DEPRECATION")
                putParcelableArrayListExtra(NavKeys.PLAYERS, players)
            }
        }
        startActivity(intent)
        finish()
    }



    // --- Phase flèche ---
    private fun enterSpinPhase() {
        if (spinRunning) return
        spinRunning = true

        // 1) Choisit le gagnant maintenant (mais on n’anime PAS encore)
        val eligible = players.mapIndexedNotNull { idx, p -> if (p.corner != null) idx else null }
        winnerIndex = if (eligible.isNotEmpty()) eligible[Random.nextInt(eligible.size)] else 0
        val winner = players[winnerIndex]
        val winnerCorner = cornerFromKey(winner.corner) ?: Corner.TL
        predeterminedWinnerAngle = angleForCorner(winnerCorner)
        hasPredeterminedWinner = true
        userHasInteracted = false

        // 2) Message au centre
        quadText.animate().alpha(0f).setDuration(150).withEndAction {
            quadText.text = "Faites tourner la flèche pour savoir qui commence"
            quadText.animate().alpha(1f).setDuration(250).start()
        }.start()

        // 3) Après 3s, on AFFICHE seulement la flèche, sans la faire tourner
        quadText.postDelayed({
            val arrow = ensureArrowView()
            arrow.rotation = 0f
            arrow.animate().alpha(1f).setDuration(220).start()
            // Prépare le drag
            setupArrowTouch()
        }, 3000L)
    }



    private fun showArrow() {
        // place le pivot au centre (et récupère la position écran pour calcul d’angle)
        arrowSpin.post {
            val loc = IntArray(2)
            arrowSpin.getLocationOnScreen(loc)
            centerX = loc[0] + arrowSpin.width / 2f
            centerY = loc[1] + arrowSpin.height / 2f
            arrowSpin.pivotX = arrowSpin.width / 2f
            arrowSpin.pivotY = arrowSpin.height / 2f
        }

        arrowSpin.rotation = 0f
        arrowSpin.fadeIn(300L)

        // drag / spin
        setupArrowTouch()
    }

    private fun animateArrowToWinner(arrow: ImageView) {
        // Phase 1 : petit boost (1–2 tours) pour l’effet, depuis la rotation actuelle
        val cur = norm360(arrow.rotation)
        val boostTurns = 2 * 360f
        val boostEnd = arrow.rotation + boostTurns

        val boost = ObjectAnimator.ofFloat(arrow, "rotation", arrow.rotation, boostEnd).apply {
            duration = 1200L
            interpolator = LinearInterpolator()
        }

        boost.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                // Phase 2 : décélération vers l’angle prédéterminé
                val cur2 = norm360(arrow.rotation)
                val target = norm360(predeterminedWinnerAngle)
                var delta = target - cur2
                if (delta < 0f) delta += 360f

                val easeEnd = arrow.rotation + delta
                ObjectAnimator.ofFloat(arrow, "rotation", arrow.rotation, easeEnd).apply {
                    duration = 1400L
                    interpolator = DecelerateInterpolator(2.2f)
                    addListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {}
                        override fun onAnimationCancel(animation: Animator) {}
                        override fun onAnimationRepeat(animation: Animator) {}
                        override fun onAnimationEnd(animation: Animator) {
                            onSpinComplete(winnerIndex)
                        }
                    })
                    start()
                }
            }
        })
        boost.start()
    }


    private fun setupArrowTouch() {
        val arrow = ensureArrowView()

        // Calcule le pivot centre (utile si tu comptes faire un calcul d’angle raw → rot)
        arrow.post {
            arrow.pivotX = arrow.width / 2f
            arrow.pivotY = arrow.height / 2f
        }

        arrow.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    userHasInteracted = true
                    // dès le premier vrai geste, on masque le texte
                    if (quadText.visibility == View.VISIBLE && quadText.alpha > 0f) {
                        quadText.animate().alpha(0f).setDuration(200).start()
                    }
                    lastAngle = 0f
                    lastTime = System.nanoTime()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // rotation libre sous le doigt (si tu veux, remplace par ton calcul d’angleAt)
                    v.rotation += (ev.historySize.takeIf { it > 0 }?.let { 2f } ?: 2f)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (hasPredeterminedWinner) {
                        animateArrowToWinner(v as ImageView)
                    }
                    true
                }
                else -> false
            }
        }
    }


    private fun startSpinInertia(fromRotation: Float, startVelocity: Float) {
        // amortissement simple via ValueAnimator
        val k = 1.2f           // friction
        val durationMs = 2500L

        val anim = ValueAnimator.ofFloat(0f, durationMs / 1000f)
        anim.duration = durationMs
        anim.interpolator = LinearInterpolator()

        val rot0 = fromRotation
        val v0 = startVelocity // deg/s

        anim.addUpdateListener { va ->
            val t = (va.animatedValue as Float) // secondes
            val decay = exp(-k * t)
            val delta = v0 * t * decay
            arrowSpin.rotation = rot0 + delta
        }
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                if (quadText.visibility == View.VISIBLE) quadText.fadeOut(200L)
            }
            override fun onAnimationEnd(animation: Animator) {
                onSpinFinished(arrowSpin.rotation)
            }
        })
        anim.start()
    }

    private fun onSpinFinished(finalRotation: Float) {
        val rot = normalize360(finalRotation)

        val chosenCorner = when {
            rot >= 315f || rot < 45f  -> pickRightCorner()
            rot >= 45f  && rot < 135f -> pickBottomCorner()
            rot >= 135f && rot < 225f -> pickLeftCorner()
            else                      -> pickTopCorner()
        }

        val starter = players.firstOrNull { it.corner == chosenCorner?.key }
        // Optional : feedback rapide
        // Toast.makeText(this, "${starter?.name ?: "Quelqu’un"} commence !", Toast.LENGTH_SHORT).show()

        // Redémarre l’activité sans flèche (conserve les tiles via players.corner)
        val extras = Bundle().apply {
            putInt(EXTRA_CURRENT_INDEX, currentIndex)
            putBoolean("spin_done", true)
            if (Build.VERSION.SDK_INT >= 33) {
                putParcelableArrayList(NavKeys.PLAYERS, players)
            } else {
                @Suppress("DEPRECATION")
                putParcelableArrayList(NavKeys.PLAYERS, players)
            }
            // (optionnel) passer le nom du starter :
            // putString("starter_name", starter?.name)
        }
        val restart = android.content.Intent(this, PlayerChooseSideActivity::class.java).apply {
            putExtras(extras)
        }
        startActivity(restart)
        finish()
    }

    private fun pickTopCorner(): Corner? {
        return listOf(Corner.TR, Corner.TL).firstOrNull { isTaken(it) }
            ?: listOf(Corner.BR, Corner.BL).firstOrNull { isTaken(it) }
    }
    private fun pickBottomCorner(): Corner? {
        return listOf(Corner.BL, Corner.BR).firstOrNull { isTaken(it) }
            ?: listOf(Corner.TL, Corner.TR).firstOrNull { isTaken(it) }
    }
    private fun pickLeftCorner(): Corner? {
        return listOf(Corner.TL, Corner.BL).firstOrNull { isTaken(it) }
            ?: listOf(Corner.TR, Corner.BR).firstOrNull { isTaken(it) }
    }
    private fun pickRightCorner(): Corner? {
        return listOf(Corner.TR, Corner.BR).firstOrNull { isTaken(it) }
            ?: listOf(Corner.TL, Corner.BL).firstOrNull { isTaken(it) }
    }

    // --- lifecycle ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_choose_side)
        enterImmersiveMode()

        // IMPORTANT : classloader pour éviter BadParcelableException
        intent.setExtrasClassLoader(Player::class.java.classLoader)

        // Read players
        players = try {
            if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableArrayListExtra(NavKeys.PLAYERS, Player::class.java) ?: arrayListOf()
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur de données joueurs.", Toast.LENGTH_LONG).show()
            finish(); return
        }

        currentIndex = intent.getIntExtra(EXTRA_CURRENT_INDEX, 1)
        spinDone = intent.getBooleanExtra("spin_done", false)

        if (players.isEmpty()) {
            Toast.makeText(this, "Aucun joueur reçu.", Toast.LENGTH_LONG).show()
            finish(); return
        }

        bindViews()

        // Listeners
        tokenTapTL.setOnClickListener { onCornerChosen(Corner.TL) }
        tokenTapTR.setOnClickListener { onCornerChosen(Corner.TR) }
        tokenTapBL.setOnClickListener { onCornerChosen(Corner.BL) }
        tokenTapBR.setOnClickListener { onCornerChosen(Corner.BR) }

        setupRoundUI()

        // Si tout le monde a choisi et qu’on n’a pas encore fait le tirage → phase flèche
        if (players.all { it.corner != null } && !spinDone) {
            enterSpinPhase()
        } else if (spinDone) {
            // retour après tirage : cacher la flèche si jamais
            arrowSpin.visibility = View.GONE
            // (optionnel : afficher le starter)
            // intent.getStringExtra("starter_name")?.let { quadText.text = "$it commence !" }
        }
    }
}
