package com.nicolas.familybudget.ui.invest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nicolas.familybudget.core.Money
import com.nicolas.familybudget.data.local.AccountEntity
import com.nicolas.familybudget.data.local.GoalEntity
import com.nicolas.familybudget.data.local.HouseholdProfile
import com.nicolas.familybudget.data.local.SettingsRepository
import com.nicolas.familybudget.data.repository.AccountRepository
import com.nicolas.familybudget.data.repository.GoalRepository
import com.nicolas.familybudget.domain.BudgetSummaryUseCase
import com.nicolas.familybudget.domain.invest.CompoundInterestSimulator
import com.nicolas.familybudget.domain.invest.InvestmentPlannerEngine
import com.nicolas.familybudget.domain.model.Allocation
import com.nicolas.familybudget.domain.model.BudgetSummary
import com.nicolas.familybudget.domain.model.InvestmentPlan
import com.nicolas.familybudget.domain.model.SimulationResult
import com.nicolas.familybudget.ui.components.KeyValueRow
import com.nicolas.familybudget.ui.components.SectionCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class InvestViewModel @Inject constructor(
    summaryUseCase: BudgetSummaryUseCase,
    settings: SettingsRepository,
    goals: GoalRepository,
    accounts: AccountRepository,
    private val planner: InvestmentPlannerEngine,
    private val simulator: CompoundInterestSimulator,
) : ViewModel() {

    val plan: StateFlow<InvestmentPlan> = combine(
        summaryUseCase.observe(),
        settings.profile,
        goals.observeAll(),
        accounts.observeActive(),
    ) { summary: BudgetSummary, profile: HouseholdProfile, g: List<GoalEntity>, a: List<AccountEntity> ->
        planner.buildPlan(profile, summary, g, a)
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000),
        InvestmentPlan(0, emptyList(), emptyList(), emptyList()),
    )

    fun simulate(initialCents: Long, monthlyCents: Long, annualRatePct: Double, years: Int): SimulationResult =
        simulator.project(initialCents, monthlyCents, annualRatePct / 100.0, years * 12)
}

@Composable
fun InvestScreen(viewModel: InvestViewModel = hiltViewModel()) {
    val plan by viewModel.plan.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Placement", style = MaterialTheme.typography.headlineSmall) }

        item {
            SectionCard {
                Text("Surplus mensuel investissable", style = MaterialTheme.typography.bodyMedium)
                Text(
                    Money.format(plan.investableSurplusCents),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "= revenus - depenses (ou - charges fixes si pas encore d'operations).",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (plan.priorityActions.isNotEmpty()) {
            item {
                SectionCard(title = "A faire en priorite") {
                    plan.priorityActions.forEach { Text("\u2022 $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp)) }
                }
            }
        }

        if (plan.allocations.isNotEmpty()) {
            item { Text("Repartition suggeree", style = MaterialTheme.typography.titleMedium) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    plan.allocations.forEach { alloc -> AllocationCard(alloc) }
                }
            }
        }

        if (plan.notes.isNotEmpty()) {
            item {
                SectionCard(title = "Notes") {
                    plan.notes.forEach {
                        Text("\u2022 $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp))
                    }
                }
            }
        }

        item { SimulatorCard(viewModel) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun AllocationCard(alloc: Allocation) {
    SectionCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(alloc.product, style = MaterialTheme.typography.titleMedium)
            Text(
                "${Money.format(alloc.monthlyAmountCents)} / mois",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        alloc.horizon?.let {
            Text(it.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        }
        Spacer(Modifier.height(6.dp))
        Text(alloc.rationale, style = MaterialTheme.typography.bodyMedium)
        alloc.caveat?.let {
            Spacer(Modifier.height(4.dp))
            Text("\u26a0 $it", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
        }
    }
}

@Composable
private fun SimulatorCard(viewModel: InvestViewModel) {
    var initial by remember { mutableStateOf("1000") }
    var monthly by remember { mutableStateOf("150") }
    var rate by remember { mutableStateOf("2.5") }
    var years by remember { mutableStateOf("10") }
    var result by remember { mutableStateOf<SimulationResult?>(null) }

    SectionCard(title = "Simulateur d'interets composes") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = initial, onValueChange = { initial = it },
                label = { Text("Capital (€)") }, singleLine = true, modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = monthly, onValueChange = { monthly = it },
                label = { Text("Mensuel (€)") }, singleLine = true, modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = rate, onValueChange = { rate = it },
                label = { Text("Taux %/an") }, singleLine = true, modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = years, onValueChange = { years = it },
                label = { Text("Duree (ans)") }, singleLine = true, modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                result = viewModel.simulate(
                    initialCents = Money.parseToCents(initial) ?: 0L,
                    monthlyCents = Money.parseToCents(monthly) ?: 0L,
                    annualRatePct = rate.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    years = years.toIntOrNull() ?: 0,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Calculer") }

        result?.let { r ->
            Spacer(Modifier.height(12.dp))
            KeyValueRow("Capital final", Money.format(r.futureValueCents), emphasize = true)
            KeyValueRow("Total verse", Money.format(r.totalContributedCents))
            KeyValueRow("Interets gagnes", Money.format(r.gainCents), emphasize = true)
            Spacer(Modifier.height(4.dp))
            Text(
                "Hypothese de rendement constant ; la realite varie d'une annee a l'autre.",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}
