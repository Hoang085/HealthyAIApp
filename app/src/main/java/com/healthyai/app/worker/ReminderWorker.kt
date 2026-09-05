package com.healthyai.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
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
        const val CHANNEL_ID   = "healthy_ai_reminders"
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
        createChannel()
        showNotification()
        return Result.success()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nhắc nhở thói quen sức khỏe",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Nhắc nhở hàng ngày cập nhật thói quen" }
            (context.getSystemService(NotificationManager::class.java))
                .createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("HealthyAI 💪 Nhắc nhở thói quen")
            .setContentText("Bạn đã cập nhật thói quen hôm nay chưa? Chỉ cần vài giây thôi!")
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
