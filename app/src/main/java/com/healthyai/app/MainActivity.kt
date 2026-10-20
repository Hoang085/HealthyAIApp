package com.healthyai.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.work.WorkManager
import com.healthyai.app.domain.widget.HealthWidgetRefresher
import com.healthyai.app.presentation.navigation.AppNavigation
import com.healthyai.app.presentation.screen.privacy.PrivacyPolicyScreen
import com.healthyai.app.presentation.ui.theme.HealthyAITheme
import com.healthyai.app.worker.HealthSyncWorker
import com.healthyai.app.worker.ReminderWorker
import com.healthyai.app.worker.SleepTrackingWorker
import com.healthyai.app.worker.SyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Màn hình duy nhất (Single Activity Architecture), toàn bộ UI dựng bằng Jetpack Compose. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var healthWidgetRefresher: HealthWidgetRefresher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo các background tasks
        val workManager = WorkManager.getInstance(applicationContext)
        HealthSyncWorker.schedule(workManager)
        SyncWorker.schedule(workManager)
        ReminderWorker.schedule(workManager)
        SleepTrackingWorker.schedule(workManager)

        // Màn rationale của Health Connect phải nằm ngoài cổng đăng nhập, nếu không Health Connect sẽ từ chối duyệt.
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
     * Rời app (về màn hình chính / chuyển app) chính là lúc người dùng sắp nhìn widget ⇒ vẽ lại ngay
     * với số mới nhất, không chờ nhịp 1 phút của bước chân hay nhịp 30 phút của hệ thống.
     * Bỏ qua khi chỉ xoay màn hình (Activity dựng lại, người dùng vẫn ở trong app).
     */
    override fun onStop() {
        super.onStop()
        if (isChangingConfigurations) return
        widgetRefreshScope.launch { healthWidgetRefresher.refresh() }
    }

    /** Intent này có phải yêu cầu hiển thị rationale của Health Connect không. */
    private fun Intent.isHealthPermissionsRationale(): Boolean =
        action == ACTION_SHOW_PERMISSIONS_RATIONALE || action == ACTION_VIEW_PERMISSION_USAGE

    private companion object {
        /** `androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE` — dùng chuỗi để khớp manifest. */
        const val ACTION_SHOW_PERMISSIONS_RATIONALE =
            "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"

        /** `android.intent.action.VIEW_PERMISSION_USAGE` — đường vào trên Android 14+. */
        const val ACTION_VIEW_PERMISSION_USAGE =
            "android.intent.action.VIEW_PERMISSION_USAGE"

        /**
         * Scope sống theo tiến trình cho lần vẽ widget lúc `onStop`: không dùng `lifecycleScope` vì
         * Activity có thể bị huỷ ngay sau `onStop` (vuốt tắt app) làm lần vẽ bị huỷ giữa chừng.
         * `refresh()` tự bắt mọi lỗi; `SupervisorJob` để một lần lỗi không khoá các lần sau.
         */
        val widgetRefreshScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
