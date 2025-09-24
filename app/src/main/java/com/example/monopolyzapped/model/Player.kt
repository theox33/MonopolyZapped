package com.example.monopolyzapped.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Player(
    val name: String,          // Nom du joueur
    val token: String,         // Pion choisi
    var card: String? = null,  // Carte capacitive (assignée plus tard)
    val money: Int = 15000,      // Argent initial
    val corner: String? = null // Coin de départ (assigné plus tard : tl, tr, bl, br)
) : Parcelable