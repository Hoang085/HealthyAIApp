package com.healthyai.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.healthyai.app.worker.ReminderNotifications
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** Application class khởi tạo Dagger Hilt và WorkManager. */
@HiltAndroidApp
class HiltApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /** Tạo kênh thông báo nhắc nhở ngay khi khởi động để hành vi thống nhất trên mọi máy, thay vì đợi ReminderWorker chạy. */
    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.ensureChannel(this)
    }

    /** Cung cấp WorkManager configuration với Hilt Worker Factory để inject dependencies vào Worker. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
