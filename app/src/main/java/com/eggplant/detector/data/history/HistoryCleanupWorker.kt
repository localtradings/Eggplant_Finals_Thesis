package com.eggplant.detector.data.history

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import com.eggplant.detector.app.EggplantApplication

class HistoryCleanupWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val application = applicationContext as? EggplantApplication ?: return Result.failure()
        return runCatching {
            application.repository.cleanupExpiredHistory()
            Result.success()
        }.getOrElse { Result.retry() }
    }
}

object HistoryCleanupScheduler {
    private const val WORK_NAME = "eggplant-history-cleanup"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<HistoryCleanupWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
