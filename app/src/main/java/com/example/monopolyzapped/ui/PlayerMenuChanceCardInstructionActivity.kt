package com.example.monopolyzapped.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.R
import com.example.monopolyzapped.ui.PlayerMenuChanceAnimationActivity.Companion.EXTRA_RANDOM_CHANCE_ID
import android.os.Build
import com.example.monopolyzapped.model.Player
import com.example.monopolyzapped.NavKeys

class PlayerMenuChanceCardInstructionActivity : AppCompatActivity() {

    // --- Mapping images de fond par ID de carte ---
    private val bgById: Map<Int, Int> by lazy {
        mapOf(
            1 to R.drawable.card_boardwalk_fr,
            2 to R.drawable.card_fee,
            3 to R.drawable.card_railroad,
            4 to R.drawable.card_jail,
            5 to R.drawable.card_go_fr,
            6 to R.drawable.card_leaping,
            7 to R.drawable.card_payme,
            8 to R.drawable.card_leaning,
            9 to R.drawable.card_fee,
            10 to R.drawable.card_running,
            11 to R.drawable.card_railroad,
            12 to R.drawable.card_repair,
            13 to R.drawable.card_leaping,
            14 to R.drawable.card_running,
            15 to R.drawable.card_running,
            16 to R.drawable.card_railroad,
            17 to R.drawable.card_fee,
            18 to R.drawable.card_beauty,
            19 to R.drawable.card_pointing,
            20 to R.drawable.card_repair
        )
    }

    // --- Sets pour logique bouton Suivant ---
    // Payer à la BANQUE (montant fixe connu ici) -> PlayerPayToBankCardScanActivity
    private val payBankAmountsK: Map<Int, Double> = mapOf(
        9 to 2500.0,   // ch_09: Amende pour excès de vitesse. Payez 2,5M.
        17 to 1000.0,  // ch_17: Amende. Payez 1M.
        20 to 1000.0   // ch_20: Travaux : payez 1M.
        // ch_12 = réparations par maison/hôtel -> variable -> on laisse "autres"
    )

    // Recevoir de la BANQUE -> PlayerMenuStartCardScanActivity
    private val gainBankAmountsK: Map<Int, Double> = mapOf(
        5 to 2000.0,   // ch_05: Départ + Recevez 2M
        6 to 1500.0,   // ch_06: Dividende 1,5M
        8 to 1000.0,   // ch_08: Intérêts banque 1M
        13 to 1000.0,  // ch_13: Intérêt 1M
        18 to 2500.0   // ch_18: Prix 2,5M
    )

    private var totalPlayersForPay = 0
    private var currentIndex1ForPay = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_menu_chance_card_instruction)

        val ivBg   = findViewById<ImageView>(R.id.ivChanceBg)
        val tvText = findViewById<TextView>(R.id.tvChanceText)
        val btnNext = findViewById<ImageView>(R.id.btnNext)

        // 🔧 Récup chanceId
        val chanceId = intent.getIntExtra(EXTRA_RANDOM_CHANCE_ID, -1)
        if (chanceId !in 1..20) { goToMainMenuAndFinish(-1); return }

        // 🔧 Récup joueurs + playerIndex pour préparer PayToBank
        intent.setExtrasClassLoader(Player::class.java.classLoader)
        val players: ArrayList<Player> = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(NavKeys.PLAYERS, Player::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
        }
        val pIdx0 = intent.getIntExtra(PlayerMenuActivity.EXTRA_PLAYER_INDEX, 0) // 0-based
        totalPlayersForPay = players.size
        currentIndex1ForPay = (pIdx0 + 1).coerceAtLeast(1)                      // 1-based

        // UI
        bgById[chanceId]?.let { ivBg.setImageResource(it) }
        val resName = "ch_%02d".format(chanceId)
        val strId = resources.getIdentifier(resName, "string", packageName)
        tvText.text = if (strId != 0) getString(strId) else getString(R.string.ch_title)

        btnNext.setOnClickListener {
            btnNext.isEnabled = false
            when {
                payBankAmountsK.containsKey(chanceId) -> startPayToBank(chanceId, payBankAmountsK.getValue(chanceId))
                gainBankAmountsK.containsKey(chanceId) -> startGainFromBank(chanceId, gainBankAmountsK.getValue(chanceId))
                else -> goToMainMenuAndFinish(chanceId)
            }
        }
    }

    private fun propagateAll(next: Intent, chanceId: Int) {
        next.putExtras(intent)
        if (chanceId in 1..20) next.putExtra(EXTRA_RANDOM_CHANCE_ID, chanceId)
    }

    private fun startPayToBank(chanceId: Int, amountK: Double) {
        val next = Intent(this, PlayerPayToBankCardScanActivity::class.java)
        propagateAll(next, chanceId)
        // 🔧 Fournir ce que PayToBank attend (1-based + total)
        next.putExtra(PlayerSetupActivity.EXTRA_TOTAL_PLAYERS, totalPlayersForPay)
        next.putExtra(PlayerSetupActivity.EXTRA_CURRENT_INDEX, currentIndex1ForPay)
        // 🔧 Le montant (en K)
        next.putExtra(PlayerPayToBankCardScanActivity.EXTRA_AMOUNT_K, amountK)
        startActivity(next)
        finish()
    }

    private fun startGainFromBank(chanceId: Int, amountK: Double) {
        val next = Intent(this, PlayerMenuStartCardScanActivity::class.java)
        propagateAll(next, chanceId)
        next.putExtra(PlayerMenuStartCardScanActivity.EXTRA_AMOUNT_K, amountK)
        startActivity(next)
        finish()
    }

    private fun goToMainMenuAndFinish(chanceId: Int) {
        val next = Intent(this, GameMainMenuActivity::class.java)
        propagateAll(next, chanceId)
        startActivity(next)
        finish()
    }
}