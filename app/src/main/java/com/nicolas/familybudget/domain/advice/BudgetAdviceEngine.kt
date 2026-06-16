package com.nicolas.familybudget.domain.advice

import com.nicolas.familybudget.core.Money
import com.nicolas.familybudget.data.local.FamilyMemberEntity
import com.nicolas.familybudget.data.local.GoalEntity
import com.nicolas.familybudget.data.local.GoalType
import com.nicolas.familybudget.data.local.HouseholdProfile
import com.nicolas.familybudget.data.local.MemberRole
import com.nicolas.familybudget.domain.model.Advice
import com.nicolas.familybudget.domain.model.AdviceSeverity
import com.nicolas.familybudget.domain.model.BudgetSummary
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Conseils budgetaires fondes sur des mecanismes de finance comportementale documentes
 * (et non sur du "neuro" marketing). Chaque conseil expose le principe qui le motive,
 * de sorte que le raisonnement reste verifiable.
 */
class BudgetAdviceEngine @Inject constructor() {

    fun generate(
        profile: HouseholdProfile,
        summary: BudgetSummary,
        goals: List<GoalEntity>,
        members: List<FamilyMemberEntity>,
    ): List<Advice> {
        val out = mutableListOf<Advice>()

        val income = if (summary.incomeCents > 0) summary.incomeCents else profile.monthlyNetIncomeCents
        if (income <= 0) {
            out += Advice(
                id = "no-income",
                title = "Commence par poser tes chiffres",
                body = "Renseigne tes revenus et tes charges fixes dans Parametres, et saisis " +
                    "quelques operations. Les conseils s'affineront automatiquement.",
                principle = "Effet de simple mesure : le seul fait de suivre ses depenses tend " +
                    "deja a les reduire, car l'attention rend les arbitrages conscients.",
                severity = AdviceSeverity.INFO,
            )
            return out
        }

        // 1. Cashflow
        if (summary.totalExpenseCents > 0 && summary.cashflowCents < 0) {
            out += Advice(
                id = "negative-cashflow",
                title = "Tu depenses plus que tu ne gagnes ce mois-ci",
                body = "Solde du mois : ${Money.formatSigned(summary.cashflowCents)}. Cible une " +
                    "categorie \"envies\" a reduire en priorite plutot que de rogner sur l'essentiel.",
                principle = "Douleur du paiement : rendre la depense plus visible (especes, " +
                    "plafond d'enveloppe) reactive le frein psychologique emousse par la carte.",
                severity = AdviceSeverity.CRITICAL,
            )
        }

        // 2. Regle 50/30/20 (uniquement si on a des depenses categorisees)
        if (summary.totalExpenseCents > 0) {
            val needsPct = (summary.share(summary.needsCents) * 100).roundToInt()
            val wantsPct = (summary.share(summary.wantsCents) * 100).roundToInt()
            val savePct = (summary.savingsRate * 100).roundToInt()
            when {
                savePct >= 20 -> out += Advice(
                    "rule-savings-good", "Beau taux d'epargne ($savePct %)",
                    "Tu es au-dessus du repere de 20 %. Verrouille cet acquis en automatisant le " +
                        "virement, pour ne pas dependre de ta volonte chaque mois.",
                    "Comptabilite mentale : separer physiquement l'epargne (compte dedie) la rend " +
                        "moins disponible a la depense impulsive.",
                    AdviceSeverity.POSITIVE,
                )
                savePct in 1..19 -> out += Advice(
                    "rule-savings-low", "Epargne sous le repere ($savePct % vs 20 %)",
                    "Vise progressivement 20 % du revenu vers l'epargne/dette. Augmente le virement " +
                        "de 1 point par mois plutot que d'un coup.",
                    "Repere 50/30/20 + escalade progressive : de petits paliers contournent " +
                        "l'aversion au changement.",
                    AdviceSeverity.WARNING,
                )
                else -> out += Advice(
                    "rule-no-savings", "Aucune epargne detectee ce mois",
                    "Mets en place un virement automatique, meme modeste, le jour de la paie.",
                    "\"Payez-vous d'abord\" : automatiser avant de pouvoir depenser neutralise le " +
                        "biais du present (preferer la gratification immediate).",
                    AdviceSeverity.WARNING,
                )
            }
            if (needsPct > 55) out += Advice(
                "rule-needs-high", "Charges essentielles elevees ($needsPct %)",
                "Au-dela de ~50 %, la marge de manoeuvre se reduit. Les postes a fort levier sont " +
                    "le logement, les assurances et l'energie \u2014 a renegocier une fois par an.",
                "Effet d'ancrage : on garde des contrats par inertie ; une revue annuelle planifiee " +
                    "casse l'ancrage au tarif initial.",
                AdviceSeverity.INFO,
            )
            if (wantsPct > 35) out += Advice(
                "rule-wants-high", "Poste \"envies\" important ($wantsPct %)",
                "Pas un probleme en soi si l'epargne suit. Sinon, cible les abonnements dormants : " +
                    "additionnes, les petits prelevements pesent lourd.",
                "Biais des petits montants : chaque abonnement parait negligeable isole, alors que " +
                    "le cumul mensuel est significatif.",
                AdviceSeverity.INFO,
            )
        }

        // 3. Epargne de precaution
        if (summary.essentialMonthlyCents > 0) {
            val months = summary.liquidSavingsCents.toDouble() / summary.essentialMonthlyCents
            val rounded = (months * 10).roundToInt() / 10.0
            when {
                months < 1 -> out += Advice(
                    "ef-critical", "Matelas de securite tres faible (~$rounded mois)",
                    "Priorise un coussin de ${profile.emergencyFundMonths} mois de charges sur " +
                        "Livret A/LDDS avant tout placement. Cela evite le credit subi en cas d'imprevu.",
                    "Aversion a la perte : un imprevu finance a credit coute bien plus que le " +
                        "rendement perdu en gardant des liquidites disponibles.",
                    AdviceSeverity.CRITICAL,
                )
                months < profile.emergencyFundMonths -> out += Advice(
                    "ef-build", "Continue de constituer ta precaution (~$rounded mois)",
                    "Encore un effort pour atteindre ${profile.emergencyFundMonths} mois. Vois le " +
                        "plan de l'onglet Placement pour le montant mensuel suggere.",
                    "Objectif intermediaire concret : un cap chiffre et date soutient la motivation " +
                        "mieux qu'une intention vague.",
                    AdviceSeverity.INFO,
                )
                else -> out += Advice(
                    "ef-ok", "Epargne de precaution solide (~$rounded mois)",
                    "Tu peux desormais orienter le surplus vers des placements a horizon plus long.",
                    "Sequencement : securiser le court terme libere mentalement pour prendre un " +
                        "risque mesure sur le long terme.",
                    AdviceSeverity.POSITIVE,
                )
            }
        }

        // 4. Objectifs
        if (goals.isEmpty()) {
            out += Advice(
                "no-goals", "Donne une destination a ton epargne",
                "Definis 1 a 3 objectifs (precaution, projet immo, etudes des enfants, retraite). " +
                    "Un euro \"pour la retraite\" se depense moins facilement qu'un euro anonyme.",
                "Comptabilite mentale + intentions d'implementation : nommer et dater un objectif " +
                    "augmente nettement la probabilite de s'y tenir.",
                AdviceSeverity.INFO,
            )
        }

        // 5. Enfants -> anticipation des etudes
        val children = members.filter { it.role == MemberRole.CHILD }
        val hasEducationGoal = goals.any { it.type == GoalType.EDUCATION }
        if (children.isNotEmpty() && !hasEducationGoal) {
            out += Advice(
                "kids-education", "Anticipe le cout des etudes (${children.size} enfant(s))",
                "Meme un petit versement mensuel demarre tot profite a plein des interets composes. " +
                    "Ouvre un objectif \"Etudes\" avec une echeance par enfant.",
                "Biais du present : les echeances lointaines sont sous-evaluees ; les rendre " +
                    "visibles et automatiques compense ce biais.",
                AdviceSeverity.INFO,
            )
        }

        // 6. Dette a taux eleve
        if (profile.hasHighInterestDebt) {
            out += Advice(
                "debt-first", "Cible d'abord la dette couteuse",
                "Rembourser un credit a taux eleve est le \"placement\" le plus rentable et sans " +
                    "risque. Methode boule de neige (plus petit solde d'abord) pour l'elan, ou " +
                    "avalanche (taux le plus haut d'abord) pour le cout optimal.",
                "Effet d'elan (boule de neige) : les premieres victoires rapides entretiennent la " +
                    "motivation, parfois plus efficaces que l'optimum purement mathematique.",
                AdviceSeverity.WARNING,
            )
        }

        val order = listOf(
            AdviceSeverity.CRITICAL, AdviceSeverity.WARNING,
            AdviceSeverity.INFO, AdviceSeverity.POSITIVE,
        )
        return out.sortedBy { order.indexOf(it.severity) }
    }
}
