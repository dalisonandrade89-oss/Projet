package com.projet.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.projet.app.data.repository.SyncRepository
import com.projet.app.data.repository.SyncResult
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val syncRepository: SyncRepository by inject()

    override suspend fun doWork(): Result = when (syncRepository.sync()) {
        // Sem consentimento ainda: nada a fazer em background (a UI pedirá ao usuário).
        SyncResult.Success, is SyncResult.NeedsConsent -> Result.success()
        is SyncResult.Error -> if (runAttemptCount < 3) Result.retry() else Result.failure()
    }
}
