package com.healthyai.app.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.ui.graphics.vector.ImageVector
import com.healthyai.app.R

/** Một destination của NavHost; titleRes là id tài nguyên để nhãn hiển thị theo ngôn ngữ hệ thống. */
sealed class Screen(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    object Home     : Screen("home",     R.string.nav_home,     Icons.Filled.Home)
    object Habits   : Screen("habits",   R.string.nav_habits,   Icons.Filled.CheckCircle)
    object Camera   : Screen("camera",   R.string.nav_camera,   Icons.Filled.CameraAlt)
    object Insights : Screen("insights", R.string.nav_insights, Icons.Filled.AutoAwesome)

    // Màn hình không nằm trong Bottom Nav
    object AddEditHabit : Screen("add_edit_habit?habitId={habitId}", R.string.nav_habits, Icons.Filled.CheckCircle) {
        fun createRoute(habitId: Long? = null) = "add_edit_habit?habitId=${habitId ?: -1L}"
    }

    /** Đo nhịp tim bằng camera + đèn flash — mở từ ô "Tim" ở thẻ "Tóm tắt sức khỏe". */
    object MeasureHeartRate : Screen("measure_heart_rate", R.string.nav_heart_rate, Icons.Filled.Favorite)

    /** Chính sách bảo mật — mở từ thẻ Đồng bộ đám mây ở Trang chủ và khi Health Connect gửi ACTION_SHOW_PERMISSIONS_RATIONALE. */
    object PrivacyPolicy : Screen("privacy_policy", R.string.nav_privacy_policy, Icons.Filled.PrivacyTip)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Habits,
    Screen.Camera,
    Screen.Insights
)
