package com.nicolas.familybudget.data.sync.supabase

import com.nicolas.familybudget.data.local.AccountDao
import com.nicolas.familybudget.data.local.AccountEntity
import com.nicolas.familybudget.data.local.AccountType
import com.nicolas.familybudget.data.local.BudgetBucket
import com.nicolas.familybudget.data.local.CategoryDao
import com.nicolas.familybudget.data.local.CategoryEntity
import com.nicolas.familybudget.data.local.CategoryKind
import com.nicolas.familybudget.data.local.TransactionDao
import com.nicolas.familybudget.data.local.TransactionEntity
import com.nicolas.familybudget.data.local.TransactionSource
import com.nicolas.familybudget.data.local.TransactionType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Synchronisation d'un budget partage entre plusieurs personnes.
 *
 * Principe : offline-first. Room reste la source de verite locale ; on pousse les
 * lignes modifiees (isDirty) puis on tire le delta (updated_at > curseur). La fusion
 * est un last-write-wins par ligne sur updated_at. Les soldes ne sont jamais
 * transmis : ils sont recalcules a partir des transactions apres chaque pull.
 *
 * Les references entre tables (transaction -> compte/categorie) passent par sync_id,
 * traduit en id Long local de part et d'autre.
 */
@Singleton
class BudgetSyncRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val auth: AuthRepository,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val syncState: SyncStateStore,
) {

    /* ---------- Gestion des budgets ---------- */

    /** Budgets dont l'utilisateur courant est membre (filtre par la RLS cote serveur). */
    suspend fun listMyBudgets(): List<RemoteBudget> =
        supabase.from("budgets").select().decodeList<RemoteBudget>()

    /**
     * Cree un budget, le rend actif, et rattache toutes les donnees locales
     * orphelines a ce budget pour qu'elles soient poussees au prochain sync.
     */
    suspend fun createBudget(name: String): String {
        val ownerId = auth.currentUserId() ?: error("Utilisateur non connecte")
        val budgetId = UUID.randomUUID().toString()
        supabase.from("budgets").insert(
            RemoteBudget(id = budgetId, name = name, ownerId = ownerId, updatedAt = now())
        )
        // Le trigger serveur inscrit l'auteur comme membre 'owner'.
        attachLocalDataTo(budgetId)
        syncState.setCurrentBudgetId(budgetId)
        return budgetId
    }

    /** Selectionne un budget existant (cas du membre invite). */
    suspend fun selectBudget(budgetId: String) {
        attachLocalDataTo(budgetId)
        syncState.setCurrentBudgetId(budgetId)
    }

    private suspend fun attachLocalDataTo(budgetId: String) {
        val ts = now()
        accountDao.attachToBudget(budgetId, ts)
        categoryDao.attachToBudget(budgetId, ts)
        transactionDao.attachToBudget(budgetId, ts)
    }

    /* ---------- Cycle de synchronisation ---------- */

    suspend fun syncCurrent() {
        val budgetId = syncState.currentBudgetIdOnce() ?: return
        if (!auth.isSignedIn()) return
        sync(budgetId)
    }

    suspend fun sync(budgetId: String) {
        // On pousse avant de tirer pour limiter le risque qu'un pull ecrase une
        // ecriture locale pas encore partie.
        push(budgetId)
        pull(budgetId)
        recomputeBalances()
    }

    /* ---------- PUSH ---------- */

    private suspend fun push(budgetId: String) {
        // Cartes id local -> sync_id pour traduire les references des transactions.
        val accSyncById = accountDao.allRaw().associate { it.id to it.syncId }
        val catSyncById = categoryDao.all().associate { it.id to it.syncId }

        accountDao.dirty().takeIf { it.isNotEmpty() }?.let { dirty ->
            supabase.from("accounts").upsert(dirty.map { it.toRemote(budgetId) })
            accountDao.markClean(dirty.map { it.syncId })
        }

        categoryDao.dirty().takeIf { it.isNotEmpty() }?.let { dirty ->
            supabase.from("categories").upsert(dirty.map { it.toRemote(budgetId) })
            categoryDao.markClean(dirty.map { it.syncId })
        }

        transactionDao.dirty().takeIf { it.isNotEmpty() }?.let { dirty ->
            val remote = dirty.mapNotNull { tx ->
                val accSync = accSyncById[tx.accountId] ?: return@mapNotNull null
                tx.toRemote(budgetId, accSync, tx.categoryId?.let { catSyncById[it] })
            }
            if (remote.isNotEmpty()) {
                supabase.from("transactions").upsert(remote)
                transactionDao.markClean(remote.map { it.syncId })
            }
        }
    }

    /* ---------- PULL (delta) ---------- */

    private suspend fun pull(budgetId: String) {
        val cursor = syncState.cursor(budgetId)
        var maxSeen = cursor

        // 1) Comptes
        val remoteAccounts = supabase.from("accounts").select {
            filter {
                eq("budget_id", budgetId)
                gt("updated_at", cursor)
            }
        }.decodeList<RemoteAccount>()
        for (r in remoteAccounts) {
            maxSeen = maxOf(maxSeen, r.updatedAt)
            val local = accountDao.bySyncId(r.syncId)
            if (local.shouldKeepOver(r.updatedAt)) continue
            accountDao.upsert(r.toEntity(localId = local?.id ?: 0, keepBalance = local?.balanceCents ?: 0))
        }

        // 2) Categories
        val remoteCategories = supabase.from("categories").select {
            filter {
                eq("budget_id", budgetId)
                gt("updated_at", cursor)
            }
        }.decodeList<RemoteCategory>()
        for (r in remoteCategories) {
            maxSeen = maxOf(maxSeen, r.updatedAt)
            val local = categoryDao.bySyncId(r.syncId)
            if (local.shouldKeepOver(r.updatedAt)) continue
            categoryDao.upsert(r.toEntity(localId = local?.id ?: 0))
        }

        // 3) Transactions : on traduit les sync_id en id locaux (comptes deja pulls).
        val accIdBySync = accountDao.allRaw().associate { it.syncId to it.id }
        val catIdBySync = categoryDao.all().associate { it.syncId to it.id }
        val remoteTx = supabase.from("transactions").select {
            filter {
                eq("budget_id", budgetId)
                gt("updated_at", cursor)
            }
        }.decodeList<RemoteTransaction>()
        for (r in remoteTx) {
            maxSeen = maxOf(maxSeen, r.updatedAt)
            val local = transactionDao.bySyncId(r.syncId)
            if (local.shouldKeepOver(r.updatedAt)) continue
            val accountId = accIdBySync[r.accountSyncId] ?: continue // compte inconnu -> on saute
            val categoryId = r.categorySyncId?.let { catIdBySync[it] }
            transactionDao.insert(r.toEntity(localId = local?.id ?: 0, accountId = accountId, categoryId = categoryId))
        }

        if (maxSeen > cursor) syncState.setCursor(budgetId, maxSeen)
    }

    /* ---------- Recalcul des soldes (agregats derives) ---------- */

    private suspend fun recomputeBalances() {
        accountDao.allRaw().forEach { acc ->
            accountDao.setBalance(acc.id, transactionDao.balanceForAccount(acc.id))
        }
    }

    private fun now() = System.currentTimeMillis()
}

/* ============================================================
 *  Helpers de fusion et de mapping entite <-> DTO
 * ============================================================ */

/** Conserve la version locale si elle est modifiee localement et plus recente. */
private fun AccountEntity?.shouldKeepOver(remoteUpdatedAt: Long): Boolean =
    this != null && isDirty && updatedAt > remoteUpdatedAt

private fun CategoryEntity?.shouldKeepOver(remoteUpdatedAt: Long): Boolean =
    this != null && isDirty && updatedAt > remoteUpdatedAt

private fun TransactionEntity?.shouldKeepOver(remoteUpdatedAt: Long): Boolean =
    this != null && isDirty && updatedAt > remoteUpdatedAt

private fun AccountEntity.toRemote(budgetId: String) = RemoteAccount(
    syncId = syncId,
    budgetId = this.budgetId ?: budgetId,
    name = name,
    type = type.name,
    ownerLabel = ownerLabel,
    colorArgb = colorArgb,
    archived = archived,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

private fun RemoteAccount.toEntity(localId: Long, keepBalance: Long) = AccountEntity(
    id = localId,
    name = name,
    type = runCatching { AccountType.valueOf(type) }.getOrDefault(AccountType.CURRENT),
    balanceCents = keepBalance, // recalcule ensuite
    ownerLabel = ownerLabel,
    colorArgb = colorArgb,
    archived = archived,
    createdAt = createdAt,
    syncId = syncId,
    budgetId = budgetId,
    updatedAt = updatedAt,
    isDirty = false,
    isDeleted = isDeleted,
)

private fun CategoryEntity.toRemote(budgetId: String) = RemoteCategory(
    syncId = syncId,
    budgetId = this.budgetId ?: budgetId,
    name = name,
    kind = kind.name,
    bucket = bucket.name,
    emoji = emoji,
    monthlyBudgetCents = monthlyBudgetCents,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

private fun RemoteCategory.toEntity(localId: Long) = CategoryEntity(
    id = localId,
    name = name,
    kind = runCatching { CategoryKind.valueOf(kind) }.getOrDefault(CategoryKind.EXPENSE),
    bucket = runCatching { BudgetBucket.valueOf(bucket) }.getOrDefault(BudgetBucket.NEEDS),
    emoji = emoji,
    monthlyBudgetCents = monthlyBudgetCents,
    syncId = syncId,
    budgetId = budgetId,
    updatedAt = updatedAt,
    isDirty = false,
    isDeleted = isDeleted,
)

private fun TransactionEntity.toRemote(
    budgetId: String,
    accountSyncId: String,
    categorySyncId: String?,
) = RemoteTransaction(
    syncId = syncId,
    budgetId = this.budgetId ?: budgetId,
    accountSyncId = accountSyncId,
    categorySyncId = categorySyncId,
    amountCents = amountCents,
    dateEpochMillis = dateEpochMillis,
    label = label,
    note = note,
    type = type.name,
    transferGroupId = transferGroupId,
    source = source.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
)

private fun RemoteTransaction.toEntity(localId: Long, accountId: Long, categoryId: Long?) = TransactionEntity(
    id = localId,
    accountId = accountId,
    amountCents = amountCents,
    dateEpochMillis = dateEpochMillis,
    categoryId = categoryId,
    label = label,
    note = note,
    type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE),
    transferGroupId = transferGroupId,
    source = runCatching { TransactionSource.valueOf(source) }.getOrDefault(TransactionSource.SYNC),
    createdAt = createdAt,
    syncId = syncId,
    budgetId = budgetId,
    updatedAt = updatedAt,
    isDirty = false,
    isDeleted = isDeleted,
)
