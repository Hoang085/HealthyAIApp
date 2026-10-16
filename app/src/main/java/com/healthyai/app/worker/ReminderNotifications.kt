package com.healthyai.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.healthyai.app.R

/** Nguồn duy nhất định nghĩa kênh thông báo nhắc nhở; tạo ngay lúc app khởi động. */
object ReminderNotifications {

    /** Id kênh thông báo nhắc nhở. Đổi id này sẽ tạo kênh mới và mất cài đặt cũ của người dùng. */
    const val CHANNEL_ID = "healthy_ai_reminders"

    /** Bảo đảm kênh nhắc nhở tồn tại; idempotent nên gọi lại vô hại. */
    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.notification_channel_description) }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}
