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
    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY name")
    fun observeActive(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY name")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun byId(id: Long): AccountEntity?

    @Query("SELECT COALESCE(SUM(balanceCents), 0) FROM accounts WHERE archived = 0")
    fun observeNetWorth(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: AccountEntity): Long

    @Query("UPDATE accounts SET balanceCents = balanceCents + :deltaCents WHERE id = :id")
    suspend fun adjustBalance(id: Long, deltaCents: Long)

    @Update suspend fun update(account: AccountEntity)
    @Delete suspend fun delete(account: AccountEntity)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateEpochMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY dateEpochMillis DESC")
    fun observeForAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT categoryId AS categoryId, COALESCE(SUM(amountCents), 0) AS totalCents
        FROM transactions
        WHERE type = 'EXPENSE' AND dateEpochMillis BETWEEN :from AND :to
        GROUP BY categoryId
        """
    )
    fun observeExpenseByCategory(from: Long, to: Long): Flow<List<CategorySpend>>

    @Query(
        "SELECT COALESCE(SUM(amountCents), 0) FROM transactions " +
            "WHERE type = 'INCOME' AND dateEpochMillis BETWEEN :from AND :to"
    )
    fun observeIncomeBetween(from: Long, to: Long): Flow<Long>

    @Query(
        "SELECT COALESCE(SUM(amountCents), 0) FROM transactions " +
            "WHERE type = 'EXPENSE' AND dateEpochMillis BETWEEN :from AND :to"
    )
    fun observeExpenseBetween(from: Long, to: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(txs: List<TransactionEntity>): List<Long>

    @Delete suspend fun delete(tx: TransactionEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY kind, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun all(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Delete suspend fun delete(category: CategoryEntity)
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
