package com.nicolas.familybudget.data.repository

import com.nicolas.familybudget.data.local.AppDatabase
import com.nicolas.familybudget.data.local.CategoryDao
import com.nicolas.familybudget.data.local.CategoryEntity
import com.nicolas.familybudget.data.local.FamilyMemberDao
import com.nicolas.familybudget.data.local.FamilyMemberEntity
import com.nicolas.familybudget.data.local.GoalDao
import com.nicolas.familybudget.data.local.GoalEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val dao: CategoryDao,
) {
    fun observeAll(): Flow<List<CategoryEntity>> = dao.observeAll()
    suspend fun all(): List<CategoryEntity> = dao.all()
    suspend fun save(category: CategoryEntity): Long = dao.upsert(category)
    suspend fun delete(category: CategoryEntity) = dao.delete(category)

    /** Insere les categories par defaut au premier lancement seulement. */
    suspend fun seedIfEmpty() {
        if (dao.count() == 0) dao.insertAll(AppDatabase.DEFAULT_CATEGORIES)
    }
}

@Singleton
class GoalRepository @Inject constructor(
    private val dao: GoalDao,
) {
    fun observeAll(): Flow<List<GoalEntity>> = dao.observeAll()
    suspend fun all(): List<GoalEntity> = dao.all()
    suspend fun save(goal: GoalEntity): Long = dao.upsert(goal)
    suspend fun delete(goal: GoalEntity) = dao.delete(goal)
}

@Singleton
class FamilyRepository @Inject constructor(
    private val dao: FamilyMemberDao,
) {
    fun observeAll(): Flow<List<FamilyMemberEntity>> = dao.observeAll()
    suspend fun all(): List<FamilyMemberEntity> = dao.all()
    suspend fun save(member: FamilyMemberEntity): Long = dao.upsert(member)
    suspend fun delete(member: FamilyMemberEntity) = dao.delete(member)
}
