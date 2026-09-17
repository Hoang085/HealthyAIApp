package com.healthyai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.work.WorkManager
import com.healthyai.app.presentation.navigation.AppNavigation
import com.healthyai.app.presentation.ui.theme.HealthyAITheme
import com.healthyai.app.worker.HealthSyncWorker
import com.healthyai.app.worker.ReminderWorker
import com.healthyai.app.worker.SyncWorker
import dagger.hilt.android.AndroidEntryPoint

/**
 * Màn hình duy nhất (Single Activity Architecture).
 * Toàn bộ UI được xây dựng bằng Jetpack Compose bên trong.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Khởi tạo các background tasks
        val workManager = WorkManager.getInstance(applicationContext)
        HealthSyncWorker.schedule(workManager)
        SyncWorker.schedule(workManager)
        ReminderWorker.schedule(workManager)

        enableEdgeToEdge()
        setContent {
            HealthyAITheme {
                AppNavigation()
            }
        }
    }
}
