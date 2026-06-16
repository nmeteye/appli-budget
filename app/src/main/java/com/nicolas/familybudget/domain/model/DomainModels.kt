package com.nicolas.familybudget.domain.model

import com.nicolas.familybudget.data.local.AccountType

/** Synthese budgetaire mensuelle calculee a partir des operations. */
data class BudgetSummary(
    val incomeCents: Long,
    val needsCents: Long,
    val wantsCents: Long,
    val savingsCents: Long,
    val essentialMonthlyCents: Long,
    val liquidSavingsCents: Long,
    val netWorthCents: Long,
) {
    val totalExpenseCents: Long get() = needsCents + wantsCents + savingsCents
    val cashflowCents: Long get() = incomeCents - totalExpenseCents
    val savingsRate: Double get() = if (incomeCents > 0) savingsCents.toDouble() / incomeCents else 0.0
    fun share(part: Long): Double = if (incomeCents > 0) part.toDouble() / incomeCents else 0.0
}

enum class AdviceSeverity { POSITIVE, INFO, WARNING, CRITICAL }

/**
 * Un conseil affiche dans l'onglet "Conseils". [principle] explicite le mecanisme
 * comportemental sous-jacent : l'idee est que tu puisses auditer le raisonnement,
 * pas avaler une boite noire.
 */
data class Advice(
    val id: String,
    val title: String,
    val body: String,
    val principle: String,
    val severity: AdviceSeverity,
)

/** Horizon de placement derive de la date cible d'un objectif. */
enum class Horizon(val label: String) {
    SHORT("Court terme (< 3 ans)"),
    MEDIUM("Moyen terme (3-8 ans)"),
    LONG("Long terme (> 8 ans)"),
}

/** Une recommandation d'enveloppe / produit dans le plan de placement. */
data class Allocation(
    val product: String,
    val monthlyAmountCents: Long,
    val horizon: Horizon?,
    val rationale: String,
    val caveat: String? = null,
)

data class InvestmentPlan(
    val investableSurplusCents: Long,
    val priorityActions: List<String>,
    val allocations: List<Allocation>,
    val notes: List<String>,
)

/** Resultat d'une projection d'interets composes. */
data class SimulationResult(
    val futureValueCents: Long,
    val totalContributedCents: Long,
    val gainCents: Long,
    val monthlyBalances: List<Long>,
)

data class AccountTypeShare(
    val type: AccountType,
    val totalCents: Long,
)
