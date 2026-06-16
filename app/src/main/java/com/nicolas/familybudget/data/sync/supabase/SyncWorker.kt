package com.nicolas.familybudget.data.sync.supabase

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Synchronise le budget courant en arriere-plan. Reseau requis. En cas d'echec
 * (hors-ligne, erreur transitoire), WorkManager re-essaie ; les lignes restees
 * isDirty repartiront de toute facon au prochain passage.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: BudgetSyncRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        syncRepository.syncCurrent()
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    companion object {
        private const val PERIODIC = "sync_periodic"
        private const val ONESHOT = "sync_now"

        private val networkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Sync periodique (toutes les ~6 h). A appeler une fois au demarrage. */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(networkConstraint)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        /** Sync immediate (bouton manuel / apres une ecriture importante). */
        fun syncNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(networkConstraint)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
