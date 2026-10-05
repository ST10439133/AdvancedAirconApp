//Android Developers. 2026. PeriodicWorkRequest | API reference. [Online]. Available at: https://developer.android.com/reference/kotlin/androidx/work/PeriodicWorkRequest [Accessed: 5 October 2026].
//Android Developers. 2026. Constraints.Builder | API reference. [Online]. Available at: https://developer.android.com/reference/androidx/work/Constraints.Builder [Accessed: 5 October 2026].
//Android Developers. 2026. Background work. [Online]. Available at: https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work [Accessed: 5 October 2026].

package com.insy7315.advancedairconapp.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        SyncManager.drain(applicationContext)
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    companion object {
        private const val UNIQUE_NAME = "arcticflow_sync_worker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}