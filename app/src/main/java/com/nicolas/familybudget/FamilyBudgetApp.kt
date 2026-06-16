package com.nicolas.familybudget

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.nicolas.familybudget.data.sync.supabase.SupabaseConfig
import com.nicolas.familybudget.data.sync.supabase.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FamilyBudgetApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Programme la synchro periodique seulement si le backend est configure.
        if (SupabaseConfig.isConfigured) {
            SyncWorker.schedulePeriodic(this)
        }
    }
}
