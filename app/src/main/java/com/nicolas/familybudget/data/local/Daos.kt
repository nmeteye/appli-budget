package com.nicolas.familybudget.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Projection : total depense par categorie sur une periode. */
data class CategorySpend(
    val categoryId: Long?,
    val totalCents: Long,
)

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE archived = 0 AND isDeleted = 0 ORDER BY name")
    fun observeActive(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isDeleted = 0 ORDER BY name")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun byId(id: Long): AccountEntity?

    @Query("SELECT COALESCE(SUM(balanceCents), 0) FROM accounts WHERE archived = 0 AND isDeleted = 0")
    fun observeNetWorth(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: AccountEntity): Long

    @Query("UPDATE accounts SET balanceCents = balanceCents + :deltaCents WHERE id = :id")
    suspend fun adjustBalance(id: Long, deltaCents: Long)

    /** Fixe le solde a une valeur absolue (recalcul post-sync). */
    @Query("UPDATE accounts SET balanceCents = :cents WHERE id = :id")
    suspend fun setBalance(id: Long, cents: Long)

    @Update suspend fun update(account: AccountEntity)
    @Delete suspend fun delete(account: AccountEntity)

    // --- Synchronisation ---
    @Query("SELECT * FROM accounts WHERE isDirty = 1")
    suspend fun dirty(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE syncId = :syncId LIMIT 1")
    suspend fun bySyncId(syncId: String): AccountEntity?

    /** Tous les comptes non supprimes : sert a construire la table syncId -> id local. */
    @Query("SELECT * FROM accounts")
    suspend fun allRaw(): List<AccountEntity>

    @Query("UPDATE accounts SET isDirty = 0 WHERE syncId IN (:syncIds)")
    suspend fun markClean(syncIds: List<String>)

    /** Rattache les lignes locales orphelines a un budget (a la creation/jonction). */
    @Query("UPDATE accounts SET budgetId = :budgetId, isDirty = 1, updatedAt = :ts WHERE budgetId IS NULL")
    suspend fun attachToBudget(budgetId: String, ts: Long)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY dateEpochMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND isDeleted = 0 ORDER BY dateEpochMillis DESC")
    fun observeForAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT categoryId AS categoryId, COALESCE(SUM(amountCents), 0) AS totalCents
        FROM transactions
        WHERE type = 'EXPENSE' AND isDeleted = 0 AND dateEpochMillis BETWEEN :from AND :to
        GROUP BY categoryId
        """
    )
    fun observeExpenseByCategory(from: Long, to: Long): Flow<List<CategorySpend>>

    @Query(
        "SELECT COALESCE(SUM(amountCents), 0) FROM transactions " +
            "WHERE type = 'INCOME' AND isDeleted = 0 AND dateEpochMillis BETWEEN :from AND :to"
    )
    fun observeIncomeBetween(from: Long, to: Long): Flow<Long>

    @Query(
        "SELECT COALESCE(SUM(amountCents), 0) FROM transactions " +
            "WHERE type = 'EXPENSE' AND isDeleted = 0 AND dateEpochMillis BETWEEN :from AND :to"
    )
    fun observeExpenseBetween(from: Long, to: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(txs: List<TransactionEntity>): List<Long>

    @Update suspend fun update(tx: TransactionEntity)

    @Delete suspend fun delete(tx: TransactionEntity)

    // --- Synchronisation ---
    @Query("SELECT * FROM transactions WHERE isDirty = 1")
    suspend fun dirty(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE syncId = :syncId LIMIT 1")
    suspend fun bySyncId(syncId: String): TransactionEntity?

    @Query("UPDATE transactions SET isDirty = 0 WHERE syncId IN (:syncIds)")
    suspend fun markClean(syncIds: List<String>)

    /** Solde recalcule d'un compte a partir de ses operations vivantes. */
    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions WHERE accountId = :accountId AND isDeleted = 0")
    suspend fun balanceForAccount(accountId: Long): Long

    @Query("UPDATE transactions SET budgetId = :budgetId, isDirty = 1, updatedAt = :ts WHERE budgetId IS NULL")
    suspend fun attachToBudget(budgetId: String, ts: Long)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY kind, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    /** Toutes les categories (y compris supprimees) : pour resoudre le nom d'anciennes operations. */
    @Query("SELECT * FROM categories")
    suspend fun all(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update suspend fun update(category: CategoryEntity)

    @Delete suspend fun delete(category: CategoryEntity)

    // --- Synchronisation ---
    @Query("SELECT * FROM categories WHERE isDirty = 1")
    suspend fun dirty(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE syncId = :syncId LIMIT 1")
    suspend fun bySyncId(syncId: String): CategoryEntity?

    @Query("UPDATE categories SET isDirty = 0 WHERE syncId IN (:syncIds)")
    suspend fun markClean(syncIds: List<String>)

    @Query("UPDATE categories SET budgetId = :budgetId, isDirty = 1, updatedAt = :ts WHERE budgetId IS NULL")
    suspend fun attachToBudget(budgetId: String, ts: Long)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY priority, createdAt")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals")
    suspend fun all(): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: GoalEntity): Long

    @Delete suspend fun delete(goal: GoalEntity)
}

@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members ORDER BY role, name")
    fun observeAll(): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members")
    suspend fun all(): List<FamilyMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(member: FamilyMemberEntity): Long

    @Delete suspend fun delete(member: FamilyMemberEntity)
}
