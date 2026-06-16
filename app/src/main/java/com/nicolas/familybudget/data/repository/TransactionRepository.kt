package com.nicolas.familybudget.data.repository

import com.nicolas.familybudget.data.local.AccountDao
import com.nicolas.familybudget.data.local.CategorySpend
import com.nicolas.familybudget.data.local.TransactionDao
import com.nicolas.familybudget.data.local.TransactionEntity
import com.nicolas.familybudget.data.local.TransactionSource
import com.nicolas.familybudget.data.local.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class TransactionRepository @Inject constructor(
    private val txDao: TransactionDao,
    private val accountDao: AccountDao,
) {
    fun observeRecent(limit: Int = 100): Flow<List<TransactionEntity>> = txDao.observeRecent(limit)
    fun observeForAccount(accountId: Long): Flow<List<TransactionEntity>> =
        txDao.observeForAccount(accountId)

    fun observeExpenseByCategory(from: Long, to: Long): Flow<List<CategorySpend>> =
        txDao.observeExpenseByCategory(from, to)

    fun observeIncomeBetween(from: Long, to: Long): Flow<Long> = txDao.observeIncomeBetween(from, to)
    fun observeExpenseBetween(from: Long, to: Long): Flow<Long> =
        txDao.observeExpenseBetween(from, to)

    /**
     * Ajoute une operation et met a jour le solde du compte.
     * [amountCents] doit etre signe : negatif pour une depense, positif pour une entree.
     */
    suspend fun add(
        accountId: Long,
        amountCents: Long,
        label: String,
        dateEpochMillis: Long,
        categoryId: Long?,
        type: TransactionType,
        note: String? = null,
        source: TransactionSource = TransactionSource.MANUAL,
    ): Long {
        val id = txDao.insert(
            TransactionEntity(
                accountId = accountId,
                amountCents = amountCents,
                dateEpochMillis = dateEpochMillis,
                categoryId = categoryId,
                label = label,
                note = note,
                type = type,
                source = source,
            )
        )
        accountDao.adjustBalance(accountId, amountCents)
        return id
    }

    /** Virement entre deux comptes : deux lignes liees par un meme groupe. */
    suspend fun transfer(
        fromAccountId: Long,
        toAccountId: Long,
        amountCents: Long,
        dateEpochMillis: Long,
        label: String = "Virement",
    ) {
        val positive = abs(amountCents)
        val group = UUID.randomUUID().toString()
        txDao.insert(
            TransactionEntity(
                accountId = fromAccountId,
                amountCents = -positive,
                dateEpochMillis = dateEpochMillis,
                label = label,
                type = TransactionType.TRANSFER,
                transferGroupId = group,
            )
        )
        txDao.insert(
            TransactionEntity(
                accountId = toAccountId,
                amountCents = positive,
                dateEpochMillis = dateEpochMillis,
                label = label,
                type = TransactionType.TRANSFER,
                transferGroupId = group,
            )
        )
        accountDao.adjustBalance(fromAccountId, -positive)
        accountDao.adjustBalance(toAccountId, positive)
    }

    suspend fun addImported(txs: List<TransactionEntity>) {
        if (txs.isEmpty()) return
        txDao.insertAll(txs)
        // Regroupe les deltas par compte pour limiter les ecritures.
        txs.groupBy { it.accountId }.forEach { (accountId, list) ->
            accountDao.adjustBalance(accountId, list.sumOf { it.amountCents })
        }
    }

    suspend fun remove(tx: TransactionEntity) {
        txDao.delete(tx)
        accountDao.adjustBalance(tx.accountId, -tx.amountCents)
    }
}
