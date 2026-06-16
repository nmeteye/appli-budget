package com.nicolas.familybudget.data.csv

import com.nicolas.familybudget.core.Money
import com.nicolas.familybudget.data.local.TransactionEntity
import com.nicolas.familybudget.data.local.TransactionSource
import com.nicolas.familybudget.data.local.TransactionType
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Import basique de releve CSV. Formats de date acceptes : jj/MM/aaaa, aaaa-MM-jj.
 * Separateur ; ou , (auto-detecte). Colonnes attendues (ordre) : date, libelle, montant.
 * Montant negatif = depense, positif = entree (comme dans les exports bancaires FR).
 */
class CsvImporter @Inject constructor() {

    data class Result(
        val transactions: List<TransactionEntity>,
        val skippedLines: Int,
    )

    private val dateFormats = listOf(
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
    )

    fun parse(accountId: Long, content: String): Result {
        val lines = content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
        if (lines.isEmpty()) return Result(emptyList(), 0)

        val separator = if (lines.first().count { it == ';' } >= lines.first().count { it == ',' }) ';' else ','
        val out = mutableListOf<TransactionEntity>()
        var skipped = 0

        lines.forEachIndexed { index, line ->
            val cols = line.split(separator).map { it.trim().removeSurrounding("\"") }
            // Saute un eventuel en-tete.
            if (index == 0 && cols.getOrNull(2)?.let { Money.parseToCents(it) } == null) return@forEachIndexed
            if (cols.size < 3) { skipped++; return@forEachIndexed }

            val date = parseDate(cols[0])
            val amount = Money.parseToCents(cols[2])
            if (date == null || amount == null) { skipped++; return@forEachIndexed }

            out += TransactionEntity(
                accountId = accountId,
                amountCents = amount,
                dateEpochMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                label = cols[1].ifBlank { "Operation importee" },
                type = if (amount >= 0) TransactionType.INCOME else TransactionType.EXPENSE,
                source = TransactionSource.IMPORT,
            )
        }
        return Result(out, skipped)
    }

    private fun parseDate(raw: String): LocalDate? {
        for (fmt in dateFormats) {
            runCatching { return LocalDate.parse(raw, fmt) }
        }
        return null
    }
}
