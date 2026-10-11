package com.healthyai.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.healthyai.app.domain.repository.SleepTrackerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/**
 * SleepTrackingWorker — **tự đo giấc ngủ** bằng cảm biến của điện thoại, chạy định kỳ.
 *
 * App **không** tự quyết định giờ ngủ: mỗi lần chạy, worker đọc **khung giờ ngủ do người dùng đặt** từ
 * `SettingsRepository` (bảng Room `app_settings`) rồi chỉ đo trong khung đó.
 *
 * Vì sao cần worker (khác phần đếm bước chân): `TYPE_STEP_COUNTER` đếm ở **phần cứng** nên số bước
 * không mất khi app đóng, còn giấc ngủ thì **không có cảm biến sẵn** — app phải tự lấy mẫu. Mỗi lần
 * chạy chỉ nghe cảm biến trong vài giây (`SleepSensor.DEFAULT_SAMPLE_WINDOW_MS`) rồi gỡ listener, nên
 * chạy thưa (15 phút/lần) vẫn tiết kiệm pin và **không cần foreground service**.
 *
 * ⚠️ **Giới hạn đã biết:** WorkManager có thể bị **hoãn trong chế độ Doze** (ban đêm máy không sạc,
 * đứng yên) → khoảng cách giữa hai mẫu có thể là vài giờ. Phép cộng dồn trong `SleepTrackerMath`
 * cố tình tính **toàn bộ** khoảng giữa hai mẫu "đang ngủ" liên tiếp để không mất thời gian ngủ vì lý
 * do này. Muốn chính xác theo pha (Sâu/Nông/REM) thì cần đồng hồ đeo tay — app vẫn ưu tiên Health
 * Connect khi có dữ liệu.
 */
@HiltWorker
class SleepTrackingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val sleepTrackerRepository: SleepTrackerRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val minutes = sleepTrackerRepository.sampleAndStore()
            android.util.Log.d(TAG, "Lấy mẫu giấc ngủ xong: $minutes phút cho đêm đang xét")

            Result.success()
        } catch (e: CancellationException) {
            // Worker bị dừng = huỷ coroutine → ném tiếp (phụ lục C #39).
            throw e
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Lấy mẫu giấc ngủ thất bại: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "periodic_sleep_tracking_worker"

        /** 15 phút là khoảng định kỳ **nhỏ nhất** WorkManager cho phép. */
        private const val INTERVAL_MINUTES = 15L

        private const val TAG = "SleepTrackingWorker"

        /**
         * Lên lịch lấy mẫu mỗi 15 phút.
         *
         * - `KEEP`: app đóng rồi mở lại không tạo job trùng (giống `HealthSyncWorker`).
         * - Không yêu cầu mạng (đọc cảm biến là hoàn toàn offline).
         * - `setRequiresBatteryNotLow(true)`: không đánh thức máy khi pin yếu; khoảng giữa hai mẫu
         *   vẫn được tính nên không mất dữ liệu giấc ngủ.
         */
        fun schedule(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<SleepTrackingWorker>(
                INTERVAL_MINUTES,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .addTag("sleep_tracking")
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
