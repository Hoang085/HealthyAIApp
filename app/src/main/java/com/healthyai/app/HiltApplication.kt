package com.healthyai.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
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
     * Cung cấp WorkManager configuration với Hilt Worker Factory.
     * Cho phép WorkManager inject dependencies vào các Worker class.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
