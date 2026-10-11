package com.healthyai.app.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.healthyai.app.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * ReminderWorker — Gửi thông báo nhắc nhở hàng ngày.
 * Hilt Worker: có thể inject bất kỳ dependency nào qua @AssistedInject.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME    = "daily_reminder_worker"
        private const val NOTIF_ID = 1001

        fun schedule(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build()
            )
        }
    }

    override suspend fun doWork(): Result {
        // Kênh đã được tạo lúc app khởi động (`HiltApplication.onCreate`); gọi lại cho chắc trong trường
        // hợp app chỉ chạy nền. Hàm này idempotent nên không tạo kênh trùng.
        ReminderNotifications.ensureChannel(context)
        showNotification()
        return Result.success()
    }

    private fun showNotification() {
        val notification = NotificationCompat.Builder(context, ReminderNotifications.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS permission chưa được cấp — bỏ qua
        }
    }
}
