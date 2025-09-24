package com.example.monopolyzapped.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.NavKeys
import com.example.monopolyzapped.R
import com.example.monopolyzapped.model.Player
import com.example.monopolyzapped.util.MoneyFormat
import java.math.BigDecimal
import java.math.RoundingMode

class PlayerMenuPropertiesBuyActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLAYER_INDEX = "player_index"
        const val EXTRA_TURN_INDEX = "turn_index"

        // Résultat renvoyé
        const val EXTRA_AMOUNT_VALUE = "amount_value"       // String saisi: "500" ou "123,45"
        const val EXTRA_AMOUNT_UNIT = "amount_unit"         // "K" ou "M"
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

    // 💰 Money du joueur en K (base de tous les calculs)
    private var playerMoneyK: Long = 0

    // State calculatrice (saisie en base K)
    private var buffer = StringBuilder("0")  // nombre saisi SANS unité (peut contenir une virgule)
    private var hasComma = false
    private var selectedUnit: Char? = null   // null, 'K' ou 'M'

    // --- Helpers drawables ---
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

    // --- Helpers conversion/contrôle (BASE = K partout) ---
    private val moneyKBD get() = BigDecimal(playerMoneyK)

    private fun parseToBigDecimal(text: String): BigDecimal {
        val normalized = text.replace(',', '.')
        return normalized.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    /**
     * Convertit la saisie en K (BigDecimal, sans arrondi).
     * - unit == null  -> base K (valeur telle quelle)
     * - unit == 'K'   -> K
     * - unit == 'M'   -> M * 1000 = K
     */
    private fun toKExactBD(text: String, unit: Char?): BigDecimal {
        val v = parseToBigDecimal(text)
        return when (unit) {
            null, 'K' -> v
            'M'       -> v.multiply(BigDecimal(1000))
            else      -> v
        }
    }

    // Pendant la saisie SANS unité: valeur (en K) <= moneyK
    private fun fitsAsBaseK(text: String): Boolean = toKExactBD(text, null) <= moneyKBD

    // Avec unité choisie: valeur (en K) <= moneyK
    private fun fitsWithUnit(text: String, unit: Char): Boolean = toKExactBD(text, unit) <= moneyKBD

    // Conversion pour l'extra en M (arrondi au plus proche)
    private fun toMillionsForResult(valueStr: String, unit: Char): Int {
        val inK = toKExactBD(valueStr, unit)
        val inM = inK.divide(BigDecimal(1000), 9, RoundingMode.HALF_UP)
        val rounded = inM.setScale(0, RoundingMode.HALF_UP)
        val asLong = rounded.toLong()
        val clamped = asLong.coerceIn(0L, 2_000_000_000L)
        return clamped.toInt()
    }


    // Compte les décimales après la virgule
    private fun decimalCount(text: String): Int {
        val i = text.indexOf(',')
        return if (i >= 0 && i + 1 < text.length) text.length - (i + 1) else 0
    }

    // Formate un BigDecimal avec virgule FR, max 3 décimales, sans zéros traînants
    private fun formatWithCommaMax3(bd: BigDecimal): String {
        val s = bd.setScale(3, RoundingMode.DOWN) // au plus 3 décimales, jamais au-dessus
            .stripTrailingZeros()
            .toPlainString()
            .replace('.', ',')
        return if (s.endsWith(",")) s.dropLast(1) else s
    }

    /**
     * Si la saisie (en base K, sans unité) atteint >= 1000 K :
     * - Convertit en M (K/1000) avec max 3 décimales (précision du K)
     * - Remplace le buffer par la valeur en M
     * - Force l’unité sur 'M'
     */
    private fun autoSwitchToMIfNeeded() {
        // Ne rien faire si déjà en 'M'
        if (selectedUnit == 'M') return

        val textK = buffer.toString()
        // Si la saisie a une virgule alors qu’on est en base K, on n’autoswitch que si la partie entière >= 1000
        // (mais le test numérique couvre tous les cas proprement)
        val k = toKExactBD(textK, null) // null = base K
        if (k >= BigDecimal(1000)) {
            val m = k.divide(BigDecimal(1000))
            val textM = formatWithCommaMax3(m)
            // Sécurité fonds (should be true car valeur équivalente)
            if (fitsWithUnit(textM, 'M')) {
                buffer.clear().append(textM)
                hasComma = textM.contains(',')
                selectedUnit = 'M'
                btnM.alpha = 1.0f
                btnK.alpha = 0.5f
            }
        }
    }



    // --- Lifecycle ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_menu_properties_buy)

        // Sécurise Parcels
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

        val player = players[playerIndex]
        playerMoneyK = player.money.toLong() // Player.money est en K
        paintBanner(player)

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
        // Affichage: si <1000K => "xxxK", sinon "xx,xxM"
        bannerMoney.text = MoneyFormat.fromK(p.money.toLong())
    }

    private fun refreshScreen() {
        val base = buffer.toString()              // valeur saisie
        val unit = selectedUnit?.toString() ?: "" // "K", "M" ou ""
        calculatorScreen.text = base + unit
    }

    private fun wireClicks() {
        btnBack.setOnClickListener { finish() }

        // C efface tout et enlève l'unité
        btnC.setOnClickListener { clearAll() }

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

        // OK : uniquement si montant > 0 ET unité sélectionnée ET montant (en K) <= moneyK
        btnOk.setOnClickListener {
            if (!btnOk.isEnabled) return@setOnClickListener
            val valueStr = buffer.toString()
            val unit = selectedUnit ?: return@setOnClickListener
            val millions = toMillionsForResult(valueStr, unit)

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
        val current = buffer.toString()

        // Si on a déjà une virgule et 3 décimales, on bloque
        if (hasComma && decimalCount(current) >= 3) return

        val next = if (!hasComma && current == "0") {
            if (d == '0') "0" else d.toString()
        } else current + d

        // Base K : ne jamais dépasser le solde en K
        if (!fitsAsBaseK(next)) return

        // Si une unité est déjà choisie, revalider avec cette unité (en K)
        selectedUnit?.let { unit ->
            if (!fitsWithUnit(next, unit)) return
        }

        buffer.clear().append(next)
        // Si la saisie en K atteint >= 1000, on force la bascule en 'M'
        autoSwitchToMIfNeeded()

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
    private fun selectUnit(unit: Char?) {
        if (unit == null) {
            selectedUnit = null
            btnM.alpha = 0.5f
            btnK.alpha = 0.5f
            refreshScreen()
            updateOkEnabled()
            return
        }

        // Règle: impossible de sélectionner K si le nombre comporte une virgule
        if (unit == 'K' && hasComma) {
            return
        }

        val text = buffer.toString()
        // La valeur en K (selon unité demandée) doit rester <= moneyK
        if (!fitsWithUnit(text, unit)) {
            return
        }

        selectedUnit = unit
        btnM.alpha = if (unit == 'M') 1.0f else 0.5f
        btnK.alpha = if (unit == 'K') 1.0f else 0.5f
        refreshScreen()
        updateOkEnabled()
    }


    // Active/désactive OK selon les règles métier
    private fun updateOkEnabled() {
        val amountPositive = parseToBigDecimal(buffer.toString()) > BigDecimal.ZERO
        val unit = selectedUnit
        val withinFunds = unit?.let { fitsWithUnit(buffer.toString(), it) } == true
        val enabled = amountPositive && unit != null && withinFunds
        btnOk.isEnabled = enabled
        btnOk.alpha = if (enabled) 1.0f else 0.5f
    }
}
