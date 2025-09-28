package com.example.monopolyzapped.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import kotlin.math.roundToInt
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.NavKeys
import com.example.monopolyzapped.R
import com.example.monopolyzapped.model.Player
import com.example.monopolyzapped.util.MoneyFormat
import kotlin.math.max

class PlayerOwnsPropertyActivity : AppCompatActivity() {

    companion object {
        // Reçus depuis WhoOwnsWhatActivity
        const val EXTRA_PROP_NAME       = "prop_name"
        const val EXTRA_PROP_PRICE_K    = "prop_price_k"
        const val EXTRA_HOUSE_PRICE_K   = "house_price_k"
        const val EXTRA_OWNER_INDEX     = "owner_index"
        const val EXTRA_PROPERTY_INDEX  = "property_index"
        const val EXTRA_GROUP_INDEX     = "group_index" // ✅ nouveau

        // Renvoyés
        const val RESULT_HOUSES_COUNT   = "houses_count"
        const val RESULT_ADDED_AMOUNT_K = "added_amount_k"
    }

    // Views
    private lateinit var headerBg: ImageView
    private lateinit var headerToken: ImageView
    private lateinit var headerName: TextView
    private lateinit var headerMoney: TextView

    private lateinit var propertyImage: ImageView
    private lateinit var propertyName: TextView
    private lateinit var propertyAndHousesValue: TextView

    private lateinit var btn0: ImageButton
    private lateinit var btn1: ImageButton
    private lateinit var btn2: ImageButton
    private lateinit var btn3: ImageButton
    private lateinit var btn4: ImageButton
    private lateinit var btn5: ImageButton
    private lateinit var btnMortgage: ImageButton
    private lateinit var btnBack2: ImageView

    // State
    private var players = arrayListOf<Player>()
    private var ownerIndex = -1
    private var propertyIndex = -1
    private var groupIndex = 0

    private var propName: String = ""
    private var propPriceK: Double = 0.0
    private var housePriceK: Double = 0.0

    private var selectedHouses = 0
    private var mortgaged = false
    private var addedAmountK = 0.0

    // ----- helpers -----
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

    private fun hlCardForGroup(idx: Int): Int = when (idx.coerceIn(0, 8)) {
        0 -> R.drawable.hl_card0
        1 -> R.drawable.hl_card1
        2 -> R.drawable.hl_card2
        3 -> R.drawable.hl_card3
        4 -> R.drawable.hl_card4
        5 -> R.drawable.hl_card5
        6 -> R.drawable.hl_card6
        7 -> R.drawable.hl_card7
        else -> R.drawable.hl_card8
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_owns_property)

        // bind
        headerBg = findViewById(R.id.headerBg)
        headerToken = findViewById(R.id.headerToken)
        headerName = findViewById(R.id.headerName)
        headerMoney = findViewById(R.id.headerMoney)

        propertyImage = findViewById(R.id.propertyImage)
        propertyName = findViewById(R.id.propertyName)
        propertyAndHousesValue = findViewById(R.id.propertyAndHousesValue)

        btn0 = findViewById(R.id.btn0)
        btn1 = findViewById(R.id.btn1)
        btn2 = findViewById(R.id.btn2)
        btn3 = findViewById(R.id.btn3)
        btn4 = findViewById(R.id.btn4)
        btn5 = findViewById(R.id.btn5)
        btnMortgage = findViewById(R.id.btnMortgaged)
        btnBack2 = findViewById(R.id.btnBack2)

        // extras
        intent.setExtrasClassLoader(Player::class.java.classLoader)
        propName      = intent.getStringExtra(EXTRA_PROP_NAME) ?: ""
        propPriceK    = intent.getDoubleExtra(EXTRA_PROP_PRICE_K, 0.0)
        housePriceK   = intent.getDoubleExtra(EXTRA_HOUSE_PRICE_K, 0.0)
        ownerIndex    = intent.getIntExtra(EXTRA_OWNER_INDEX, -1)
        propertyIndex = intent.getIntExtra(EXTRA_PROPERTY_INDEX, -1)
        groupIndex    = intent.getIntExtra(EXTRA_GROUP_INDEX, 0)

        players = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(NavKeys.PLAYERS, Player::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
        }

        // header joueur
        val owner = players.getOrNull(ownerIndex)
        if (owner != null) {
            headerBg.setImageResource(owner.headerBgDrawable())
            headerToken.setImageResource(owner.tokenDrawable())
            headerName.text = owner.name
            headerMoney.text = MoneyFormat.fromK(owner.money.toLong())
        }

        // propriété
        propertyName.text = propName
        propertyImage.setImageResource(hlCardForGroup(groupIndex)) // ✅ dynamique

        // interactions
        btnBack2.setOnClickListener { setResult(RESULT_CANCELED); finish() }

        btn0.setOnClickListener { chooseAndApply(0, mortgaged = false) }
        btn1.setOnClickListener { chooseAndApply(1, mortgaged = false) }
        btn2.setOnClickListener { chooseAndApply(2, mortgaged = false) }
        btn3.setOnClickListener { chooseAndApply(3, mortgaged = false) }
        btn4.setOnClickListener { chooseAndApply(4, mortgaged = false) }
        btn5.setOnClickListener { chooseAndApply(5, mortgaged = false) }
        btnMortgage.setOnClickListener { chooseAndApply(0, mortgaged = true) }

        // affichage initial
        updatePreviewAndHeader(0.0, 0)
    }

    private fun chooseAndApply(houses: Int, mortgaged: Boolean) {
        selectedHouses = houses.coerceIn(0, 5)
        this.mortgaged = mortgaged

        // calcul du montant ajouté
        val base = if (mortgaged) propPriceK / 2.0 else propPriceK
        val gain = base + selectedHouses * housePriceK
        addedAmountK = gain

        // met à jour le joueur (pour de vrai)
        val owner = players.getOrNull(ownerIndex)
        if (owner != null) {
            val currentK = owner.money.toDouble()
            val newMoneyK = (currentK + gain).coerceAtLeast(0.0)
            players[ownerIndex] = owner.copy(money = newMoneyK.roundToInt())
            updatePreviewAndHeader(gain, selectedHouses)

            // Met à jour l’affichage avec la valeur réellement stockée (Int)
            headerMoney.text = MoneyFormat.fromK(players[ownerIndex].money.toLong())
        }

        // après 4s, renvoyer résultat + players mis à jour
        Handler(Looper.getMainLooper()).postDelayed({
            val result = Intent().apply {
                putExtra(RESULT_HOUSES_COUNT, selectedHouses)
                putExtra(RESULT_ADDED_AMOUNT_K, addedAmountK)
                putExtra("owner_index", ownerIndex)
                putExtra("property_index", propertyIndex)
                if (Build.VERSION.SDK_INT >= 33) {
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                } else {
                    @Suppress("DEPRECATION")
                    putParcelableArrayListExtra(NavKeys.PLAYERS, players)
                }
            }
            setResult(RESULT_OK, result)
            finish()
        }, 1000L)
    }

    private fun updatePreviewAndHeader(gainK: Double, houses: Int) {
        // "+XM" dans la vue
        val plus = MoneyFormat.fromK(gainK.toLong())
        propertyAndHousesValue.text = if (plus.startsWith("+")) plus else "+$plus"

        // met à jour visuellement le header
        players.getOrNull(ownerIndex)?.let { p ->
            headerMoney.text = MoneyFormat.fromK(p.money.toLong())
        }
    }
}
