package com.example.monopolyzapped.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.NavKeys
import com.example.monopolyzapped.R
import com.example.monopolyzapped.model.Player
import com.example.monopolyzapped.ui.PlayerMenuCommunityAnimationActivity.Companion.EXTRA_RANDOM_CC_ID

class PlayerMenuCommunityCardInstructionActivity : BaseFlippableActivity() {

    // Fond d’écran par carte CC (cc_01..cc_20)
    private val bgById: Map<Int, Int> by lazy {
        mapOf(
            1  to R.drawable.card_go_fr,     // cc_01
            2  to R.drawable.card_winner,   // cc_02
            3  to R.drawable.card_leaning,     // cc_03
            4  to R.drawable.card_leaping,      // cc_04
            5  to R.drawable.card_leaping,   // cc_05
            6  to R.drawable.card_fee,   // cc_06
            7  to R.drawable.card_pointing,       // cc_07
            8  to R.drawable.card_fee,   // cc_08
            9  to R.drawable.card_jail,    // cc_09
            10 to R.drawable.card_pointing,   // cc_10
            11 to R.drawable.card_present,   // cc_11
            12 to R.drawable.card_winner,    // cc_12
            13 to R.drawable.card_leaning,       // cc_13
            14 to R.drawable.card_beauty,  // cc_14
            15 to R.drawable.card_repair,   // cc_15
            16 to R.drawable.card_running  // cc_16
        )
    }

    // Payer la BANQUE (→ PlayerPayToBankCardScanActivity), montants en K
    private val payBankAmountsK: Map<Int, Double> = mapOf(
        3  to 500.0, // cc_03: 500k
        6  to 500.0, // cc_06: 500k
        8  to 1000.0 // cc_08: 1M
    )

    // Recevoir de la BANQUE (→ PlayerMenuStartCardScanActivity), montants en K
    private val gainBankAmountsK: Map<Int, Double> = mapOf(
        1  to 2000.0, // cc_01: 2M
        2  to 2000.0, // cc_02: 2M
        4  to 1000.0, // cc_04: 1M
        7  to 200.0,  // cc_07: 200k
        10 to 250.0, // cc_10: 250k
        11 to 500.0, // cc_11: 500k
        12 to 1000.0, // cc_12: 1M
        13 to 500.0, // cc_13: 500k
        14 to 100.0 // cc_14: 100k
    )

    // Pour satisfaire PlayerPayToBankCardScanActivity (contrat legacy 1-based + total)
    private var totalPlayersForPay = 0
    private var currentIndex1ForPay = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_menu_community_card_instruction)

        val ivBg   = findViewById<ImageView>(R.id.ivCcBg)
        val tvText = findViewById<TextView>(R.id.tvCcText)
        val btnNext = findViewById<ImageView>(R.id.btnNext) // ImageView cliquable

        // ID CC (1..20)
        val ccId = intent.getIntExtra(EXTRA_RANDOM_CC_ID, -1)
        if (ccId !in 1..20) { goToMainMenuAndFinish(-1); return }

        // Contexte joueurs pour calculer index 1-based & total
        intent.setExtrasClassLoader(Player::class.java.classLoader)
        val players: ArrayList<Player> = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(NavKeys.PLAYERS, Player::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
        }
        val pIdx0 = intent.getIntExtra(PlayerMenuActivity.EXTRA_PLAYER_INDEX, 0)
        totalPlayersForPay = players.size
        currentIndex1ForPay = (pIdx0 + 1).coerceAtLeast(1)

        // Fond d’écran
        bgById[ccId]?.let { ivBg.setImageResource(it) }

        // Texte (cc_01..cc_20)
        val resName = "cc_%02d".format(ccId)
        val strId = resources.getIdentifier(resName, "string", packageName)
        tvText.text = if (strId != 0) getString(strId) else getString(R.string.cc_title)

        // Suivant
        btnNext.setOnClickListener {
            btnNext.isEnabled = false
            when {
                payBankAmountsK.containsKey(ccId) -> startPayToBank(ccId, payBankAmountsK.getValue(ccId))
                gainBankAmountsK.containsKey(ccId) -> startGainFromBank(ccId, gainBankAmountsK.getValue(ccId))
                else -> goToMainMenuAndFinish(ccId)
            }
        }
    }

    /** Copie tout le contexte entrant + force ccId pour les écrans suivants. */
    private fun propagateAll(next: Intent, ccId: Int) {
        next.putExtras(intent)
        if (ccId in 1..20) next.putExtra(EXTRA_RANDOM_CC_ID, ccId)
    }

    private fun startPayToBank(ccId: Int, amountK: Double) {
        val next = Intent(this, PlayerPayToBankCardScanActivity::class.java)
        propagateAll(next, ccId)
        // Contrat legacy attendu par PayToBank
        next.putExtra(PlayerSetupActivity.EXTRA_TOTAL_PLAYERS, totalPlayersForPay)
        next.putExtra(PlayerSetupActivity.EXTRA_CURRENT_INDEX, currentIndex1ForPay)
        // Montant en K
        next.putExtra(PlayerPayToBankCardScanActivity.EXTRA_AMOUNT_K, amountK)
        startActivity(next)
        finish()
    }

    private fun startGainFromBank(ccId: Int, amountK: Double) {
        val next = Intent(this, PlayerMenuStartCardScanActivity::class.java)
        propagateAll(next, ccId)
        next.putExtra(PlayerMenuStartCardScanActivity.EXTRA_AMOUNT_K, amountK)
        startActivity(next)
        finish()
    }

    private fun goToMainMenuAndFinish(ccId: Int) {
        val next = Intent(this, GameMainMenuActivity::class.java)
        propagateAll(next, ccId)
        startActivity(next)
        finish()
    }
}
