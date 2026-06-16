package com.nicolas.familybudget.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        GoalEntity::class,
        FamilyMemberEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun goalDao(): GoalDao
    abstract fun familyMemberDao(): FamilyMemberDao

    companion object {
        const val NAME = "family_budget.db"

        /** Categories par defaut, deja rangees dans leur seau 50/30/20. */
        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity(name = "Salaire", kind = CategoryKind.INCOME, bucket = BudgetBucket.INCOME, emoji = "💼"),
            CategoryEntity(name = "Autres revenus", kind = CategoryKind.INCOME, bucket = BudgetBucket.INCOME, emoji = "➕"),
            CategoryEntity(name = "Loyer / credit", kind = CategoryKind.EXPENSE, bucket = BudgetBucket.NEEDS, emoji = "🏠"),
            CategoryEntity(name = "Energie / eau", kind = CategoryKind.EXPENSE, bucket = BudgetBucket.NEEDS, emoji = "⚡"),
            CategoryEntity(name = "Courses", kind = CategoryKind.EXPENSE, bucket = BudgetBucket.NEEDS, emoji = "🛒"),
            CategoryEntity(name = "Transport", kind = CategoryKind.EXPENSE, bucket = BudgetBucket.NEEDS, emoji = "🚗"),
            CategoryEntity(name = "Sante", kind = CategoryKind.EXPENSE, bucket = BudgetBucket.NEEDS, emoji = "🩺"),
            CategoryEntity(name = "Assurances", kind = CategoryKind.EXPENSE, bucket = BudgetBucket.NEEDS, emoji = "🛡️"),
            CategoryEntity(name = "Enfants / ecole", kind = CategoryKind.EXPENSE, bucket = BudgetBucket.NEEDS, emoji = "🎒"),
            CategoryEntity(name = "Restaurants / sorties", kind = CategoryKind.EXPENSE, bucket = BudgetBucket.WANTS, emoji = "🍽️"),
            CategoryEntity(name = "Loisirs", kind = CategoryKind.EXPENSE, bucket = BudgetBucket.WANTS, emoji = "🎮"),
            CategoryEntity(name = "Abonnements", kind = CategoryKind.EXPENSE, bucket = BudgetBucket.WANTS, emoji = "📺"),
            CategoryEntity(name = "Shopping", kind = CategoryKind.EXPENSE, bucket = BudgetBucket.WANTS, emoji = "🛍️"),
            CategoryEntity(name = "Vacances", kind = CategoryKind.EXPENSE, bucket = BudgetBucket.WANTS, emoji = "✈️"),
            CategoryEntity(name = "Epargne", kind = CategoryKind.EXPENSE, bucket = BudgetBucket.SAVINGS, emoji = "🐖"),
            CategoryEntity(name = "Remboursement dette", kind = CategoryKind.EXPENSE, bucket = BudgetBucket.SAVINGS, emoji = "📉"),
        )
    }
}
