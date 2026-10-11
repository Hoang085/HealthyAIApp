package com.healthyai.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.healthyai.app.R

/**
 * Nguồn duy nhất định nghĩa **kênh thông báo** nhắc nhở thói quen.
 *
 * ### Vì sao phải tạo kênh ngay lúc app khởi động
 *
 * Trước đây kênh chỉ được tạo bên trong `ReminderWorker.doWork()` — tức là **tối đa 24 giờ** sau lần
 * mở app đầu tiên (chu kỳ của worker). Từ Android 8 (API 26), thông báo **bắt buộc** thuộc một kênh, và
 * khi app chưa có kênh nào thì trạng thái thông báo của app là **chưa xác định**: các bản Android/OEM
 * khác nhau trả kết quả khác nhau cho `NotificationManagerCompat.areNotificationsEnabled()`.
 *
 * Đó là tín hiệu mà thẻ "Bật nhắc nhở hàng ngày" dựa vào để quyết định hiện/ẩn (`HomeUiState.
 * showReminderNotificationCard`), nên trạng thái chưa xác định làm hành vi **khác nhau theo máy** —
 * đúng thứ cần loại bỏ để luồng chạy đúng trên mọi phiên bản Android. Tạo kênh ngay trong
 * `HiltApplication.onCreate()` khiến trạng thái này xác định từ lần mở app đầu tiên, và cũng đúng cách
 * làm theo khuyến nghị của Android (kênh nên được tạo lúc khởi động, không phải lúc gửi thông báo).
 *
 * Hàm **idempotent**: gọi lại nhiều lần vô hại — `createNotificationChannel` với cùng một id chỉ **cập
 * nhật** kênh đã có, không tạo trùng và không xoá lịch sử thông báo của kênh.
 */
object ReminderNotifications {

    /** Id kênh thông báo nhắc nhở. Đổi id này sẽ tạo kênh mới và mất cài đặt cũ của người dùng. */
    const val CHANNEL_ID = "healthy_ai_reminders"

    /**
     * Bảo đảm kênh nhắc nhở tồn tại. Gọi ở `Application.onCreate()`, và gọi lại (vô hại) trước khi gửi
     * thông báo trong `ReminderWorker` để phòng trường hợp app chạy nền mà chưa từng mở màn hình.
     *
     * Không cần kiểm `Build.VERSION.SDK_INT`: `minSdk` của app là 26 nên `NotificationChannel` luôn có
     * mặt.
     */
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
