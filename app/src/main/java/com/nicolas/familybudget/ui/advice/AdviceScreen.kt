package com.nicolas.familybudget.ui.advice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nicolas.familybudget.data.local.FamilyMemberEntity
import com.nicolas.familybudget.data.local.GoalEntity
import com.nicolas.familybudget.data.local.HouseholdProfile
import com.nicolas.familybudget.data.local.SettingsRepository
import com.nicolas.familybudget.data.repository.FamilyRepository
import com.nicolas.familybudget.data.repository.GoalRepository
import com.nicolas.familybudget.domain.BudgetSummaryUseCase
import com.nicolas.familybudget.domain.advice.BudgetAdviceEngine
import com.nicolas.familybudget.domain.model.Advice
import com.nicolas.familybudget.domain.model.AdviceSeverity
import com.nicolas.familybudget.domain.model.BudgetSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AdviceViewModel @Inject constructor(
    summaryUseCase: BudgetSummaryUseCase,
    settings: SettingsRepository,
    goals: GoalRepository,
    family: FamilyRepository,
    private val engine: BudgetAdviceEngine,
) : ViewModel() {

    val advice: StateFlow<List<Advice>> = combine(
        summaryUseCase.observe(),
        settings.profile,
        goals.observeAll(),
        family.observeAll(),
    ) { summary: BudgetSummary, profile: HouseholdProfile, g: List<GoalEntity>, m: List<FamilyMemberEntity> ->
        engine.generate(profile, summary, g, m)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun AdviceScreen(viewModel: AdviceViewModel = hiltViewModel()) {
    val advice by viewModel.advice.collectAsStateWithLifecycle()
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Conseils", style = MaterialTheme.typography.headlineSmall) }
        item {
            Text(
                "Recommandations fondees sur des mecanismes de finance comportementale. " +
                    "Touche une carte pour voir le principe applique.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        items(advice, key = { it.id }) { item ->
            AdviceCard(
                advice = item,
                showPrinciple = expanded[item.id] == true,
                onToggle = { expanded[item.id] = !(expanded[item.id] ?: false) },
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun AdviceCard(advice: Advice, showPrinciple: Boolean, onToggle: () -> Unit) {
    val accent = when (advice.severity) {
        AdviceSeverity.CRITICAL -> MaterialTheme.colorScheme.error
        AdviceSeverity.WARNING -> MaterialTheme.colorScheme.secondary
        AdviceSeverity.INFO -> MaterialTheme.colorScheme.primary
        AdviceSeverity.POSITIVE -> MaterialTheme.colorScheme.primary
    }
    Card(
        modifier = Modifier.clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(advice.title, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(advice.body, style = MaterialTheme.typography.bodyMedium)
            AnimatedVisibility(visible = showPrinciple) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Principe : ${advice.principle}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}
