package com.nicolas.familybudget.di

import android.content.Context
import androidx.room.Room
import com.nicolas.familybudget.data.local.AccountDao
import com.nicolas.familybudget.data.local.AppDatabase
import com.nicolas.familybudget.data.local.CategoryDao
import com.nicolas.familybudget.data.local.FamilyMemberDao
import com.nicolas.familybudget.data.local.GoalDao
import com.nicolas.familybudget.data.local.TransactionDao
import com.nicolas.familybudget.data.sync.BankSyncProvider
import com.nicolas.familybudget.data.sync.MockBankSyncProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun accountDao(db: AppDatabase): AccountDao = db.accountDao()
    @Provides fun transactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun categoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun goalDao(db: AppDatabase): GoalDao = db.goalDao()
    @Provides fun familyMemberDao(db: AppDatabase): FamilyMemberDao = db.familyMemberDao()

    /**
     * Provider de synchro par defaut = mock hors-ligne.
     * Pour activer une vraie synchro DSP2, remplace par ton implementation
     * (ex. PowensBankSyncProvider) ici.
     */
    @Provides
    @Singleton
    fun provideBankSyncProvider(): BankSyncProvider = MockBankSyncProvider()
}
