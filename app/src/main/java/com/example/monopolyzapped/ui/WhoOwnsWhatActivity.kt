// WhoOwnsWhatActivity.kt
package com.example.monopolyzapped.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import com.example.monopolyzapped.NavKeys
import com.example.monopolyzapped.R
import com.example.monopolyzapped.model.Player
import kotlin.math.max

class WhoOwnsWhatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TURN_INDEX = "turn_index"

        // ➜ contrat avec PlayerOwnsPropertyActivity (à implémenter plus tard)
        const val EXTRA_PROP_NAME = "prop_name"
        const val EXTRA_PROP_PRICE_K = "prop_price_k"
        const val EXTRA_HOUSE_PRICE_K = "house_price_k"
        const val RESULT_HOUSES_COUNT = "houses_count"
    }

    // --- Data ---

    private data class MonopolyProperty(
        val groupName: String,
        val name: String,
        val priceK: Double,
        val housePriceK: Double,
        val groupIndex: Int
    )

    private data class Assignment(
        val propertyIndex: Int,
        val ownerPlayerIndex: Int?, // null => banque
        val houses: Int
    )

    private var players = arrayListOf<Player>()
    private var currentTurnIndex = 0

    private val properties = mutableListOf<MonopolyProperty>()
    private var currentPropIndex = 0
    private val history = ArrayDeque<Assignment>() // pour Undo

    // --- Views ---

    private fun <T : View> v(@IdRes id: Int): T = findViewById(id)

    private val tileTL by lazy { v<View>(R.id.tileTL) }
    private val tileTR by lazy { v<View>(R.id.tileTR) }
    private val tileBL by lazy { v<View>(R.id.tileBL) }
    private val tileBR by lazy { v<View>(R.id.tileBR) }

    private val tileTokenTL by lazy { v<ImageView>(R.id.tileTokenTL) }
    private val tileTokenTR by lazy { v<ImageView>(R.id.tileTokenTR) }
    private val tileTokenBL by lazy { v<ImageView>(R.id.tileTokenBL) }
    private val tileTokenBR by lazy { v<ImageView>(R.id.tileTokenBR) }

    private val tileNameTL by lazy { v<TextView>(R.id.tileNameTL) }
    private val tileNameTR by lazy { v<TextView>(R.id.tileNameTR) }
    private val tileNameBL by lazy { v<TextView>(R.id.tileNameBL) }
    private val tileNameBR by lazy { v<TextView>(R.id.tileNameBR) }

    private val tileBottomTL by lazy { v<ImageView>(R.id.tileBottomTL) }
    private val tileBottomTR by lazy { v<ImageView>(R.id.tileBottomTR) }
    private val tileBottomBL by lazy { v<ImageView>(R.id.tileBottomBL) }
    private val tileBottomBR by lazy { v<ImageView>(R.id.tileBottomBR) }

    private val centerTitle by lazy { v<TextView>(R.id.centerTitle) }
    private val propertyName by lazy { v<TextView>(R.id.propertyName) }
    private val propertyImage by lazy { v<ImageView>(R.id.propertyImage) }
    private val btnUndo by lazy { v<View>(R.id.btnUndo) }
    private val ivBank by lazy { v<ImageView>(R.id.ivBank) }

    // --- helpers drawables (reprise de GameMainMenuActivity, sans l’argent) ---

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

    @DrawableRes
    private fun bankruptCardForGroupIndex(idx: Int): Int = when (idx) {
        0 -> R.drawable.bankrupt_property_card_0
        1 -> R.drawable.bankrupt_property_card_1
        2 -> R.drawable.bankrupt_property_card_2
        3 -> R.drawable.bankrupt_property_card_3
        4 -> R.drawable.bankrupt_property_card_4
        5 -> R.drawable.bankrupt_property_card_5
        6 -> R.drawable.bankrupt_property_card_6
        7 -> R.drawable.bankrupt_property_card_7
        else -> R.drawable.bankrupt_property_card_8
    }

    // Map “Groupe” (texte dans properties.xml) -> index 0..8
    private fun groupIndexOf(name: String): Int = when (name.trim().lowercase()) {
        "marron", "brown"              -> 0
        "bleu clair", "light blue"     -> 1
        "rose", "pink"                 -> 2
        "orange"                       -> 3
        "rouge", "red"                 -> 4
        "jaune", "yellow"              -> 5
        "vert", "verte", "green"       -> 6
        "bleu", "bleu foncé", "dark blue" -> 7
        // gares/compagnies ou autres → 8 (dernière carte générique)
        else -> 8
    }

    // --- parsing properties.xml ---


    private fun loadPropertiesFromValues() {
        try {
            val lines = resources.getStringArray(R.array.properties)
            lines.forEach { raw ->
                val parts = raw.split("|").map { it.trim() }
                if (parts.size >= 4) {
                    val gRaw = parts[0]
                    val n = parts[1]
                    val buyK = parts[2].replace(",", ".").toDoubleOrNull() ?: 0.0
                    val houseK = parts[3].replace(",", ".").toDoubleOrNull() ?: 0.0

                    // ✅ si c’est un chiffre (0..8), on le prend tel quel, sinon on mappe via groupIndexOf()
                    val gIndex = gRaw.toIntOrNull()?.coerceIn(0, 8) ?: groupIndexOf(gRaw)

                    properties += MonopolyProperty(
                        groupName = gRaw,           // (facultatif, conservé tel quel)
                        name = n,
                        priceK = buyK,
                        housePriceK = houseK,
                        groupIndex = gIndex        // ✅ bon index
                    )
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Impossible de lire res/values/properties.xml", Toast.LENGTH_LONG).show()
        }
    }


    // --- UI binding ---

    private fun bindAllTiles() {
        fun bindOne(tile: View, token: ImageView, name: TextView, strip: ImageView, p: Player?) {
            if (p == null) {
                tile.visibility = View.GONE
                tile.isClickable = false
                return
            }
            tile.setBackgroundResource(p.tileBgDrawable())
            tile.visibility = View.VISIBLE
            tile.alpha = 0f
            tile.animate().alpha(1f).setDuration(180)
                .setInterpolator(DecelerateInterpolator()).start()

            token.setImageResource(p.tokenDrawableNoBg())
            name.text = p.name
            strip.setImageResource(p.stripDrawable())
        }

        val idxTL = players.indexOfFirst { it.corner == "tl" }.takeIf { it >= 0 }
        val idxTR = players.indexOfFirst { it.corner == "tr" }.takeIf { it >= 0 }
        val idxBL = players.indexOfFirst { it.corner == "bl" }.takeIf { it >= 0 }
        val idxBR = players.indexOfFirst { it.corner == "br" }.takeIf { it >= 0 }

        bindOne(tileTL, tileTokenTL, tileNameTL, tileBottomTL, idxTL?.let { players[it] })
        bindOne(tileTR, tileTokenTR, tileNameTR, tileBottomTR, idxTR?.let { players[it] })
        bindOne(tileBL, tileTokenBL, tileNameBL, tileBottomBL, idxBL?.let { players[it] })
        bindOne(tileBR, tileTokenBR, tileNameBR, tileBottomBR, idxBR?.let { players[it] })

        // Clics : choisir le propriétaire
        tileTL.setOnClickListener { idxTL?.let { onPlayerChosen(it) } }
        tileTR.setOnClickListener { idxTR?.let { onPlayerChosen(it) } }
        tileBL.setOnClickListener { idxBL?.let { onPlayerChosen(it) } }
        tileBR.setOnClickListener { idxBR?.let { onPlayerChosen(it) } }
    }

    private fun refreshCenterForCurrentProperty() {
        if (currentPropIndex !in properties.indices) {
            goToReadyToFindOut()
            return
        }
        val prop = properties[currentPropIndex]
        centerTitle.text = "Qui possède ?"
        propertyName.text = prop.name
        propertyImage.setImageResource(bankruptCardForGroupIndex(prop.groupIndex))

        // bouton banque
        ivBank.setOnClickListener {
            // propriété à la banque → on avance
            history.addLast(Assignment(currentPropIndex, ownerPlayerIndex = null, houses = 0))
            currentPropIndex++
            refreshCenterForCurrentProperty()
        }

        // bouton undo
        btnUndo.setOnClickListener { performUndo() }
    }

    private fun performUndo() {
        val last = history.removeLastOrNull() ?: run {
            Toast.makeText(this, "Rien à annuler.", Toast.LENGTH_SHORT).show()
            return
        }

        // Si on avait attribué à un joueur avec ajout d’argent, il faut l’enlever
        val prop = properties[last.propertyIndex]
        last.ownerPlayerIndex?.let { pIdx ->
            if (pIdx in players.indices) {
                val deltaK = prop.priceK + last.houses * prop.housePriceK
                players[pIdx] = players[pIdx].copy(money = max(0.0, players[pIdx].money - deltaK).toInt())
            }
        }

        // Revenir sur cette propriété
        currentPropIndex = last.propertyIndex
        refreshCenterForCurrentProperty()
    }

    // --- Résultat de PlayerOwnsPropertyActivity ---

    private val pickHousesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Attendu : resultCode == RESULT_OK, intent with RESULT_HOUSES_COUNT (Int)
        val houses = result.data?.getIntExtra(RESULT_HOUSES_COUNT, 0) ?: 0
        val pIdx = result.data?.getIntExtra("owner_index", -1) ?: -1
        val propIdx = result.data?.getIntExtra("property_index", currentPropIndex) ?: currentPropIndex

        if (propIdx in properties.indices && pIdx in players.indices) {
            val prop = properties[propIdx]
            val deltaK = prop.priceK + houses * prop.housePriceK
            players[pIdx] = players[pIdx].copy(money = (players[pIdx].money + deltaK).toInt())

            history.addLast(Assignment(propIdx, ownerPlayerIndex = pIdx, houses = houses))

            // Passe à la suivante
            currentPropIndex = propIdx + 1
            refreshCenterForCurrentProperty()
        } else {
            // Si pas de données, on ne bouge pas et on réaffiche
            refreshCenterForCurrentProperty()
        }
    }

    private fun onPlayerChosen(ownerIndex: Int) {
        if (currentPropIndex !in properties.indices) return
        val prop = properties[currentPropIndex]

        // Lance l’activité (à coder plus tard) qui renverra le nb de maisons
        val intent = Intent(this, PlayerOwnsPropertyActivity::class.java).apply {
            putExtra(EXTRA_PROP_NAME, prop.name)
            putExtra(EXTRA_PROP_PRICE_K, prop.priceK)
            putExtra(EXTRA_HOUSE_PRICE_K, prop.housePriceK)
            putExtra("owner_index", ownerIndex)
            putExtra("property_index", currentPropIndex)
            if (Build.VERSION.SDK_INT >= 33) {
                putParcelableArrayListExtra(NavKeys.PLAYERS, players)
            } else {
                @Suppress("DEPRECATION")
                putParcelableArrayListExtra(NavKeys.PLAYERS, players)
            }
        }
        pickHousesLauncher.launch(intent)
    }

    private fun goToReadyToFindOut() {
        val intent = Intent(this, ReadyToFindOutActivity::class.java).apply {
            putExtra(EXTRA_TURN_INDEX, currentTurnIndex)
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

    // --- lifecycle ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_who_owns_what)

        intent.setExtrasClassLoader(Player::class.java.classLoader)
        currentTurnIndex = intent.getIntExtra(EXTRA_TURN_INDEX, 0)
        players = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(NavKeys.PLAYERS, Player::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Player>(NavKeys.PLAYERS) ?: arrayListOf()
        }
        if (players.isEmpty()) { Toast.makeText(this, "Aucun joueur reçu.", Toast.LENGTH_LONG).show(); finish(); return }

        // ✅ CHARGER LES PROPRIÉTÉS AVANT TOUT
        properties.clear()
        loadPropertiesFromValues()
        if (properties.isEmpty()) { Toast.makeText(this, "Aucune propriété dans res/values/properties.xml", Toast.LENGTH_LONG).show(); finish(); return }

        // ✅ (re)positionner l’index
        currentPropIndex = 0

        bindAllTiles()
        refreshCenterForCurrentProperty()
    }

}
