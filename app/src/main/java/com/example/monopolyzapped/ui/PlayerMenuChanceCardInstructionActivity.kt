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

class PlayerMenuChanceCardInstructionActivity : BaseFlippableActivity() {

    // --- Mapping images de fond par ID de carte ---
    private val bgById: Map<Int, Int> by lazy {
        mapOf(
            1 to R.drawable.card_running,
            2 to R.drawable.card_payme,
            3 to R.drawable.card_fee,
            4 to R.drawable.card_fee,
            5 to R.drawable.card_railroad,
            6 to R.drawable.card_railroad,
            7 to R.drawable.card_boardwalk_fr,
            8 to R.drawable.card_railroad,
            9 to R.drawable.card_leaping,
            10 to R.drawable.card_running,
            11 to R.drawable.card_winner,
            12 to R.drawable.card_running,
            13 to R.drawable.card_jail,
            14 to R.drawable.card_go_fr,
            15 to R.drawable.card_leaning,
            16 to R.drawable.card_fee
        )
    }

    // --- Sets pour logique bouton Suivant ---
    // Payer à la BANQUE (montant fixe connu ici) -> PlayerPayToBankCardScanActivity
    private val payBankAmountsK: Map<Int, Double> = mapOf(
        4 to 150.0,   // ch_09: Amende pour excès de vitesse. Payez 2,5M.
    )

    // Recevoir de la BANQUE -> PlayerMenuStartCardScanActivity
    private val gainBankAmountsK: Map<Int, Double> = mapOf(
        9 to 1500.0,   // ch_05: Départ + Recevez 2M
        14 to 2000.0,   // ch_06: Dividende 1,5M
        15 to 500.0,   // ch_08: Intérêts banque 1M
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