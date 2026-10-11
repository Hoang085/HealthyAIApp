package com.healthyai.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.healthyai.app.worker.ReminderNotifications
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class khởi tạo Dagger Hilt và WorkManager với HiltWorkerFactory.
 * Đây là entry point đầu tiên của ứng dụng.
 */
@HiltAndroidApp
class HiltApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /**
     * Tạo **kênh thông báo** nhắc nhở ngay lúc khởi động.
     *
     * Từ Android 8, app chưa có kênh nào thì trạng thái thông báo là **chưa xác định**, và
     * `areNotificationsEnabled()` trả kết quả khác nhau tuỳ bản Android/OEM. Thẻ "Bật nhắc nhở hàng
     * ngày" ở màn Trang chủ dựa vào tín hiệu đó, nên trạng thái chưa xác định làm hành vi đổi theo máy
     * (lỗi KB-36/37/38). Tạo kênh ở đây — thay vì đợi `ReminderWorker` chạy (tối đa 24 giờ) — để hành
     * vi giống nhau trên mọi phiên bản Android ngay từ lần mở app đầu tiên.
     */
    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.ensureChannel(this)
    }

    /**
     * Cung cấp WorkManager configuration với Hilt Worker Factory.
     * Cho phép WorkManager inject dependencies vào các Worker class.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
