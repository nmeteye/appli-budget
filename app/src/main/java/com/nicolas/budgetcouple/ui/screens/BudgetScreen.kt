package com.nicolas.budgetcouple.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nicolas.budgetcouple.data.*
import com.nicolas.budgetcouple.ui.*
import com.nicolas.budgetcouple.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(state: AppState, vm: BudgetViewModel) {
    val t = state.totals()
    val byCat = state.expensesByCategory()
    val maxCat = byCat.maxOfOrNull { it.second } ?: 1.0

    var accId by remember { mutableStateOf(state.accounts.first().id) }
    var type by remember { mutableStateOf(TxType.EXPENSE) }
    var category by remember { mutableStateOf(Categories.expense.first()) }
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Les 3 comptes
        state.accounts.forEach { acc ->
            val accColor = Color(acc.colorArgb)
            val inc = state.transactions.filter { it.accountId == acc.id && it.type == TxType.INCOME }.sumOf { it.amount }
            val exp = state.transactions.filter { it.accountId == acc.id && it.type == TxType.EXPENSE }.sumOf { it.amount }
            LedgerCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(accColor, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(acc.name, fontSize = 17.sp)
                        Text(acc.owner.uppercase(), fontSize = 10.sp, color = Muted,
                            fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                    }
                    Text(eur(acc.balance), color = accColor, fontSize = 22.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text("+${eur(inc)} · −${eur(exp)}", fontSize = 12.sp, color = Muted,
                    fontFamily = FontFamily.Monospace)
            }
        }

        // Ajout d'opération
        LedgerCard {
            Text("Ajouter une opération", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            DropdownField("Compte", state.accounts.map { it.id to it.name }, accId) { accId = it }
            Spacer(Modifier.height(8.dp))
            DropdownField(
                "Type",
                listOf(TxType.EXPENSE.name to "Dépense", TxType.INCOME.name to "Revenu"),
                type.name
            ) {
                type = TxType.valueOf(it)
                category = if (type == TxType.INCOME) Categories.income.first() else Categories.expense.first()
            }
            Spacer(Modifier.height(8.dp))
            val cats = if (type == TxType.INCOME) Categories.income else Categories.expense
            DropdownField("Catégorie", cats.map { it to it }, category) { category = it }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = label, onValueChange = { label = it },
                label = { Text("Libellé") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Montant (€)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    vm.addTransaction(accId, type, category, label, amount.toDoubleOrNull() ?: 0.0)
                    label = ""; amount = ""
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Ink)
            ) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Ajouter")
            }
        }

        // Répartition par catégorie
        LedgerCard {
            Text("Où part l'argent", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if (byCat.isEmpty()) {
                Text("Aucune dépense enregistrée.", color = Muted, fontSize = 14.sp)
            }
            byCat.forEach { (cat, amt) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(cat, fontSize = 14.sp)
                    val pct = if (t.expense > 0) (amt / t.expense * 100).roundToInt() else 0
                    Text("${eur(amt)} · $pct%", fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(3.dp))
                ProgressBar((amt / maxCat).toFloat(), Sage, SageLite)
                Spacer(Modifier.height(10.dp))
            }
        }

        // Liste des opérations
        LedgerCard {
            Text("Opérations (${state.transactions.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            state.transactions.forEach { tx ->
                val acc = state.accounts.find { it.id == tx.accountId }
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Box(Modifier.size(8.dp).background(Color(acc?.colorArgb ?: 0xFF999999L), CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tx.label, fontSize = 14.sp)
                        Text("${tx.category} · ${acc?.name ?: "?"}", fontSize = 11.sp,
                            color = Muted, fontFamily = FontFamily.Monospace)
                    }
                    val sign = if (tx.type == TxType.INCOME) "+" else "−"
                    val col = if (tx.type == TxType.INCOME) Sage else Brique
                    Text("$sign${eur(tx.amount)}", color = col, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace)
                    IconButton(onClick = { vm.removeTransaction(tx.id) }) {
                        Icon(Icons.Default.Delete, "Supprimer", tint = Muted)
                    }
                }
                HorizontalDivider(color = LineCol)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == selected }?.second ?: ""
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel, onValueChange = {}, readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(value); expanded = false })
            }
        }
    }
}
