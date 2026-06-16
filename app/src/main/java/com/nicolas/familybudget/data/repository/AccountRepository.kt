package com.nicolas.familybudget.data.repository

import com.nicolas.familybudget.data.local.AccountDao
import com.nicolas.familybudget.data.local.AccountEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val dao: AccountDao,
) {
    fun observeActive(): Flow<List<AccountEntity>> = dao.observeActive()
    fun observeAll(): Flow<List<AccountEntity>> = dao.observeAll()
    fun observeNetWorth(): Flow<Long> = dao.observeNetWorth()

    suspend fun byId(id: Long) = dao.byId(id)
    suspend fun save(account: AccountEntity): Long = dao.upsert(account)
    suspend fun update(account: AccountEntity) = dao.update(account)
    suspend fun delete(account: AccountEntity) = dao.delete(account)
    suspend fun archive(account: AccountEntity) = dao.update(account.copy(archived = true))
}
