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

/** Tự đo giấc ngủ bằng cảm biến điện thoại, chạy định kỳ; mỗi lần chỉ nghe cảm biến vài giây. */
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
            // Worker bị dừng = huỷ coroutine → ném tiếp.
            throw e
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Lấy mẫu giấc ngủ thất bại: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "periodic_sleep_tracking_worker"

        /** 15 phút là khoảng định kỳ nhỏ nhất WorkManager cho phép. */
        private const val INTERVAL_MINUTES = 15L

        private const val TAG = "SleepTrackingWorker"

        /** Lên lịch lấy mẫu mỗi 15 phút; không cần mạng và không chạy khi pin yếu. */
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
