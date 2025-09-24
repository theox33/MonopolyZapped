package com.example.monopolyzapped.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

object MoneyFormat {

    /**
     * Règle d'affichage (entrée en milliers = K):
     * - Si < 1000  -> affiche "xxxK"
     * - Sinon      -> (K / 1000) arrondi au centième + "M" (ex: 10353 -> "10,35M")
     */
    fun fromK(amountK: Long): String {
        return if (amountK < 1000L) {
            "${amountK}K"
        } else {
            val m = BigDecimal(amountK).divide(BigDecimal(1000))
                .setScale(if (amountK % 100 == 0L) 1 else 2, RoundingMode.HALF_UP)
            val str = m.stripTrailingZeros().toPlainString().replace('.', ',')
            "${str}M"
        }
    }



    /**
     * Entrée en K (K)
     * Exemple: 15000 (K) -> 15,00M
     */
    fun fromM(amountM: Long): String = fromK(amountM * 1000L)

    /**
     * Pour des montants saisis côté calculatrice: (valueStr, unit) -> affichage normalisé.
     * valueStr peut contenir une virgule FR.
     */
    fun fromUserInput(valueStr: String, unit: Char): String {
        val normalized = valueStr.replace(',', '.')
        val bd = normalized.toBigDecimalOrNull() ?: BigDecimal.ZERO
        // Convertit en K selon l'unité (M -> *1000, K -> *1)
        val asK = when (unit) {
            'M' -> bd.multiply(BigDecimal(1000))
            'K' -> bd
            else -> bd
        }
        val kLong = try {
            asK.setScale(0, RoundingMode.HALF_UP).longValueExact()
        } catch (_: ArithmeticException) {
            asK.setScale(0, RoundingMode.HALF_UP).toLong()
        }
        return fromK(kLong)
    }
}
