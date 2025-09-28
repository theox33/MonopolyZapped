package com.example.monopolyzapped.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.NavKeys
import com.example.monopolyzapped.R
import com.example.monopolyzapped.model.Player
import com.example.monopolyzapped.util.MoneyFormat

class PlayerMenuActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLAYER_INDEX = "player_index"
        const val EXTRA_TURN_INDEX = "turn_index"
    }

    private enum class Corner(val key: String) { TL("tl"), TR("tr"), BL("bl"), BR("br") }

    // Header
    private lateinit var headerBg: ImageView
    private lateinit var headerToken: ImageView
    private lateinit var headerName: TextView
    private lateinit var headerMoney: TextView

    // Around-grid quick buttons
    private lateinit var btnHome: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var btnEndGame: ImageButton
    private lateinit var btnHelp: ImageButton

    // Inside-grid buttons

    private lateinit var btnProprietes: ImageButton
    private lateinit var btnMaisons: ImageButton
    private lateinit var btnPayerLoyer: ImageButton
    private lateinit var btnDepart: ImageButton
    private lateinit var btnTaxeDeLuxe: ImageButton
    private lateinit var btnImpots: ImageButton
    private lateinit var btnPrison: ImageButton
    private lateinit var btnChance: ImageButton
    private lateinit var btnCaisse: ImageButton

    // State
    private var players = arrayListOf<Player>()
    private var playerIndex = 0
    private var currentTurnIndex = 0

    // --- helpers drawables ---
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

        btnHome     = findViewById(R.id.btnHome)
        btnSettings = findViewById(R.id.btnSettings)
        btnEndGame  = findViewById(R.id.btnEndGame)
        btnHelp     = findViewById(R.id.btnHelp)

        // Les 12 boutons de la grille existent aussi (ids dans le layout), on les pluggera plus tard.
        // Exemple: val btnProprietes: ImageButton = findViewById(R.id.btnProprietes)
        btnProprietes = findViewById(R.id.btnProprietes)
        btnMaisons = findViewById(R.id.btnMaisons)
        btnPayerLoyer = findViewById(R.id.btnPayerLoyer)
        btnDepart = findViewById(R.id.btnDepart)
        btnTaxeDeLuxe = findViewById(R.id.btnTaxeDeLuxe)
        btnImpots = findViewById(R.id.btnImpots)
        btnPrison = findViewById(R.id.btnSortirPrison)
        btnChance = findViewById(R.id.btnChance)
        btnCaisse = findViewById(R.id.btnCaisse)
    }

    private fun paintHeader(p: Player) {
        headerBg.setImageResource(p.headerBgDrawable())
        headerToken.setImageResource(p.tokenDrawable())
        headerName.text = p.name
        headerMoney.text = MoneyFormat.fromK(p.money.toLong())
    }

    private fun wireClicks() {
        // Retour au menu principal (son "back")
        btnHome.bindClickBackWithPressAndSound {
            val intent = Intent(this, GameMainMenuActivity::class.java).apply {
                putExtra(GameMainMenuActivity.EXTRA_FIRST_PLAYER_INDEX, currentTurnIndex)
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

        // Les autres boutons avec le son “clic” standard
        btnSettings.bindClickWithPressAndSound { toast("Paramètres (à venir)") }
        btnEndGame.bindClickWithPressAndSound { toast("Fin de partie (à venir)") }
        btnHelp.bindClickWithPressAndSound     { toast("Aide (à venir)") }

        // Si tu veux déjà sonoriser la grille centrale, exemple :
        // findViewById<ImageButton>(R.id.btnProprietes)?.bindClickWithPressAndSound { /* TODO */ }
        // ... répéter pour chaque bouton de la grille quand tu les brancheras.
        btnProprietes.bindClickWithPressAndSound {
            val intent = Intent(this, PlayerMenuPropertiesActivity::class.java).apply {
                putExtra(PlayerMenuPropertiesActivity.EXTRA_PLAYER_INDEX, playerIndex)
                putExtra(PlayerMenuPropertiesActivity.EXTRA_TURN_INDEX, currentTurnIndex)
                if (Build.VERSION.SDK_INT >= 33) {
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                } else {
                    @Suppress("DEPRECATION")
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                }
            }
            startActivity(intent)
        }

        btnMaisons.bindClickWithPressAndSound {
            val intent = Intent(this, PlayerMenuHousesActivity::class.java).apply {
                putExtra(PlayerMenuHousesActivity.EXTRA_PLAYER_INDEX, playerIndex)
                putExtra(PlayerMenuHousesActivity.EXTRA_TURN_INDEX, currentTurnIndex)
                if (Build.VERSION.SDK_INT >= 33) {
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                } else {
                    @Suppress("DEPRECATION")
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                }
            }
            startActivity(intent)
        }

        // … dans onCreate()
        btnPayerLoyer.bindClickWithPressAndSound {
            val intent = Intent(this, PlayerPayRentCardScanActivity::class.java).apply {
                // propage la nav comme d’habitude
                putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                putExtra(EXTRA_TURN_INDEX, currentTurnIndex)
            }
            startActivity(intent)
        }

        btnDepart.bindClickWithPressAndSound {
            val intent = Intent(this, PlayerMenuStartCardScanActivity::class.java).apply {
                putExtra(EXTRA_PLAYER_INDEX, playerIndex)
                putExtra(EXTRA_TURN_INDEX, currentTurnIndex)
                if (Build.VERSION.SDK_INT >= 33) {
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                } else {
                    @Suppress("DEPRECATION")
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                }
            }
            startActivity(intent)
        }

        btnTaxeDeLuxe.bindClickWithPressAndSound {
            val intent = Intent(this, PlayerPayToBankCardScanActivity::class.java).apply {
                putExtra(EXTRA_PLAYER_INDEX, playerIndex)
                putExtra(EXTRA_TURN_INDEX, currentTurnIndex)

                // ✅ send Double, and qualify the key
                val amountK = 1000.0
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


        btnImpots.bindClickWithPressAndSound {
            val intent = Intent(this, PlayerPayToBankCardScanActivity::class.java).apply {
                putExtra(PlayerMenuActivity.EXTRA_PLAYER_INDEX, playerIndex)          // optional if you refactor later
                putExtra(PlayerMenuActivity.EXTRA_TURN_INDEX, currentTurnIndex)       // optional if you refactor later

                // 🔑 use the correct key (qualify it) + send Double
                val amountK = 2000.0
                putExtra(PlayerPayToBankCardScanActivity.EXTRA_AMOUNT_K, amountK)

                // This screen still reads PlayerSetupActivity’s CURRENT_INDEX (1-based)
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

        btnPrison.bindClickWithPressAndSound {
            val intent = Intent(this, PlayerMenuJailActivity::class.java).apply {
                putExtra(PlayerMenuJailActivity.EXTRA_PLAYER_INDEX, playerIndex)
                putExtra(PlayerMenuJailActivity.EXTRA_TURN_INDEX, currentTurnIndex)
                if (Build.VERSION.SDK_INT >= 33) {
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                } else {
                    @Suppress("DEPRECATION")
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                }
            }
            startActivity(intent)
        }

        btnChance.bindClickWithPressAndSound {
            val intent = Intent(this, PlayerMenuChanceAnimationActivity::class.java).apply {
                putExtra(EXTRA_PLAYER_INDEX, playerIndex)
                putExtra(EXTRA_TURN_INDEX, currentTurnIndex)

                if (Build.VERSION.SDK_INT >= 33) {
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                } else {
                    @Suppress("DEPRECATION")
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                }
            }
            startActivity(intent)
        }

        btnCaisse.bindClickWithPressAndSound {
            val intent = Intent(this, PlayerMenuCommunityAnimationActivity::class.java).apply {
                putExtra(EXTRA_PLAYER_INDEX, playerIndex)          // 0-based
                putExtra(EXTRA_TURN_INDEX, currentTurnIndex)       // 0-based
                if (Build.VERSION.SDK_INT >= 33) {
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                } else {
                    @Suppress("DEPRECATION")
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                }
            }
            startActivity(intent)
        }

        btnEndGame.bindClickWithPressAndSound { showEndGameDialog() }
    }

    private fun showEndGameDialog() {
        val message = """
            Une partie de Monopoly se termine habituellement lorsqu'un joueur fait faillite.
            Si vous ne voulez pas attendre jusque là, vous pouvez demander d'arrêter la partie et connaître le vainqueur.

            Êtes-vous SÛR de vouloir terminer la partie maintenant ?
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Terminer la partie ?")
            .setMessage(message)
            .setNegativeButton("Annuler") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Confirmer") { _, _ ->
                // Démarre l’activité suivante (placeholder à développer plus tard)
                val intent = Intent(this, GameEndIntroActivity::class.java).apply {
                    putExtra(EXTRA_PLAYER_INDEX, playerIndex)
                    putExtra(EXTRA_TURN_INDEX, currentTurnIndex)
                    if (Build.VERSION.SDK_INT >= 33) {
                        putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                    } else {
                        @Suppress("DEPRECATION")
                        putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                    }
                }
                startActivity(intent)
            }
            .show()
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    // ---- lifecycle ----
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_menu)

        // Sécurise le classloader pour les Parcelize
        intent.setExtrasClassLoader(Player::class.java.classLoader)

        // Récupère joueurs + index
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
