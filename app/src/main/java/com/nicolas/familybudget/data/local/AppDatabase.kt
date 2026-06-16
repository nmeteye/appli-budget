package com.nicolas.familybudget.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        GoalEntity::class,
        FamilyMemberEntity::class,
    ],
    version = 2,
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

        /**
         * v1 -> v2 : ajout des metadonnees de synchronisation multi-appareils.
         * Le backfill de syncId utilise randomblob() qui est evalue PAR LIGNE en
         * SQLite, donc chaque ligne existante recoit bien un UUID-like distinct.
         * isDirty = 1 sur l'existant pour que tout remonte au premier sync.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                for (table in listOf("accounts", "transactions", "categories")) {
                    db.execSQL("ALTER TABLE $table ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE $table ADD COLUMN budgetId TEXT")
                    db.execSQL("ALTER TABLE $table ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE $table ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 1")
                    db.execSQL("ALTER TABLE $table ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("UPDATE $table SET syncId = lower(hex(randomblob(16))), updatedAt = strftime('%s','now') * 1000")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_${table}_syncId ON $table(syncId)")
                }
            }
        }

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
