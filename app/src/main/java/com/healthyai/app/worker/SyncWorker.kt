package com.healthyai.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * SyncWorker — Đẩy dữ liệu chưa đồng bộ (isDirty = true) lên Firebase Firestore.
 * Chạy ngầm mỗi 6 giờ, chỉ khi có kết nối mạng.
 * Retry tối đa 3 lần với Exponential Backoff nếu thất bại.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    // TODO: Inject HabitRepository khi bật Firebase
    // private val habitRepository: HabitRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // TODO (Module 2 — Firebase):
            //   1. val unsyncedLogs = habitRepository.getUnsyncedLogs()
            //   2. firestore.collection("users/{uid}/habit_logs").add(log)
            //   3. habitRepository.markAsSynced(log.id) → isDirty = false
            //
            // Last Write Wins conflict resolution:
            //   Mỗi log có timestamp loggedAt → bên nào có timestamp mới hơn thắng.
            android.util.Log.d("SyncWorker", "Sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "Sync failed: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "periodic_sync_worker"

        fun schedule(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .setRequiresDeviceIdle(true) // Sync when user is not actively using device
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                    .build()
            )
        }
    }
}
