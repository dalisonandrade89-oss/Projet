package com.projet.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Agenda o [SyncWorker]: sync "logo após editar" (com atraso/coalescência) e sync periódico. */
class SyncScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context)
    private val needsNetwork = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** REPLACE + atraso de 10s: várias edições seguidas resultam em um único sync. */
    fun schedule() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(needsNetwork)
            .setInitialDelay(10, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork("projet-sync-now", ExistingWorkPolicy.REPLACE, request)
    }

    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(needsNetwork)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "projet-sync-periodic", ExistingPeriodicWorkPolicy.KEEP, request
        )
    }
}
