package com.nicolas.familybudget.domain.invest

import com.nicolas.familybudget.domain.model.SimulationResult
import javax.inject.Inject
import kotlin.math.pow

/**
 * Calculs d'interets composes. Fonctions pures, testables unitairement.
 * Tous les montants sont en centimes ; les taux sont annuels (ex. 0.025 = 2,5 %/an).
 */
class CompoundInterestSimulator @Inject constructor() {

    /** Projection : capital initial + versement mensuel, capitalises mensuellement. */
    fun project(
        initialCents: Long,
        monthlyContributionCents: Long,
        annualRate: Double,
        months: Int,
    ): SimulationResult {
        val monthlyRate = annualRate / 12.0
        var balance = initialCents.toDouble()
        val series = ArrayList<Long>(months + 1)
        series.add(balance.toLong())
        repeat(months) {
            balance = balance * (1 + monthlyRate) + monthlyContributionCents
            series.add(balance.toLong())
        }
        val future = balance.toLong()
        val contributed = initialCents + monthlyContributionCents * months
        return SimulationResult(
            futureValueCents = future,
            totalContributedCents = contributed,
            gainCents = future - contributed,
            monthlyBalances = series,
        )
    }

    /** Versement mensuel necessaire pour atteindre [targetCents] en [months] mois. */
    fun requiredMonthlyContribution(
        targetCents: Long,
        initialCents: Long,
        annualRate: Double,
        months: Int,
    ): Long {
        if (months <= 0) return targetCents - initialCents
        val r = annualRate / 12.0
        if (r == 0.0) {
            val remaining = (targetCents - initialCents).coerceAtLeast(0)
            return remaining / months
        }
        val growth = (1 + r).pow(months)
        val futureOfInitial = initialCents * growth
        val annuityFactor = (growth - 1) / r
        val needed = (targetCents - futureOfInitial) / annuityFactor
        return needed.coerceAtLeast(0.0).toLong()
    }
}
