package com.nicolas.familybudget.domain.invest

import com.nicolas.familybudget.core.Money
import com.nicolas.familybudget.data.local.AccountEntity
import com.nicolas.familybudget.data.local.AccountType
import com.nicolas.familybudget.data.local.GoalEntity
import com.nicolas.familybudget.data.local.GoalType
import com.nicolas.familybudget.data.local.HouseholdProfile
import com.nicolas.familybudget.domain.model.Allocation
import com.nicolas.familybudget.domain.model.BudgetSummary
import com.nicolas.familybudget.domain.model.Horizon
import com.nicolas.familybudget.domain.model.InvestmentPlan
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Moteur de placement hors-ligne, base sur des regles transparentes adaptees au
 * contexte francais. Logique en "cascade" :
 *   0. Stabiliser : eteindre les dettes a taux eleve.
 *   1. Constituer l'epargne de precaution (Livret A / LDDS), liquide et sans risque.
 *   2. Placer le surplus restant selon l'horizon DERIVE des objectifs.
 *
 * Aucune donnee n'est envoyee sur le reseau ; chaque recommandation porte sa
 * justification pour rester auditable.
 */
class InvestmentPlannerEngine @Inject constructor() {

    fun buildPlan(
        profile: HouseholdProfile,
        summary: BudgetSummary,
        goals: List<GoalEntity>,
        accounts: List<AccountEntity>,
    ): InvestmentPlan {
        val actions = mutableListOf<String>()
        val allocations = mutableListOf<Allocation>()
        val notes = mutableListOf<String>()

        // --- Surplus mensuel investissable ---
        val monthlyIncome =
            if (summary.incomeCents > 0) summary.incomeCents else profile.monthlyNetIncomeCents
        val essentialMonthly = when {
            summary.essentialMonthlyCents > 0 -> summary.essentialMonthlyCents
            else -> profile.monthlyFixedChargesCents
        }
        val realizedExpense = summary.totalExpenseCents
        val surplus = if (realizedExpense > 0) {
            (monthlyIncome - realizedExpense)
        } else {
            (monthlyIncome - essentialMonthly)
        }.coerceAtLeast(0)

        if (monthlyIncome <= 0) {
            notes += "Renseigne tes revenus et tes charges dans Parametres pour activer le plan."
            return InvestmentPlan(0, actions, allocations, notes + FrenchProducts.DISCLAIMER)
        }

        var remaining = surplus

        // --- Etape 0 : dette a taux eleve ---
        if (profile.hasHighInterestDebt) {
            actions += "Priorite absolue : solder les credits a taux eleve (revolving, " +
                "decouvert, carte). Rembourser un credit a 15 % equivaut a un placement " +
                "garanti a 15 % \u2014 imbattable et sans risque."
            notes += "Tant qu'une dette couteuse subsiste, on n'investit pas dans des " +
                "supports risques : le rendement attendu y est inferieur au cout du credit."
        }

        // --- Etape 1 : epargne de precaution ---
        val efTarget = essentialMonthly * profile.emergencyFundMonths
        val liquid = summary.liquidSavingsCents
        val efGap = (efTarget - liquid).coerceAtLeast(0)
        if (efTarget > 0 && efGap > 0 && remaining > 0) {
            val fillMonthly = minOf(remaining, ceilDiv(efGap, 12))
            remaining -= fillMonthly
            allocations += Allocation(
                product = "Livret A / LDDS",
                monthlyAmountCents = fillMonthly,
                horizon = Horizon.SHORT,
                rationale = "Constituer une epargne de precaution de " +
                    "${profile.emergencyFundMonths} mois de charges (cible ${Money.format(efTarget)}, " +
                    "il manque ${Money.format(efGap)}). Capital disponible, garanti et net d'impot.",
                caveat = "A combler avant tout placement risque.",
            )
            if (profile.lepEligible) {
                notes += "Tu es eligible au LEP : son taux est superieur au Livret A, " +
                    "privilegie-le pour ta precaution (plafond ${Money.format(FrenchProducts.LEP_CAP_CENTS)})."
            }
        } else if (efTarget > 0) {
            actions += "Epargne de precaution OK (~${profile.emergencyFundMonths} mois couverts). " +
                "Tu peux passer a la phase placement."
        }

        // --- Etape 2 : placement du surplus restant selon l'horizon des objectifs ---
        if (remaining > 0 && !profile.hasHighInterestDebt) {
            val investGoals = goals.filter { it.type != GoalType.EMERGENCY_FUND }
            if (investGoals.isEmpty()) {
                // Pas d'objectif : allocation long terme par defaut, ponderee par le risque.
                allocations += defaultLongTermAllocations(remaining, profile)
                notes += "Aucun objectif defini : repartition long terme par defaut. " +
                    "Ajoute des objectifs (immo, etudes, retraite) pour un plan sur-mesure."
            } else {
                val weightTotal = investGoals.sumOf { goalWeight(it) }.coerceAtLeast(1)
                investGoals.sortedBy { it.priority }.forEach { goal ->
                    val share = (remaining * goalWeight(goal) / weightTotal)
                    if (share <= 0) return@forEach
                    allocations += allocateForGoal(goal, share, profile)
                }
            }
        }

        // --- Verification des plafonds ---
        checkCaps(accounts, notes)

        notes += FrenchProducts.DISCLAIMER
        return InvestmentPlan(surplus, actions, allocations, notes)
    }

    private fun allocateForGoal(goal: GoalEntity, monthly: Long, profile: HouseholdProfile): Allocation {
        val horizon = horizonFor(goal.targetDateEpochMillis)
        return when {
            goal.type == GoalType.RETIREMENT || horizon == Horizon.LONG && goal.type == GoalType.RETIREMENT ->
                Allocation(
                    product = "PER (Plan Epargne Retraite)",
                    monthlyAmountCents = monthly,
                    horizon = Horizon.LONG,
                    rationale = "Objectif \"${goal.name}\" : retraite, horizon long. Le PER offre " +
                        "une deduction fiscale des versements ; capital bloque jusqu'a la retraite " +
                        "(sauf cas de deblocage).",
                    caveat = "Epargne immobilisee : n'y mets que ce dont tu n'auras pas besoin avant.",
                )
            horizon == Horizon.SHORT -> Allocation(
                product = "Livret A / LDDS",
                monthlyAmountCents = monthly,
                horizon = Horizon.SHORT,
                rationale = "Objectif \"${goal.name}\" a echeance proche : on privilegie la " +
                    "securite et la disponibilite plutot que le rendement.",
            )
            horizon == Horizon.MEDIUM -> Allocation(
                product = "Assurance-vie (fonds euros + UC moderee)",
                monthlyAmountCents = monthly,
                horizon = Horizon.MEDIUM,
                rationale = "Objectif \"${goal.name}\" a moyen terme : l'assurance-vie permet de " +
                    "doser securite (fonds euros) et un peu de rendement (unites de compte).",
                caveat = "La part en unites de compte n'est pas garantie.",
            )
            else -> { // LONG (hors retraite) : actions via PEA + AV, pondere par le risque
                val equityShare = profile.riskAppetite.equityShareLongTerm
                val pct = (equityShare * 100).toInt()
                Allocation(
                    product = "PEA (ETF) + assurance-vie",
                    monthlyAmountCents = monthly,
                    horizon = Horizon.LONG,
                    rationale = "Objectif \"${goal.name}\" lointain : l'horizon long permet de viser " +
                        "les actions (PEA, fiscalite avantageuse apres 5 ans) a hauteur d'environ " +
                        "$pct % selon ton profil ${profile.riskAppetite.label.lowercase()}, le reste " +
                        "en assurance-vie pour lisser.",
                    caveat = "Risque de perte en capital sur la part actions ; tenir sur la duree.",
                )
            }
        }
    }

    private fun defaultLongTermAllocations(monthly: Long, profile: HouseholdProfile): List<Allocation> {
        val equityShare = profile.riskAppetite.equityShareLongTerm
        val equity = (monthly * equityShare).toLong()
        val safe = monthly - equity
        val out = mutableListOf<Allocation>()
        if (equity > 0) out += Allocation(
            product = "PEA (ETF actions monde)",
            monthlyAmountCents = equity,
            horizon = Horizon.LONG,
            rationale = "Profil ${profile.riskAppetite.label.lowercase()} : ~${(equityShare * 100).toInt()} % " +
                "en actions diversifiees pour le long terme. PEA = enveloppe fiscale avantageuse apres 5 ans.",
            caveat = "Risque de perte en capital ; investissement programme pour lisser.",
        )
        if (safe > 0) out += Allocation(
            product = "Assurance-vie (fonds euros)",
            monthlyAmountCents = safe,
            horizon = Horizon.LONG,
            rationale = "Part defensive pour amortir la volatilite et garder de la souplesse.",
        )
        return out
    }

    /** Poids d'un objectif : priorite (1 = max) + presence d'un montant cible. */
    private fun goalWeight(goal: GoalEntity): Long {
        val base = (6 - goal.priority).coerceIn(1, 5).toLong()
        return base
    }

    private fun checkCaps(accounts: List<AccountEntity>, notes: MutableList<String>) {
        fun total(type: AccountType) = accounts.filter { it.type == type }.sumOf { it.balanceCents }
        if (total(AccountType.LIVRET_A) >= FrenchProducts.LIVRET_A_CAP_CENTS)
            notes += "Livret A au plafond (${Money.format(FrenchProducts.LIVRET_A_CAP_CENTS)}) : " +
                "bascule les versements vers le LDDS puis le fonds euros."
        if (total(AccountType.LDDS) >= FrenchProducts.LDDS_CAP_CENTS)
            notes += "LDDS au plafond (${Money.format(FrenchProducts.LDDS_CAP_CENTS)})."
        if (total(AccountType.PEA) >= FrenchProducts.PEA_CAP_CENTS)
            notes += "PEA au plafond de versement (${Money.format(FrenchProducts.PEA_CAP_CENTS)}) : " +
                "envisage un compte-titres ou l'assurance-vie."
    }

    private fun horizonFor(targetDateMillis: Long?): Horizon {
        if (targetDateMillis == null) return Horizon.LONG
        val today = LocalDate.now()
        val target = java.time.Instant.ofEpochMilli(targetDateMillis)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        val years = ChronoUnit.MONTHS.between(today, target) / 12.0
        return when {
            years < 3 -> Horizon.SHORT
            years <= 8 -> Horizon.MEDIUM
            else -> Horizon.LONG
        }
    }

    private fun ceilDiv(a: Long, b: Long): Long = if (b == 0L) a else (a + b - 1) / b
}
