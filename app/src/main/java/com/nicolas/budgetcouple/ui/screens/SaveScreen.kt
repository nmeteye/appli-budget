package com.nicolas.budgetcouple.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nicolas.budgetcouple.data.*
import com.nicolas.budgetcouple.ui.*
import com.nicolas.budgetcouple.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun SaveScreen(state: AppState, vm: BudgetViewModel) {
    val t = state.totals()
    val byCat = state.expensesByCategory().toMap()
    fun cat(c: String) = byCat[c] ?: 0.0

    val target = t.income * state.savingsGoal
    val gap = target - t.net

    val tips = buildList {
        if (cat("Abonnements") > 30)
            add("Abonnements" to "Vous payez ${eur(cat("Abonnements"))}/mois. Auditez les doublons (streaming, cloud) : viser ${eur(cat("Abonnements") * 0.6)} libère ${eur(cat("Abonnements") * 0.4)}/mois.")
        if (cat("Restaurant") > 100)
            add("Restaurant" to "${eur(cat("Restaurant"))} de restos. Limiter à 1 sortie/semaine et préparer les midis peut récupérer ~${eur(cat("Restaurant") * 0.4)}.")
        if (cat("Courses") > 350)
            add("Courses" to "Liste fixe + marques distributeur + anti-gaspi : 10–15% de gain réaliste, soit ${eur(cat("Courses") * 0.12)}/mois.")
        if (cat("Transport") > 100)
            add("Transport" to "Covoiturage interne au couple, vélo sur les trajets courts, ou forfait transport employeur (remboursé à 50%).")
        add("Automatisez" to "Programmez un virement automatique le lendemain de la paie : on n'épargne pas ce qu'on a déjà dépensé.")
        add("Règle 50/30/20" to "50% besoins, 30% envies, 20% épargne. Comparez votre répartition réelle ci-dessous.")
    }

    val needs = Categories.needs.sumOf { cat(it) }
    val wants = t.expense - needs
    val splits = listOf(
        Triple("Besoins", needs, 0.5 to Sage),
        Triple("Envies", wants, 0.3 to Ocre),
        Triple("Épargne", maxOf(t.net, 0.0), 0.2 to Brique),
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Objectif
        LedgerCard {
            Text("Objectif d'épargne du foyer", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Part des revenus à mettre de côté chaque mois.", fontSize = 14.sp, color = Muted)
            Spacer(Modifier.height(12.dp))
            Slider(
                value = state.savingsGoal.toFloat(),
                onValueChange = { vm.setSavingsGoal(it.toDouble()) },
                valueRange = 0.05f..0.5f, steps = 8,
                colors = SliderDefaults.colors(thumbColor = Sage, activeTrackColor = Sage)
            )
            FlowSpacing {
                Pill("Objectif : ${(state.savingsGoal * 100).roundToInt()}% → ${eur(target)}/mois", Muted)
                Pill("Épargne actuelle : ${eur(t.net)}", Muted)
                Pill(
                    if (gap > 0) "Manque ${eur(gap)}/mois" else "Objectif atteint (+${eur(-gap)})",
                    if (gap > 0) Brique else Sage
                )
            }
        }

        // 50/30/20
        LedgerCard {
            Text("Votre répartition vs 50/30/20", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            splits.forEach { (labelTxt, value, pair) ->
                val (ideal, col) = pair
                val realPct = if (t.income > 0) value / t.income else 0.0
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(labelTxt, fontSize = 14.sp)
                    Text("${(realPct * 100).roundToInt()}% / cible ${(ideal * 100).roundToInt()}%",
                        fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = Muted)
                }
                Spacer(Modifier.height(4.dp))
                ProgressBar(realPct.toFloat(), col, SageLite)
                Spacer(Modifier.height(12.dp))
            }
        }

        // Conseils
        tips.forEach { (title, desc) ->
            LedgerCard {
                Row {
                    Icon(Icons.Default.Lightbulb, null, tint = Ocre)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text(desc, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowSpacing(content: @Composable () -> Unit) {
    Spacer(Modifier.height(10.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}
