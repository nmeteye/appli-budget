package com.nicolas.familybudget.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nicolas.familybudget.core.Money
import com.nicolas.familybudget.data.csv.CsvImporter
import com.nicolas.familybudget.data.local.AccountEntity
import com.nicolas.familybudget.data.local.FamilyMemberEntity
import com.nicolas.familybudget.data.local.GoalEntity
import com.nicolas.familybudget.data.local.GoalType
import com.nicolas.familybudget.data.local.HouseholdProfile
import com.nicolas.familybudget.data.local.MemberRole
import com.nicolas.familybudget.data.local.RiskAppetite
import com.nicolas.familybudget.data.local.SettingsRepository
import com.nicolas.familybudget.data.repository.AccountRepository
import com.nicolas.familybudget.data.repository.FamilyRepository
import com.nicolas.familybudget.data.repository.GoalRepository
import com.nicolas.familybudget.data.repository.TransactionRepository
import com.nicolas.familybudget.data.sync.BankSyncProvider
import com.nicolas.familybudget.ui.components.SectionCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val family: FamilyRepository,
    private val goals: GoalRepository,
    private val accounts: AccountRepository,
    private val transactions: TransactionRepository,
    private val csvImporter: CsvImporter,
    private val bankSync: BankSyncProvider,
) : ViewModel() {

    val profile: StateFlow<HouseholdProfile> =
        settings.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HouseholdProfile())
    val members: StateFlow<List<FamilyMemberEntity>> =
        family.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val goalsState: StateFlow<List<GoalEntity>> =
        goals.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val accountsState: StateFlow<List<AccountEntity>> =
        accounts.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val syncProviderName: String get() = bankSync.displayName

    private val _status = mutableStateOf<String?>(null)
    val status get() = _status

    fun saveProfile(
        incomeCents: Long, chargesCents: Long, efMonths: Int,
        risk: RiskAppetite, lep: Boolean, debt: Boolean,
    ) {
        viewModelScope.launch {
            settings.update {
                it.copy(
                    monthlyNetIncomeCents = incomeCents,
                    monthlyFixedChargesCents = chargesCents,
                    emergencyFundMonths = efMonths.coerceIn(1, 12),
                    riskAppetite = risk,
                    lepEligible = lep,
                    hasHighInterestDebt = debt,
                    onboardingDone = true,
                )
            }
            _status.value = "Profil enregistre."
        }
    }

    fun addMember(name: String, role: MemberRole, birthYear: Int?) {
        viewModelScope.launch {
            family.save(FamilyMemberEntity(name = name.ifBlank { "Membre" }, role = role, birthYear = birthYear))
        }
    }

    fun deleteMember(m: FamilyMemberEntity) { viewModelScope.launch { family.delete(m) } }

    fun addGoal(
        name: String, type: GoalType, targetCents: Long,
        targetYear: Int?, monthlyCents: Long, priority: Int,
    ) {
        viewModelScope.launch {
            val targetMillis = targetYear?.let {
                LocalDate.of(it, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            goals.save(
                GoalEntity(
                    name = name.ifBlank { type.label }, type = type,
                    targetAmountCents = targetCents, targetDateEpochMillis = targetMillis,
                    monthlyContributionCents = monthlyCents, priority = priority.coerceIn(1, 5),
                )
            )
        }
    }

    fun deleteGoal(g: GoalEntity) { viewModelScope.launch { goals.delete(g) } }

    fun importCsv(accountId: Long, content: String) {
        viewModelScope.launch {
            val result = csvImporter.parse(accountId, content)
            transactions.addImported(result.transactions)
            _status.value =
                "Import : ${result.transactions.size} operation(s), ${result.skippedLines} ligne(s) ignoree(s)."
        }
    }

    /** Synchro de demonstration : cree les comptes distants et importe leurs operations. */
    fun syncDemo() {
        viewModelScope.launch {
            val remoteAccounts = bankSync.fetchAccounts()
            val idMap = HashMap<String, Long>()
            remoteAccounts.forEach { ra ->
                val localId = accounts.save(
                    AccountEntity(name = ra.name, type = ra.type, balanceCents = 0)
                )
                idMap[ra.externalId] = localId
            }
            val remoteTx = bankSync.fetchTransactions(0)
            remoteTx.forEach { rt ->
                val localId = idMap[rt.accountExternalId] ?: return@forEach
                transactions.add(
                    accountId = localId,
                    amountCents = rt.amountCents,
                    label = rt.label,
                    dateEpochMillis = rt.dateEpochMillis,
                    categoryId = null,
                    type = rt.type,
                    source = com.nicolas.familybudget.data.local.TransactionSource.SYNC,
                )
            }
            _status.value = "Synchro demo : ${remoteAccounts.size} compte(s), ${remoteTx.size} operation(s)."
        }
    }

    fun clearStatus() { _status.value = null }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val goals by viewModel.goalsState.collectAsStateWithLifecycle()
    val accounts by viewModel.accountsState.collectAsStateWithLifecycle()
    val status = viewModel.status.value

    var showAddMember by remember { mutableStateOf(false) }
    var showAddGoal by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Parametres", style = MaterialTheme.typography.headlineSmall) }
        status?.let { item { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) } }

        item { ProfileSection(profile, viewModel) }

        item {
            SectionCard(title = "Composition familiale") {
                if (members.isEmpty()) Text("Aucun membre.", style = MaterialTheme.typography.bodyMedium)
                members.forEach { m ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "${m.name} \u00b7 ${if (m.role == MemberRole.ADULT) "adulte" else "enfant"}" +
                                (m.birthYear?.let { " ($it)" } ?: ""),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        IconButton(onClick = { viewModel.deleteMember(m) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
                        }
                    }
                }
                OutlinedButton(onClick = { showAddMember = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ajouter un membre")
                }
            }
        }

        item {
            SectionCard(title = "Objectifs") {
                if (goals.isEmpty()) Text("Aucun objectif.", style = MaterialTheme.typography.bodyMedium)
                goals.forEach { g ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(g.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${g.type.label} \u00b7 cible ${Money.format(g.targetAmountCents)}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        IconButton(onClick = { viewModel.deleteGoal(g) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
                        }
                    }
                }
                OutlinedButton(onClick = { showAddGoal = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ajouter un objectif")
                }
            }
        }

        item {
            SectionCard(title = "Donnees bancaires") {
                Text(
                    "Saisie manuelle, import CSV, et synchro (${viewModel.syncProviderName}). " +
                        "La synchro DSP2 reelle se branche dans le code (voir README).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showImport = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = accounts.isNotEmpty(),
                ) { Text(if (accounts.isEmpty()) "Cree d'abord un compte" else "Importer un CSV") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.syncDemo() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Synchroniser (demo)")
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showAddMember) AddMemberDialog(
        onDismiss = { showAddMember = false },
        onConfirm = { n, r, y -> viewModel.addMember(n, r, y); showAddMember = false },
    )
    if (showAddGoal) AddGoalDialog(
        onDismiss = { showAddGoal = false },
        onConfirm = { n, t, amt, yr, mth, p -> viewModel.addGoal(n, t, amt, yr, mth, p); showAddGoal = false },
    )
    if (showImport) ImportCsvDialog(
        accounts = accounts,
        onDismiss = { showImport = false },
        onConfirm = { accId, text -> viewModel.importCsv(accId, text); showImport = false },
    )
}

@Composable
private fun ProfileSection(profile: HouseholdProfile, viewModel: SettingsViewModel) {
    var income by remember(profile) { mutableStateOf(if (profile.monthlyNetIncomeCents > 0) Money.formatPlain(profile.monthlyNetIncomeCents) else "") }
    var charges by remember(profile) { mutableStateOf(if (profile.monthlyFixedChargesCents > 0) Money.formatPlain(profile.monthlyFixedChargesCents) else "") }
    var efMonths by remember(profile) { mutableStateOf(profile.emergencyFundMonths.toString()) }
    var risk by remember(profile) { mutableStateOf(profile.riskAppetite) }
    var lep by remember(profile) { mutableStateOf(profile.lepEligible) }
    var debt by remember(profile) { mutableStateOf(profile.hasHighInterestDebt) }

    SectionCard(title = "Profil financier du foyer") {
        OutlinedTextField(
            value = income, onValueChange = { income = it },
            label = { Text("Revenu net mensuel (€)") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = charges, onValueChange = { charges = it },
            label = { Text("Charges fixes mensuelles (€)") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = efMonths, onValueChange = { efMonths = it.filter { c -> c.isDigit() } },
            label = { Text("Mois d'epargne de precaution visee") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Text("Appetit pour le risque", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RiskAppetite.entries.forEach { r ->
                FilterChip(selected = risk == r, onClick = { risk = r }, label = { Text(r.label) })
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Eligible au LEP", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = lep, onCheckedChange = { lep = it })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Dette a taux eleve en cours", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = debt, onCheckedChange = { debt = it })
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                viewModel.saveProfile(
                    incomeCents = Money.parseToCents(income) ?: 0L,
                    chargesCents = Money.parseToCents(charges) ?: 0L,
                    efMonths = efMonths.toIntOrNull() ?: 4,
                    risk = risk, lep = lep, debt = debt,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Enregistrer le profil") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, MemberRole, Int?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(MemberRole.ADULT) }
    var year by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(name, role, year.toIntOrNull()) }) { Text("Ajouter") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        title = { Text("Membre de la famille") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Prenom") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = role == MemberRole.ADULT, onClick = { role = MemberRole.ADULT }, label = { Text("Adulte") })
                    FilterChip(selected = role == MemberRole.CHILD, onClick = { role = MemberRole.CHILD }, label = { Text("Enfant") })
                }
                OutlinedTextField(value = year, onValueChange = { year = it.filter { c -> c.isDigit() } }, label = { Text("Annee de naissance (option)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, GoalType, Long, Int?, Long, Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(GoalType.EMERGENCY_FUND) }
    var target by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var monthly by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("3") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    name, type,
                    Money.parseToCents(target) ?: 0L,
                    year.toIntOrNull(),
                    Money.parseToCents(monthly) ?: 0L,
                    priority.toIntOrNull() ?: 3,
                )
            }) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        title = { Text("Objectif") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = type.label, onValueChange = {}, readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    androidx.compose.material3.ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        GoalType.entries.forEach { t ->
                            DropdownMenuItem(text = { Text(t.label) }, onClick = { type = t; expanded = false })
                        }
                    }
                }
                OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Montant cible (€)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = year, onValueChange = { year = it.filter { c -> c.isDigit() } }, label = { Text("Annee cible (option)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = monthly, onValueChange = { monthly = it }, label = { Text("Versement mensuel prevu (€)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = priority, onValueChange = { priority = it.filter { c -> c.isDigit() } }, label = { Text("Priorite (1 = max, 5 = min)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportCsvDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, String) -> Unit,
) {
    var selected by remember { mutableStateOf(accounts.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { selected?.let { onConfirm(it.id, text) } },
                enabled = selected != null && text.isNotBlank(),
            ) { Text("Importer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        title = { Text("Import CSV") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Colle un releve : une ligne par operation au format " +
                        "date;libelle;montant (montant negatif = depense).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selected?.name ?: "", onValueChange = {}, readOnly = true,
                        label = { Text("Compte cible") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    androidx.compose.material3.ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        accounts.forEach { a ->
                            DropdownMenuItem(text = { Text(a.name) }, onClick = { selected = a; expanded = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text("Contenu CSV") },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                )
                Text(
                    "Exemple :\n15/06/2026;Courses;-45,90\n01/06/2026;Salaire;2450,00",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                )
            }
        },
    )
}
