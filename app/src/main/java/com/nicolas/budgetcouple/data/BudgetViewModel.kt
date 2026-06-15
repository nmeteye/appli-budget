package com.nicolas.budgetcouple.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "budget_couple")
private val STATE_KEY = stringPreferencesKey("app_state")

class BudgetRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): AppState {
        val raw = context.dataStore.data.first()[STATE_KEY] ?: return AppState()
        return runCatching { json.decodeFromString<AppState>(raw) }.getOrDefault(AppState())
    }

    suspend fun save(state: AppState) {
        context.dataStore.edit { it[STATE_KEY] = json.encodeToString(AppState.serializer(), state) }
    }
}

class BudgetViewModel(private val repo: BudgetRepository) : ViewModel() {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init { viewModelScope.launch { _state.value = repo.load() } }

    private fun update(block: (AppState) -> AppState) {
        val next = block(_state.value)
        _state.value = next
        viewModelScope.launch { repo.save(next) }
    }

    /** Ajoute une opération et répercute le montant sur le solde du compte. */
    fun addTransaction(accountId: String, type: TxType, category: String, label: String, amount: Double) {
        if (label.isBlank() || amount <= 0) return
        val tx = Tx(UUID.randomUUID().toString(), accountId, type, category, label, amount)
        update { s ->
            val delta = if (type == TxType.INCOME) amount else -amount
            s.copy(
                transactions = listOf(tx) + s.transactions,
                accounts = s.accounts.map {
                    if (it.id == accountId) it.copy(balance = it.balance + delta) else it
                }
            )
        }
    }

    fun removeTransaction(txId: String) {
        update { s ->
            val tx = s.transactions.find { it.id == txId } ?: return@update s
            val delta = if (tx.type == TxType.INCOME) -tx.amount else tx.amount
            s.copy(
                transactions = s.transactions.filterNot { it.id == txId },
                accounts = s.accounts.map {
                    if (it.id == tx.accountId) it.copy(balance = it.balance + delta) else it
                }
            )
        }
    }

    fun renameAccount(id: String, name: String, owner: String) =
        update { s -> s.copy(accounts = s.accounts.map { if (it.id == id) it.copy(name = name, owner = owner) else it }) }

    fun setSavingsGoal(goal: Double) = update { it.copy(savingsGoal = goal) }

    fun resetDemo() = update { AppState() }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BudgetViewModel(BudgetRepository(context.applicationContext)) as T
    }
}
