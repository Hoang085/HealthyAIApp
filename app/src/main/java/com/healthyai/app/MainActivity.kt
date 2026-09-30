package com.healthyai.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.work.WorkManager
import com.healthyai.app.presentation.navigation.AppNavigation
import com.healthyai.app.presentation.screen.privacy.PrivacyPolicyScreen
import com.healthyai.app.presentation.ui.theme.HealthyAITheme
import com.healthyai.app.worker.HealthSyncWorker
import com.healthyai.app.worker.ReminderWorker
import com.healthyai.app.worker.SleepTrackingWorker
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
        SleepTrackingWorker.schedule(workManager)

        // Health Connect gọi app để hiển thị chính sách bảo mật (rationale) trước khi xin quyền.
        // Màn này KHÔNG được nằm sau cổng đăng nhập, nếu không Health Connect sẽ thấy app hiện
        // màn Đăng nhập và từ chối duyệt (mục 3 #31).
        val showPrivacyRationale = intent.isHealthPermissionsRationale()

        enableEdgeToEdge()
        setContent {
            HealthyAITheme {
                if (showPrivacyRationale) {
                    PrivacyPolicyScreen(onBack = { finish() })
                } else {
                    AppNavigation()
                }
            }
        }
    }

    /**
     * Intent này có phải yêu cầu hiển thị chính sách bảo mật của Health Connect không.
     *
     * Hai hành động khớp với hai `intent-filter` trong `AndroidManifest.xml`:
     * `ACTION_SHOW_PERMISSIONS_RATIONALE` (Android < 14) và `ACTION_VIEW_PERMISSION_USAGE` (Android 14+,
     * qua `activity-alias` cùng trỏ vào MainActivity).
     */
    private fun Intent.isHealthPermissionsRationale(): Boolean =
        action == ACTION_SHOW_PERMISSIONS_RATIONALE || action == ACTION_VIEW_PERMISSION_USAGE

    private companion object {
        /** `androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE` — dùng chuỗi để khớp manifest. */
        const val ACTION_SHOW_PERMISSIONS_RATIONALE =
            "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"

        /** `android.intent.action.VIEW_PERMISSION_USAGE` — đường vào trên Android 14+. */
        const val ACTION_VIEW_PERMISSION_USAGE =
            "android.intent.action.VIEW_PERMISSION_USAGE"
    }
}
