package com.example.monopolyzapped.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.NavKeys
import com.example.monopolyzapped.R
import com.example.monopolyzapped.model.Player
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Locale

class PlayerMenuPropertiesBuyActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLAYER_INDEX = "player_index"
        const val EXTRA_TURN_INDEX = "turn_index"

        // Résultat renvoyé
        const val EXTRA_AMOUNT_VALUE = "amount_value"     // String "500" ou "123,45"
        const val EXTRA_AMOUNT_UNIT = "amount_unit"       // "K" ou "M"
        const val EXTRA_AMOUNT_MILLIONS = "amount_millions" // Int (valeur convertie en M)
    }

    // Bandeau gauche
    private lateinit var leftInfoBanner: ImageView
    private lateinit var bannerToken: ImageView
    private lateinit var bannerName: TextView
    private lateinit var bannerMoney: TextView

    // Calculatrice
    private lateinit var calculatorScreen: TextView
    private lateinit var btnBack: ImageButton

    // Ligne C / M / K
    private lateinit var btnC: Button
    private lateinit var btnM: Button
    private lateinit var btnK: Button

    // Numpad
    private lateinit var btn0: Button
    private lateinit var btn1: Button
    private lateinit var btn2: Button
    private lateinit var btn3: Button
    private lateinit var btn4: Button
    private lateinit var btn5: Button
    private lateinit var btn6: Button
    private lateinit var btn7: Button
    private lateinit var btn8: Button
    private lateinit var btn9: Button
    private lateinit var btnComma: Button
    private lateinit var btnOk: Button

    // State joueurs
    private var players = arrayListOf<Player>()
    private var playerIndex = 0
    private var currentTurnIndex = 0

    // State calculatrice
    private var buffer = StringBuilder("0")  // nombre saisi SANS unité (peut contenir une virgule)
    private var hasComma = false
    private var selectedUnit: Char? = null   // null, 'K' ou 'M'

    private fun Player.leftInfoDrawable(): Int = when (card) {
        "ORANGE" -> R.drawable.left_info_bar_0
        "BLEUE"  -> R.drawable.left_info_bar_1
        "ROSE"   -> R.drawable.left_info_bar_2
        "VERTE"  -> R.drawable.left_info_bar_3
        else     -> R.drawable.left_info_bar_3
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_menu_properties_buy)

        // Sécurise le classloader pour Parcelize
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
        paintBanner(players[playerIndex])
        refreshScreen()
        updateOkEnabled()
        wireClicks()
    }

    private fun bindViews() {
        leftInfoBanner  = findViewById(R.id.leftInfoBanner)
        bannerToken     = findViewById(R.id.bannerToken)
        bannerName      = findViewById(R.id.bannerName)
        bannerMoney     = findViewById(R.id.bannerMoney)

        calculatorScreen = findViewById(R.id.calculatorScreen)
        btnBack          = findViewById(R.id.btnBack)

        // Ligne C / M / K
        btnC = findViewById(R.id.btnC)
        btnM = findViewById(R.id.btnM)
        btnK = findViewById(R.id.btnK)

        // Numpad
        btn0 = findViewById(R.id.btn0)
        btn1 = findViewById(R.id.btn1)
        btn2 = findViewById(R.id.btn2)
        btn3 = findViewById(R.id.btn3)
        btn4 = findViewById(R.id.btn4)
        btn5 = findViewById(R.id.btn5)
        btn6 = findViewById(R.id.btn6)
        btn7 = findViewById(R.id.btn7)
        btn8 = findViewById(R.id.btn8)
        btn9 = findViewById(R.id.btn9)
        btnComma = findViewById(R.id.btnComma)
        btnOk = findViewById(R.id.btnOk)
    }

    private fun paintBanner(p: Player) {
        leftInfoBanner.setImageResource(p.leftInfoDrawable())
        bannerToken.setImageResource(p.tokenDrawable())
        bannerName.text = p.name
        bannerMoney.text = "${p.money}M"
    }

    // Remplace intégralement cette fonction
    private fun refreshScreen() {
        val base = buffer.toString()              // valeur saisie
        val unit = selectedUnit?.toString() ?: "" // "K", "M" ou ""
        calculatorScreen.text = base + unit
    }


    private fun wireClicks() {
        btnBack.setOnClickListener { finish() }

        // C efface tout et enlève l'unité
        btnC.setOnClickListener {
            clearAll()
        }

        // Sélection exclusive de l’unité
        btnM.setOnClickListener { selectUnit('M') }
        btnK.setOnClickListener { selectUnit('K') }

        // Chiffres
        btn0.setOnClickListener { appendDigit('0') }
        btn1.setOnClickListener { appendDigit('1') }
        btn2.setOnClickListener { appendDigit('2') }
        btn3.setOnClickListener { appendDigit('3') }
        btn4.setOnClickListener { appendDigit('4') }
        btn5.setOnClickListener { appendDigit('5') }
        btn6.setOnClickListener { appendDigit('6') }
        btn7.setOnClickListener { appendDigit('7') }
        btn8.setOnClickListener { appendDigit('8') }
        btn9.setOnClickListener { appendDigit('9') }

        // Virgule
        btnComma.setOnClickListener { addComma() }

        // OK : uniquement si montant > 0 ET unité sélectionnée
        btnOk.setOnClickListener {
            if (!btnOk.isEnabled) return@setOnClickListener
            val valueStr = buffer.toString()
            val unit = selectedUnit ?: return@setOnClickListener
            val millions = toMillions(valueStr, unit)

            val data = Intent().apply {
                putExtra(EXTRA_AMOUNT_VALUE, valueStr)
                putExtra(EXTRA_AMOUNT_UNIT, unit.toString())
                putExtra(EXTRA_AMOUNT_MILLIONS, millions)
                putExtra(EXTRA_PLAYER_INDEX, playerIndex)
                putExtra(EXTRA_TURN_INDEX, currentTurnIndex)
            }
            setResult(RESULT_OK, data)
            finish()
        }
    }

    // --- Edition ---

    private fun appendDigit(d: Char) {
        // Évite les zéros de tête inutiles si pas de décimal
        if (!hasComma && buffer.toString() == "0") {
            if (d != '0') buffer.clear().append(d)
            // d == '0' -> rester sur "0"
        } else {
            buffer.append(d)
        }
        refreshScreen()
        updateOkEnabled()
    }

    private fun addComma() {
        if (!hasComma) {
            hasComma = true
            if (buffer.isEmpty()) buffer.append('0')
            buffer.append(',')
            refreshScreen()
            updateOkEnabled()
        }
    }

    private fun clearAll() {
        buffer.clear().append("0")
        hasComma = false
        selectUnit(null) // retire l’unité
        refreshScreen()
        updateOkEnabled()
    }

    // Sélectionne K ou M, exclusif ; null pour déselectionner (utilisé par C)
    // Remplace intégralement cette fonction
    private fun selectUnit(unit: Char?) {
        selectedUnit = unit
        // feedback visuel simple par alpha
        btnM.alpha = if (unit == 'M') 1.0f else 0.5f
        btnK.alpha = if (unit == 'K') 1.0f else 0.5f

        refreshScreen()    // <-- met à jour l’affichage immédiatement ("123M" / "123K")
        updateOkEnabled()  // garde la logique d’activation du bouton OK
    }


    // Active/désactive OK selon les règles métier
    private fun updateOkEnabled() {
        val amountPositive = parseToBigDecimal(buffer.toString()) > BigDecimal.ZERO
        val unitChosen = selectedUnit != null
        val enabled = amountPositive && unitChosen
        btnOk.isEnabled = enabled
        btnOk.alpha = if (enabled) 1.0f else 0.5f
    }

    // --- Parsing & conversion ---

    private fun parseToBigDecimal(text: String): BigDecimal {
        // accepte virgule FR, remplace par point
        val normalized = text.replace(',', '.')
        return normalized.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    /**
     * Convertit la valeur saisie + unité en **millions** (int, arrondi).
     * - Si unité == 'M' :  "500" => 500M
     * - Si unité == 'K' :  "500" => 0.5M -> 1M (arrondi HALF_UP)
     */
    private fun toMillions(valueStr: String, unit: Char): Int {
        val value = parseToBigDecimal(valueStr)
        val inMillions = when (unit) {
            'M' -> value
            'K' -> value.divide(BigDecimal(1000), 9, RoundingMode.HALF_UP)
            else -> BigDecimal.ZERO
        }
        val rounded = inMillions.setScale(0, RoundingMode.HALF_UP)
        val asLong = rounded.toLong()
        val clamped = asLong.coerceIn(0L, 2_000_000_000L)
        return clamped.toInt()
    }
}
