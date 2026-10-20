package com.healthyai.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.healthyai.app.data.firebase.FirestoreSyncDataSource
import com.healthyai.app.domain.repository.AuthRepository
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.repository.SettingsRepository
import com.healthyai.app.domain.usecase.sync.CloudSyncPolicy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/** Đẩy dữ liệu chưa đồng bộ lên Firestore mỗi 6 giờ; chỉ tải khi đã bật đồng bộ đám mây và log tạo sau mốc đồng ý. */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val habitRepository: HabitRepository,
    private val firestoreSyncDataSource: FirestoreSyncDataSource,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userId = authRepository.currentUserId
        if (userId == null) {
            android.util.Log.d("SyncWorker", "Sync skipped: chưa đăng nhập (chế độ khách)")
            return Result.success()
        }

        return try {
            val cloudSync = settingsRepository.getCloudSync()
            val logsToUpload = CloudSyncPolicy.logsToUpload(
                logs = habitRepository.getUnsyncedHabitLogs(),
                settings = cloudSync
            )

            if (logsToUpload.isEmpty()) {
                android.util.Log.d(
                    "SyncWorker",
                    "Sync skipped: chưa bật đồng bộ đám mây hoặc không có log nào tạo sau khi đồng ý"
                )
                return Result.success()
            }

            logsToUpload.forEach { log ->
                firestoreSyncDataSource.uploadHabitLog(userId, log)
                    .onSuccess {
                        habitRepository.markHabitLogSynced(log.id)
                    }
                    .onFailure { throw it }
            }

            android.util.Log.d("SyncWorker", "Sync completed successfully: ${logsToUpload.size} logs")
            Result.success()
        } catch (e: CancellationException) {
            // Worker bị dừng = huỷ coroutine → ném tiếp, không biến thành "thử lại/thất bại".
            throw e
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
                // Không dùng setRequiresDeviceIdle(true): kết hợp với setBackoffCriteria() gây IllegalArgumentException → crash.
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
