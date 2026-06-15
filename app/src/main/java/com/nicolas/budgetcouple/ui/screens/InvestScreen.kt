package com.nicolas.budgetcouple.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun InvestScreen(state: AppState) {
    val net = maxOf(state.totals().net, 0.0)
    var years by remember { mutableStateOf(10f) }
    var rate by remember { mutableStateOf(0.04f) }

    val fv = futureValue(net, years.roundToInt(), rate.toDouble())
    val invested = net * years.roundToInt() * 12

    val products = listOf(
        Quad("Livret A / LDDS", "Épargne de précaution, 100% liquide et garantie. Visez 3 à 6 mois de dépenses avant tout le reste.", "Risque nul", Sage),
        Quad("Fonds euros (assurance-vie)", "Capital garanti, rendement modéré. Bon socle après l'épargne de précaution.", "Risque faible", Sage),
        Quad("Assurance-vie en unités de compte / ETF", "Diversifié, fiscalité avantageuse après 8 ans. Pour un horizon long.", "Risque modéré", Ocre),
        Quad("PEA (ETF actions)", "Actions Europe/monde, exonération d'impôt sur les gains après 5 ans (hors prélèvements sociaux).", "Risque élevé", Brique),
        Quad("PER (plan épargne retraite)", "Versements déductibles du revenu imposable. Bloqué jusqu'à la retraite (sauf cas prévus).", "Long terme", Ocre),
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LedgerCard {
            Row {
                Icon(Icons.Default.Info, null, tint = Brique)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Ordre conseillé : 1) épargne de précaution (Livret A), 2) objectifs court terme, " +
                        "3) placement long terme diversifié. N'investissez en risqué que ce dont vous n'avez pas besoin à court terme.",
                    fontSize = 14.sp, lineHeight = 20.sp
                )
            }
        }

        LedgerCard {
            Text("Simulateur — placer ${eur(net)}/mois", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("DURÉE : ${years.roundToInt()} ans", fontSize = 11.sp, color = Muted, fontFamily = FontFamily.Monospace)
            Slider(value = years, onValueChange = { years = it }, valueRange = 1f..30f, steps = 28,
                colors = SliderDefaults.colors(thumbColor = Ocre, activeTrackColor = Ocre))
            Text("RENDEMENT : ${(rate * 100).roundToInt()}%/an", fontSize = 11.sp, color = Muted, fontFamily = FontFamily.Monospace)
            Slider(value = rate, onValueChange = { rate = it }, valueRange = 0.01f..0.08f, steps = 6,
                colors = SliderDefaults.colors(thumbColor = Ocre, activeTrackColor = Ocre))
            FlowSpacing {
                Pill("Versé : ${eur(invested)}", Muted)
                Pill("Gains : ${eur(fv - invested)}", Sage)
                Pill("Capital final : ${eur(fv)}", Ink)
            }
            Spacer(Modifier.height(10.dp))
            Text("Intérêts composés, versement mensuel constant. Hypothèse indicative, hors inflation et fiscalité.",
                fontSize = 11.sp, color = Muted, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
        }

        products.forEach { p ->
            LedgerCard {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(p.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text(p.risk.uppercase(), fontSize = 10.sp, color = p.color,
                        fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(6.dp))
                Text(p.desc, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

data class Quad(val name: String, val desc: String, val risk: String, val color: Color)
