package com.nicolas.familybudget.data.sync.supabase

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_state")

/**
 * Etat de synchronisation persiste localement :
 *  - le budget partage actuellement actif,
 *  - le curseur delta par budget (dernier updated_at recu).
 */
@Singleton
class SyncStateStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val budgetId = stringPreferencesKey("current_budget_id")
        fun cursor(budgetId: String) = longPreferencesKey("cursor_$budgetId")
    }

    val currentBudgetId: Flow<String?> = context.syncDataStore.data.map { it[Keys.budgetId] }

    suspend fun currentBudgetIdOnce(): String? = currentBudgetId.first()

    suspend fun setCurrentBudgetId(id: String?) {
        context.syncDataStore.edit { prefs ->
            if (id == null) prefs.remove(Keys.budgetId) else prefs[Keys.budgetId] = id
        }
    }

    suspend fun cursor(budgetId: String): Long =
        context.syncDataStore.data.map { it[Keys.cursor(budgetId)] ?: 0L }.first()

    suspend fun setCursor(budgetId: String, value: Long) {
        context.syncDataStore.edit { it[Keys.cursor(budgetId)] = value }
    }
}
