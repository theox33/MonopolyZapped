package com.example.monopolyzapped.ui

import android.content.Intent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.NavKeys
import com.example.monopolyzapped.R
import com.example.monopolyzapped.model.Player
import com.example.monopolyzapped.util.MoneyFormat

class GameMainMenuActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FIRST_PLAYER_INDEX = "first_player_index"
    }

    private enum class Corner(val key: String) { TL("tl"), TR("tr"), BL("bl"), BR("br") }

    // -------- helpers assets --------
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

    // -------- state --------
    private var players = arrayListOf<Player>()
    private var currentTurnIndex = 0

    private fun enterImmersiveMode() {
        // Permet au contenu d'aller sous les barres système
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    // -------- safe view getter (évite les NPE silencieux) --------
    private fun <T : View> v(@IdRes id: Int, name: String): T =
        findViewById<T>(id)
            ?: throw IllegalStateException(
                "Layout 'activity_game_main_menu' ne contient pas la vue '$name' (id=${resources.getResourceEntryName(id)})"
            )

    // -------- views (lazy, résolues après setContentView) --------
    private val quadInfo by lazy { v<ImageView>(R.id.quadInfo, "quadInfo") }
    private val quadText by lazy { v<TextView>(R.id.quadText, "quadText") }

    private val tileTL by lazy { v<View>(R.id.tileTL, "tileTL") }
    private val tileTR by lazy { v<View>(R.id.tileTR, "tileTR") }
    private val tileBL by lazy { v<View>(R.id.tileBL, "tileBL") }
    private val tileBR by lazy { v<View>(R.id.tileBR, "tileBR") }

    private val tileTokenTL by lazy { v<ImageView>(R.id.tileTokenTL, "tileTokenTL") }
    private val tileTokenTR by lazy { v<ImageView>(R.id.tileTokenTR, "tileTokenTR") }
    private val tileTokenBL by lazy { v<ImageView>(R.id.tileTokenBL, "tileTokenBL") }
    private val tileTokenBR by lazy { v<ImageView>(R.id.tileTokenBR, "tileTokenBR") }

    private val tileNameTL by lazy { v<TextView>(R.id.tileNameTL, "tileNameTL") }
    private val tileNameTR by lazy { v<TextView>(R.id.tileNameTR, "tileNameTR") }
    private val tileNameBL by lazy { v<TextView>(R.id.tileNameBL, "tileNameBL") }
    private val tileNameBR by lazy { v<TextView>(R.id.tileNameBR, "tileNameBR") }

    private val tileBottomTL by lazy { v<ImageView>(R.id.tileBottomTL, "tileBottomTL") }
    private val tileBottomTR by lazy { v<ImageView>(R.id.tileBottomTR, "tileBottomTR") }
    private val tileBottomBL by lazy { v<ImageView>(R.id.tileBottomBL, "tileBottomBL") }
    private val tileBottomBR by lazy { v<ImageView>(R.id.tileBottomBR, "tileBottomBR") }

    private val tileMoneyTL by lazy { v<TextView>(R.id.tileMoneyTL, "tileMoneyTL") }
    private val tileMoneyTR by lazy { v<TextView>(R.id.tileMoneyTR, "tileMoneyTR") }
    private val tileMoneyBL by lazy { v<TextView>(R.id.tileMoneyBL, "tileMoneyBL") }
    private val tileMoneyBR by lazy { v<TextView>(R.id.tileMoneyBR, "tileMoneyBR") }

    private fun cornerFromKey(key: String?): Corner? = when (key) {
        Corner.TL.key -> Corner.TL
        Corner.TR.key -> Corner.TR
        Corner.BL.key -> Corner.BL
        Corner.BR.key -> Corner.BR
        else -> null
    }

    // -------- tiles ----------
    private fun bindTileForCorner(corner: Corner, p: Player) {
        // récupère les vues du coin demandé
        val tile: View
        val tokenIv: ImageView
        val nameTv: TextView
        val moneyTv: TextView
        val stripIv: ImageView

        when (corner) {
            Corner.TL -> { tile = tileTL; tokenIv = tileTokenTL; nameTv = tileNameTL; moneyTv = tileMoneyTL; stripIv = tileBottomTL }
            Corner.TR -> { tile = tileTR; tokenIv = tileTokenTR; nameTv = tileNameTR; moneyTv = tileMoneyTR; stripIv = tileBottomTR }
            Corner.BL -> { tile = tileBL; tokenIv = tileTokenBL; nameTv = tileNameBL; moneyTv = tileMoneyBL; stripIv = tileBottomBL }
            Corner.BR -> { tile = tileBR; tokenIv = tileTokenBR; nameTv = tileNameBR; moneyTv = tileMoneyBR; stripIv = tileBottomBR }
        }

        tile.apply {
            setBackgroundResource(p.tileBgDrawable())
            visibility = View.VISIBLE
            alpha = 0f
            animate().alpha(1f)
                .setDuration(180)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
        tokenIv.setImageResource(p.tokenDrawableNoBg())
        nameTv.text = p.name
        moneyTv.text = MoneyFormat.fromK(p.money.toLong())
        stripIv.setImageResource(p.stripDrawable())
    }

    private fun rebuildAllTiles() {
        players.forEach { p ->
            cornerFromKey(p.corner)?.let { bindTileForCorner(it, p) }
        }
    }

    // -------- centre : messages alternés --------
    private val handler = Handler(Looper.getMainLooper())
    private var msgIndex = 0
    private val rotator = object : Runnable {
        override fun run() {
            val turnName = players.getOrNull(currentTurnIndex)?.name ?: "À vous"
            val nextText = if (msgIndex % 2 == 0)
                "$turnName, lancez les dés et avancez votre pion autour du plateau"
            else
                "Appuyez sur votre coin pour le menu d'action"
            msgIndex++

            quadText.animate().alpha(0f).setDuration(180).withEndAction {
                quadText.text = nextText
                quadText.animate().alpha(1f).setDuration(220).start()
            }.start()

            handler.postDelayed(this, 5_000L)
        }
    }

    private fun startRotatingMessages() {
        handler.removeCallbacksAndMessages(null)
        msgIndex = 0
        rotator.run()
    }
    private fun stopRotatingMessages() = handler.removeCallbacksAndMessages(null)

    // -------- interactions : clic sur chaque tuile --------
    private fun setupTileClicks() {
        fun go(playerIndex: Int) {
            val intent = Intent(this, PlayerMenuActivity::class.java).apply {
                putExtra(PlayerMenuActivity.EXTRA_PLAYER_INDEX, playerIndex)
                putParcelableArrayListExtra(NavKeys.PLAYERS, players)
            }
            startActivity(intent)
        }

        // coin -> index du joueur
        val idxTL = players.indexOfFirst { it.corner == "tl" }
        val idxTR = players.indexOfFirst { it.corner == "tr" }
        val idxBL = players.indexOfFirst { it.corner == "bl" }
        val idxBR = players.indexOfFirst { it.corner == "br" }

        // N’attache le clic (avec anim + son) que si une tuile est occupée
        if (idxTL != -1) tileTL.bindClickWithPressAndSound { go(idxTL) } else tileTL.isClickable = false
        if (idxTR != -1) tileTR.bindClickWithPressAndSound { go(idxTR) } else tileTR.isClickable = false
        if (idxBL != -1) tileBL.bindClickWithPressAndSound { go(idxBL) } else tileBL.isClickable = false
        if (idxBR != -1) tileBR.bindClickWithPressAndSound { go(idxBR) } else tileBR.isClickable = false
    }



    // -------- lifecycle --------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_main_menu) // <-- ce layout doit contenir tous les IDs listés plus haut
        enterImmersiveMode()

        intent.setExtrasClassLoader(Player::class.java.classLoader)

        val firstPlayerIndex = intent.getIntExtra(EXTRA_FIRST_PLAYER_INDEX, 0)
        players = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(NavKeys.PLAYERS, Player::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
        }
        if (players.isEmpty()) {
            Toast.makeText(this, "Aucun joueur reçu.", Toast.LENGTH_LONG).show()
            finish(); return
        }

        currentTurnIndex = firstPlayerIndex.coerceIn(0, players.lastIndex)

        // Repeint toutes les tuiles choisies
        rebuildAllTiles()

        // Premier message immédiat
        quadText.alpha = 0f
        val firstName = players[currentTurnIndex].name
        quadText.text = "$firstName, lancez les dés et avancez votre pion autour du plateau"
        quadText.animate().alpha(1f).setDuration(220).start()

        setupTileClicks()
    }

    override fun onResume() {
        super.onResume()
        startRotatingMessages()
    }

    override fun onPause() {
        super.onPause()
        stopRotatingMessages()
    }
}
