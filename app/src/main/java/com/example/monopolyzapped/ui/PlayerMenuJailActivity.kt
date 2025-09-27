package com.example.monopolyzapped.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.NavKeys
import com.example.monopolyzapped.R
import com.example.monopolyzapped.model.Player
import com.example.monopolyzapped.util.MoneyFormat

class PlayerMenuJailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLAYER_INDEX = "player_index"
        const val EXTRA_TURN_INDEX = "turn_index"
    }

    // Header
    private lateinit var headerBg: ImageView
    private lateinit var headerToken: ImageView
    private lateinit var headerName: TextView
    private lateinit var headerMoney: TextView

    // Buttons
    private lateinit var jail_btn_1: ImageButton
    private lateinit var jail_btn_2: ImageButton
    private lateinit var jail_btn_3: ImageButton
    private lateinit var btnBack: ImageButton

    // State
    private var players = arrayListOf<Player>()
    private var playerIndex = 0
    private var currentTurnIndex = 0

    // --- Activity Result: retour de PlayerMenuPropertiesBuyActivity ---
    private val buyLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val data = result.data!!
            val amountValue = data.getStringExtra(PlayerMenuPropertiesBuyActivity.EXTRA_AMOUNT_VALUE) ?: "0"
            val amountUnit  = data.getStringExtra(PlayerMenuPropertiesBuyActivity.EXTRA_AMOUNT_UNIT) ?: ""
            val amountM     = data.getIntExtra(PlayerMenuPropertiesBuyActivity.EXTRA_AMOUNT_MILLIONS, 0)

            // Exemple d’usage immédiat (affichage) :
            Toast.makeText(this, "Achat validé: $amountValue$amountUnit (≈ ${amountM}M)", Toast.LENGTH_SHORT).show()

            // TODO: enchaîner vers l'activité suivante de ton flux (ex: écran de confirmation,
            //       sélection de propriété, mise à jour du joueur, etc.)
            // startActivity(Intent(this, NextActivity::class.java).apply { ... })
        }
    }

    // --- helpers drawables (mêmes règles que PlayerMenuActivity) ---
    private fun Player.headerBgDrawable(): Int = when (card) {
        "BLEUE"  -> R.drawable.header_half_blue
        "ROSE"   -> R.drawable.header_half_pink
        "ORANGE" -> R.drawable.header_half_orange
        "VERTE"  -> R.drawable.header_half_green
        else     -> R.drawable.header_half_green
    }

    private fun Player.tokenDrawable(): Int = when (token) {
        "car"  -> R.drawable.hasbro_token_car
        "ship" -> R.drawable.hasbro_token_ship
        "shoe" -> R.drawable.hasbro_token_shoe
        "hat"  -> R.drawable.hasbro_token_hat
        "iron" -> R.drawable.hasbro_token_iron
        "dog"  -> R.drawable.hasbro_token_dog
        else   -> R.drawable.hasbro_token_car
    }

    private fun bindViews() {
        headerBg    = findViewById(R.id.headerBg)
        headerToken = findViewById(R.id.headerToken)
        headerName  = findViewById(R.id.headerName)
        headerMoney = findViewById(R.id.headerMoney)

        jail_btn_1  = findViewById(R.id.jail_btn_1)
        jail_btn_2  = findViewById(R.id.jail_btn_2)
        jail_btn_3  = findViewById(R.id.jail_btn_3)
        btnBack     = findViewById(R.id.btnBack)
    }

    private fun paintHeader(p: Player) {
        headerBg.setImageResource(p.headerBgDrawable())
        headerToken.setImageResource(p.tokenDrawable())
        headerName.text = p.name
        headerMoney.text = MoneyFormat.fromK(p.money.toLong())
    }


    private fun wireClicks() {
        // Retour à l’écran précédent
        btnBack.bindClickBackWithPressAndSound { finish() }

        // Actions principales
        jail_btn_1.bindClickWithPressAndSound   { toast("Mini-jeu (à venir)") }
        jail_btn_2.bindClickWithPressAndSound {
            val intent = Intent(this, PlayerPayToBankCardScanActivity::class.java).apply {
                putExtra(PlayerMenuActivity.Companion.EXTRA_PLAYER_INDEX, playerIndex)
                putExtra(PlayerMenuActivity.Companion.EXTRA_TURN_INDEX, currentTurnIndex)

                // ✅ send Double, and qualify the key
                val amountK = 500.0
                putExtra(PlayerPayToBankCardScanActivity.EXTRA_AMOUNT_K, amountK)

                // This screen still expects PlayerSetupActivity indices (1-based)
                putExtra(PlayerSetupActivity.EXTRA_CURRENT_INDEX, playerIndex + 1)
                putExtra(PlayerSetupActivity.EXTRA_TOTAL_PLAYERS, players.size)

                if (Build.VERSION.SDK_INT >= 33) {
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                } else {
                    @Suppress("DEPRECATION")
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                }
            }
            startActivity(intent)
        }

        jail_btn_3.bindClickWithPressAndSound    { toast("(à venir)") }

    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_menu_jail)

        // Sécuriser le classloader pour Parcelize
        intent.setExtrasClassLoader(Player::class.java.classLoader)

        playerIndex = intent.getIntExtra(EXTRA_PLAYER_INDEX, 0)
        currentTurnIndex = intent.getIntExtra(EXTRA_TURN_INDEX, playerIndex)

        players = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(NavKeys.PLAYERS, Player::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
        }

        if (players.isEmpty() || playerIndex !in players.indices) {
            Toast.makeText(this, "Données joueur manquantes.", Toast.LENGTH_LONG).show()
            finish(); return
        }

        bindViews()
        paintHeader(players[playerIndex])
        wireClicks()
    }
}
