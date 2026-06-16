package com.nicolas.familybudget.ui.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nicolas.familybudget.core.Money
import com.nicolas.familybudget.data.local.AccountEntity
import com.nicolas.familybudget.data.local.AccountType
import com.nicolas.familybudget.data.repository.AccountRepository
import com.nicolas.familybudget.ui.components.SectionCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repository: AccountRepository,
) : ViewModel() {
    val accounts: StateFlow<List<AccountEntity>> =
        repository.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val netWorth: StateFlow<Long> =
        repository.observeActive().map { list -> list.sumOf { it.balanceCents } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun addAccount(name: String, type: AccountType, owner: String, initialBalanceCents: Long) {
        viewModelScope.launch {
            repository.save(
                AccountEntity(
                    name = name.ifBlank { type.label },
                    type = type,
                    ownerLabel = owner.ifBlank { "Commun" },
                    balanceCents = initialBalanceCents,
                )
            )
        }
    }
}

@Composable
fun AccountsScreen(
    onOpenAccount: (Long) -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val netWorth by viewModel.netWorth.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter un compte")
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text("Comptes", style = MaterialTheme.typography.headlineSmall) }
            item {
                SectionCard {
                    Text("Total", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        Money.format(netWorth),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (accounts.isEmpty()) {
                item {
                    Text(
                        "Ajoute ton premier compte avec le bouton +.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            items(accounts, key = { it.id }) { acc ->
                SectionCard(modifier = Modifier.clickable { onOpenAccount(acc.id) }) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(acc.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${acc.type.label} \u00b7 ${acc.ownerLabel}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Text(
                            Money.format(acc.balanceCents),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showAdd) {
        AddAccountDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name, type, owner, balance ->
                viewModel.addAccount(name, type, owner, balance)
                showAdd = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, AccountType, String, Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("Commun") }
    var balance by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.CURRENT) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(name, type, owner, Money.parseToCents(balance) ?: 0L)
            }) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        title = { Text("Nouveau compte") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nom") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = type.label, onValueChange = {}, readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    androidx.compose.material3.ExposedDropdownMenu(
                        expanded = expanded, onDismissRequest = { expanded = false },
                    ) {
                        AccountType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.label) },
                                onClick = { type = t; expanded = false },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = owner, onValueChange = { owner = it },
                    label = { Text("Titulaire (ex. Commun, prenom)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = balance, onValueChange = { balance = it },
                    label = { Text("Solde initial (€)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}
