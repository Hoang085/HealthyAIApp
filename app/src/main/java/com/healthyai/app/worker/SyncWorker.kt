package com.healthyai.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.healthyai.app.data.firebase.FirestoreSyncDataSource
import com.healthyai.app.domain.repository.HabitRepository
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
    private val habitRepository: HabitRepository,
    private val firestoreSyncDataSource: FirestoreSyncDataSource,
    private val auth: FirebaseAuth
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userId = auth.currentUser?.uid ?: return Result.failure()

        return try {
            val unsyncedLogs = habitRepository.getUnsyncedHabitLogs()
            
            unsyncedLogs.forEach { log ->
                firestoreSyncDataSource.uploadHabitLog(userId, log)
                    .onSuccess {
                        habitRepository.markHabitLogSynced(log.id)
                    }
                    .onFailure { throw it }
            }

            android.util.Log.d("SyncWorker", "Sync completed successfully: ${unsyncedLogs.size} logs")
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
                // KHÔNG dùng setRequiresDeviceIdle(true) ở đây: WorkManager ném
                // IllegalArgumentException("Cannot set backoff criteria on an idle mode job")
                // khi vừa requiresDeviceIdle(true) vừa setBackoffCriteria() → crash ngay khi mở app.
                // Giữ backoff (như KDoc mô tả) và để hệ thống tự hoãn khi máy vào Doze.
                .setRequiresDeviceIdle(false)
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
