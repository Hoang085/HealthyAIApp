package com.healthyai.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.healthyai.app.domain.repository.HealthDataRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * HealthSyncWorker — Đồng bộ dữ liệu từ Health Connect định kỳ.
 * Tuân thủ các hạn chế về pin và tài nguyên của hệ thống.
 */
@HiltWorker
class HealthSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val healthDataRepository: HealthDataRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            healthDataRepository.syncHealthData()
                .onSuccess { 
                    android.util.Log.d("HealthSyncWorker", "Periodic sync successful")
                }
                .onFailure { e ->
                    android.util.Log.e("HealthSyncWorker", "Periodic sync failed: ${e.message}")
                }
            
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "periodic_health_sync_worker"

        /**
         * Lên lịch chạy định kỳ mỗi 6 giờ.
         * Chú ý Battery Restrictions:
         * - Chỉ chạy khi Pin không thấp (setRequiresBatteryNotLow).
         * - Chỉ chạy khi thiết bị đang sạc (tùy chọn, ở đây chọn không thấp là đủ).
         * - Hệ thống có thể delay task nếu đang ở chế độ Doze.
         */
        fun schedule(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Health Connect là offline
                .setRequiresBatteryNotLow(true)
                .setRequiresDeviceIdle(false) // Không cần idle để dữ liệu cập nhật thường xuyên hơn chút
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<HealthSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .addTag("health_sync")
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Giữ worker cũ nếu đã tồn tại
                syncRequest
            )
        }
    }
}
