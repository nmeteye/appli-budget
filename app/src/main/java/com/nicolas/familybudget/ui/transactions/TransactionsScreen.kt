package com.nicolas.familybudget.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.nicolas.familybudget.data.local.CategoryEntity
import com.nicolas.familybudget.data.local.CategoryKind
import com.nicolas.familybudget.data.local.TransactionEntity
import com.nicolas.familybudget.data.local.TransactionType
import com.nicolas.familybudget.data.repository.CategoryRepository
import com.nicolas.familybudget.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactions: TransactionRepository,
    categories: CategoryRepository,
) : ViewModel() {

    private var accountId: Long = 0
    var items: StateFlow<List<TransactionEntity>> =
        transactions.observeRecent(0).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        private set

    val categories: StateFlow<List<CategoryEntity>> =
        categories.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun bind(id: Long) {
        if (id == accountId && accountId != 0L) return
        accountId = id
        items = transactions.observeForAccount(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    fun add(amountAbsCents: Long, isExpense: Boolean, label: String, categoryId: Long?) {
        viewModelScope.launch {
            val signed = if (isExpense) -abs(amountAbsCents) else abs(amountAbsCents)
            transactions.add(
                accountId = accountId,
                amountCents = signed,
                label = label.ifBlank { if (isExpense) "Depense" else "Entree" },
                dateEpochMillis = System.currentTimeMillis(),
                categoryId = categoryId,
                type = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME,
            )
        }
    }

    fun delete(tx: TransactionEntity) {
        viewModelScope.launch { transactions.remove(tx) }
    }
}

private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    accountId: Long,
    onBack: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    viewModel.bind(accountId)
    val items by viewModel.items.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Operations") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter une operation")
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (items.isEmpty()) {
                item {
                    Text(
                        "Aucune operation. Ajoute-en une avec +.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
            items(items, key = { it.id }) { tx ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(tx.label, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            Instant.ofEpochMilli(tx.dateEpochMillis)
                                .atZone(ZoneId.systemDefault()).toLocalDate().format(dateFmt),
                            style = MaterialTheme.typography.bodyMedium,
                        )
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

    if (showAdd) {
        AddTransactionDialog(
            categories = categories,
            onDismiss = { showAdd = false },
            onConfirm = { amount, isExpense, label, catId ->
                viewModel.add(amount, isExpense, label, catId)
                showAdd = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Boolean, String, Long?) -> Unit,
) {
    var isExpense by remember { mutableStateOf(true) }
    var amount by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<CategoryEntity?>(null) }
    var expanded by remember { mutableStateOf(false) }

    val relevant = categories.filter {
        if (isExpense) it.kind == CategoryKind.EXPENSE else it.kind == CategoryKind.INCOME
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val cents = Money.parseToCents(amount) ?: 0L
                onConfirm(cents, isExpense, label, category?.id)
            }) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        title = { Text("Nouvelle operation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isExpense,
                        onClick = { isExpense = true; category = null },
                        label = { Text("Depense") },
                    )
                    FilterChip(
                        selected = !isExpense,
                        onClick = { isExpense = false; category = null },
                        label = { Text("Entree") },
                    )
                }
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("Montant (€)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = label, onValueChange = { label = it },
                    label = { Text("Libelle") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = category?.let { "${it.emoji} ${it.name}" } ?: "Sans categorie",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Categorie") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded, onDismissRequest = { expanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sans categorie") },
                            onClick = { category = null; expanded = false },
                        )
                        relevant.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.emoji} ${c.name}") },
                                onClick = { category = c; expanded = false },
                            )
                        }
                    }
                }
            }
        },
    )
}
