package com.nicolas.familybudget.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nicolas.familybudget.core.Money
import com.nicolas.familybudget.data.local.TransactionEntity
import com.nicolas.familybudget.data.repository.TransactionRepository
import com.nicolas.familybudget.domain.BudgetSummaryUseCase
import com.nicolas.familybudget.domain.model.BudgetSummary
import com.nicolas.familybudget.ui.components.KeyValueRow
import com.nicolas.familybudget.ui.components.SectionCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DashboardState(
    val summary: BudgetSummary = BudgetSummary(0, 0, 0, 0, 0, 0, 0),
    val recent: List<TransactionEntity> = emptyList(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    summaryUseCase: BudgetSummaryUseCase,
    transactions: TransactionRepository,
) : ViewModel() {
    val state: StateFlow<DashboardState> =
        combine(summaryUseCase.observe(), transactions.observeRecent(6)) { summary, recent ->
            DashboardState(summary, recent)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())
}

private val dateFmt = DateTimeFormatter.ofPattern("dd/MM")

private fun TransactionEntity.dateLabel(): String =
    Instant.ofEpochMilli(dateEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFmt)

@Composable
fun DashboardScreen(
    onOpenAccounts: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state.summary

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Tableau de bord", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            SectionCard {
                Text("Patrimoine net", style = MaterialTheme.typography.bodyMedium)
                Text(
                    Money.format(s.netWorthCents),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        item {
            SectionCard(title = "Ce mois-ci") {
                KeyValueRow("Revenus", Money.format(s.incomeCents))
                KeyValueRow("Depenses", Money.format(s.totalExpenseCents))
                KeyValueRow("Solde", Money.formatSigned(s.cashflowCents), emphasize = true)
                Spacer(Modifier.height(8.dp))
                val ratePct = (s.savingsRate * 100).toInt()
                KeyValueRow("Taux d'epargne", "$ratePct %", emphasize = true)
            }
        }
        if (s.totalExpenseCents > 0) {
            item { BucketBars(s) }
        }
        item {
            SectionCard(title = "Dernieres operations") {
                if (state.recent.isEmpty()) {
                    Text(
                        "Aucune operation pour l'instant.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    state.recent.forEach { tx ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(tx.label, style = MaterialTheme.typography.bodyLarge)
                                Text(tx.dateLabel(), style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                Money.formatSigned(tx.amountCents),
                                color = if (tx.amountCents >= 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
        item {
            Button(onClick = onOpenAccounts, modifier = Modifier.fillMaxWidth()) {
                Text("Gerer mes comptes et operations")
            }
        }
    }
}

@Composable
private fun BucketBars(s: BudgetSummary) {
    SectionCard(title = "Repartition 50 / 30 / 20") {
        BucketBar("Besoins", s.needsCents, s.incomeCents, 0.50)
        Spacer(Modifier.height(8.dp))
        BucketBar("Envies", s.wantsCents, s.incomeCents, 0.30)
        Spacer(Modifier.height(8.dp))
        BucketBar("Epargne / dette", s.savingsCents, s.incomeCents, 0.20)
    }
}

@Composable
private fun BucketBar(label: String, part: Long, income: Long, target: Double) {
    val share = if (income > 0) (part.toDouble() / income).coerceIn(0.0, 1.0) else 0.0
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$label  (cible ${(target * 100).toInt()} %)", style = MaterialTheme.typography.bodyMedium)
            Text("${(share * 100).toInt()} %", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { share.toFloat() },
            modifier = Modifier.fillMaxWidth().height(8.dp),
        )
    }
}
