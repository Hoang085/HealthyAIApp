package com.healthyai.app.presentation.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.healthyai.app.R
import com.healthyai.app.domain.model.NotificationPermissionStatus
import com.healthyai.app.presentation.ui.component.InfoActionCard
import com.healthyai.app.presentation.ui.theme.HealthyAITheme

/**
 * Thẻ nhắc người dùng bật thông báo để `ReminderWorker` gửi được nhắc nhở hàng ngày.
 *
 * - Còn xin được quyền runtime `POST_NOTIFICATIONS` (Android 13+) → nút **"Bật thông báo"**.
 * - Đã cấp quyền nhưng người dùng tắt thông báo trong Settings (hoặc Android ≤ 12) →
 *   nút **"Mở cài đặt thông báo"**.
 *
 * Người gọi chỉ nên hiển thị thẻ này khi `HomeUiState.showReminderNotificationCard == true`.
 */
@Composable
fun ReminderNotificationCard(
    status: NotificationPermissionStatus,
    onRequestNotificationPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (status.canRequestPermission) {
        InfoActionCard(
            icon = Icons.Default.NotificationsActive,
            title = stringResource(R.string.reminder_enable_title),
            description = stringResource(R.string.reminder_enable_description),
            actionLabel = stringResource(R.string.reminder_enable_action),
            onAction = onRequestNotificationPermission,
            modifier = modifier
        )
    } else {
        InfoActionCard(
            icon = Icons.Default.NotificationsOff,
            title = stringResource(R.string.reminder_disabled_title),
            description = stringResource(R.string.reminder_disabled_description),
            actionLabel = stringResource(R.string.reminder_disabled_action),
            onAction = onOpenNotificationSettings,
            modifier = modifier
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6FFF5)
@Composable
private fun ReminderNotificationCardPreview() {
    HealthyAITheme {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ReminderNotificationCard(
                status = NotificationPermissionStatus(
                    notificationsEnabled = false,
                    canRequestPermission = true
                ),
                onRequestNotificationPermission = {},
                onOpenNotificationSettings = {}
            )
            ReminderNotificationCard(
                status = NotificationPermissionStatus(
                    notificationsEnabled = false,
                    canRequestPermission = false
                ),
                onRequestNotificationPermission = {},
                onOpenNotificationSettings = {}
            )
        }
    }
}
