package com.retailone.pos.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.retailone.pos.repository.PosSaleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("SyncWorker", "🔄 Starting sync of offline sales...")

            val repository = PosSaleRepository(applicationContext)
            val syncResult = repository.syncOfflineSales()

            return@withContext if (syncResult) {
                Log.d("SyncWorker", "✅ Sync completed successfully")

                // ⚡ Schedule another network-triggered sync for next time
                scheduleNetworkTriggeredSync(applicationContext)

                Result.success()
            } else {
                Log.d("SyncWorker", "⚠️ Sync failed, will retry")
                Result.retry()
            }

        } catch (e: Exception) {
            Log.e("SyncWorker", "❌ Sync error: ${e.message}")
            return@withContext Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "SyncOfflineSalesWork"
        const val NETWORK_SYNC_WORK = "NetworkTriggeredSyncWork"

        /**
         * Schedule both periodic sync AND network-triggered sync
         */
        fun scheduleSync(context: Context) {
            // 1. Periodic sync (every 15 minutes as backup)
            schedulePeriodicSync(context)

            // 2. Network-triggered sync (immediate when internet connects)
            scheduleNetworkTriggeredSync(context)

            Log.d("SyncWorker", "✅ Sync setup complete: 15min periodic + instant network trigger")
        }

        /**
         * Periodic sync every 15 minutes (backup)
         */
        private fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10, TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }

        /**
         * One-time sync that triggers when network becomes available
         * This provides instant sync when internet connects
         */
        private fun scheduleNetworkTriggeredSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .addTag("network_triggered")
                .build()

            // Use KEEP policy so it doesn't duplicate if already scheduled
            WorkManager.getInstance(context).enqueueUniqueWork(
                NETWORK_SYNC_WORK,
                ExistingWorkPolicy.KEEP,
                syncRequest
            )

            Log.d("SyncWorker", "⚡ Network-triggered sync scheduled (runs when internet connects)")
        }

        /**
         * Manual sync trigger (for UI buttons)
         */
        fun syncNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(syncRequest)
            Log.d("SyncWorker", "🔵 Manual sync requested")
        }
    }
}
