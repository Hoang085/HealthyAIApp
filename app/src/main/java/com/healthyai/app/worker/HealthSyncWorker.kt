package com.healthyai.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.healthyai.app.domain.repository.HealthDataRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/** Đồng bộ dữ liệu từ Health Connect định kỳ, tôn trọng giới hạn pin/tài nguyên. */
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
                // Thất bại thật → retry để WorkManager dùng backoff đã đặt (tránh backoff không bao giờ chạy).
                retryOrFail()
            }
        } catch (e: CancellationException) {
            // Worker bị dừng = huỷ coroutine → ném tiếp, không biến thành "thử lại/thất bại".
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

        /** Lên lịch mỗi 6 giờ; chỉ chạy khi pin không thấp, hệ thống có thể hoãn trong Doze. */
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
