package com.nicolas.familybudget.core

import java.text.NumberFormat
import java.util.Locale

/**
 * Tout l'argent est stocke en centimes (Long) pour eviter les erreurs d'arrondi
 * des flottants. Ces helpers convertissent et formattent en euros, locale FR.
 */
object Money {

    private val currencyFormat: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale.FRANCE)

    private val plainFormat: NumberFormat =
        NumberFormat.getNumberInstance(Locale.FRANCE).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

    /** 1234567 (centimes) -> "12 345,67 €" */
    fun format(cents: Long): String = currencyFormat.format(cents / 100.0)

    /** 1234567 (centimes) -> "12 345,67" (sans symbole) */
    fun formatPlain(cents: Long): String = plainFormat.format(cents / 100.0)

    /** Affiche un signe explicite (+/-) : utile pour les operations. */
    fun formatSigned(cents: Long): String {
        val prefix = if (cents > 0) "+" else ""
        return prefix + format(cents)
    }

    /** "12,50" ou "12.50" ou "1 250,00" -> centimes. Renvoie null si invalide. */
    fun parseToCents(input: String): Long? {
        val cleaned = input
            .trim()
            .replace(Regex("[\\s\\u00A0\\u202F]"), "")
            .replace("€", "")
            .replace(",", ".")
        if (cleaned.isEmpty()) return null
        val value = cleaned.toDoubleOrNull() ?: return null
        return Math.round(value * 100.0)
    }

    fun euros(amount: Double): Long = Math.round(amount * 100.0)
}
