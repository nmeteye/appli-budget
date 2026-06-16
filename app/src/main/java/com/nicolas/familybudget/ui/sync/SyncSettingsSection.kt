package com.nicolas.familybudget.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nicolas.familybudget.data.sync.supabase.AuthRepository
import com.nicolas.familybudget.data.sync.supabase.BudgetSyncRepository
import com.nicolas.familybudget.data.sync.supabase.RemoteBudget
import com.nicolas.familybudget.data.sync.supabase.SupabaseConfig
import com.nicolas.familybudget.data.sync.supabase.SyncStateStore
import com.nicolas.familybudget.ui.components.SectionCard
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncUiState(
    val configured: Boolean,
    val signedIn: Boolean = false,
    val currentBudgetId: String? = null,
    val budgets: List<RemoteBudget> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val budgetSync: BudgetSyncRepository,
    private val syncState: SyncStateStore,
) : ViewModel() {

    private val _ui = MutableStateFlow(SyncUiState(configured = SupabaseConfig.isConfigured))
    val ui: StateFlow<SyncUiState> = _ui

    init {
        viewModelScope.launch {
            auth.sessionStatus.collect { status ->
                val signed = status is SessionStatus.Authenticated
                _ui.update { it.copy(signedIn = signed) }
                if (signed) refreshBudgets()
            }
        }
        viewModelScope.launch {
            syncState.currentBudgetId.collect { id -> _ui.update { it.copy(currentBudgetId = id) } }
        }
    }

    private fun runOp(label: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, message = null) }
            try {
                block()
            } catch (e: Exception) {
                _ui.update { it.copy(message = "$label : ${e.message}") }
            } finally {
                _ui.update { it.copy(busy = false) }
            }
        }
    }

    fun signIn(email: String, password: String) = runOp("Connexion") { auth.signIn(email.trim(), password) }
    fun signUp(email: String, password: String) = runOp("Inscription") { auth.signUp(email.trim(), password) }
    fun signOut() = runOp("Deconnexion") { auth.signOut() }

    fun refreshBudgets() = runOp("Chargement des budgets") {
        _ui.update { it.copy(budgets = budgetSync.listMyBudgets()) }
    }

    fun createBudget(name: String) = runOp("Creation du budget") {
        budgetSync.createBudget(name.trim().ifBlank { "Budget familial" })
        budgetSync.syncCurrent()
        _ui.update { it.copy(message = "Budget cree et synchronise") }
    }

    fun selectBudget(id: String) = runOp("Selection du budget") {
        budgetSync.selectBudget(id)
        budgetSync.syncCurrent()
    }

    fun syncNow() = runOp("Synchronisation") {
        budgetSync.syncCurrent()
        _ui.update { it.copy(message = "Synchronise") }
    }
}

/**
 * Section a inserer dans l'ecran Reglages :
 *   import com.nicolas.familybudget.ui.sync.SyncSettingsSection
 *   ... puis dans la liste/colonne :  SyncSettingsSection()
 */
@Composable
fun SyncSettingsSection(modifier: Modifier = Modifier) {
    val vm: SyncViewModel = hiltViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()

    SectionCard(title = "Budget partage (synchronisation)", modifier = modifier) {
        if (!ui.configured) {
            Text(
                "Backend non configure. Renseigne l'URL et la cle anon dans SupabaseConfig.kt, " +
                    "puis execute le script SQL dans ton projet Supabase.",
                style = MaterialTheme.typography.bodyMedium,
            )
            return@SectionCard
        }

        when {
            !ui.signedIn -> AuthForm(busy = ui.busy, onSignIn = vm::signIn, onSignUp = vm::signUp)
            else -> BudgetControls(
                ui = ui,
                onCreate = vm::createBudget,
                onSelect = vm::selectBudget,
                onSync = vm::syncNow,
                onRefresh = vm::refreshBudgets,
                onSignOut = vm::signOut,
            )
        }

        ui.message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        if (ui.busy) {
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun AuthForm(
    busy: Boolean,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column {
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("E-mail") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Mot de passe") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSignIn(email, password) }, enabled = !busy) { Text("Se connecter") }
            OutlinedButton(onClick = { onSignUp(email, password) }, enabled = !busy) { Text("Creer un compte") }
        }
    }
}

@Composable
private fun BudgetControls(
    ui: SyncUiState,
    onCreate: (String) -> Unit,
    onSelect: (String) -> Unit,
    onSync: () -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
) {
    var newBudgetName by remember { mutableStateOf("") }
    Column {
        if (ui.currentBudgetId == null) {
            Text("Aucun budget actif. Cree-en un ou rejoins-en un existant.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = newBudgetName, onValueChange = { newBudgetName = it },
                label = { Text("Nom du budget") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onCreate(newBudgetName) }, enabled = !ui.busy) { Text("Creer ce budget") }

            if (ui.budgets.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Budgets accessibles :", style = MaterialTheme.typography.labelLarge)
                ui.budgets.forEach { b ->
                    TextButton(onClick = { onSelect(b.id) }, enabled = !ui.busy) { Text(b.name) }
                }
            }
        } else {
            Text("Budget actif synchronise.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSync, enabled = !ui.busy) { Text("Synchroniser") }
                OutlinedButton(onClick = onRefresh, enabled = !ui.busy) { Text("Mes budgets") }
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSignOut, enabled = !ui.busy) { Text("Se deconnecter") }
    }
}
