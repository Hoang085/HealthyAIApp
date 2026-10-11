package com.healthyai.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.healthyai.app.domain.repository.HealthDataRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
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
            val syncResult = healthDataRepository.syncHealthData()

            syncResult
                .onSuccess {
                    android.util.Log.d("HealthSyncWorker", "Periodic sync successful")
                }
                .onFailure { e ->
                    android.util.Log.e("HealthSyncWorker", "Periodic sync failed: ${e.message}")
                }

            if (syncResult.isSuccess) {
                Result.success()
            } else {
                // Đồng bộ thất bại thật (đọc cảm biến/ghi DB lỗi) → trả `retry` để WorkManager dùng
                // backoff đã đặt thay vì luôn "thành công" khiến backoff không bao giờ chạy
                // (phụ lục C #43).
                retryOrFail()
            }
        } catch (e: CancellationException) {
            // WorkManager dừng worker = huỷ coroutine → phải ném tiếp, KHÔNG được biến thành
            // "thử lại/thất bại" (phụ lục C #39).
            throw e
        } catch (e: Exception) {
            android.util.Log.e("HealthSyncWorker", "Periodic sync threw: ${e.message}")
            retryOrFail()
        }
    }

    /** Thử lại tối đa [MAX_ATTEMPTS] lần rồi báo thất bại hẳn (backoff do [schedule] đặt). */
    private fun retryOrFail(): Result =
        if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()

    companion object {
        const val WORK_NAME = "periodic_health_sync_worker"

        /** Số lần thử tối đa trước khi báo thất bại hẳn. */
        private const val MAX_ATTEMPTS = 3

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
