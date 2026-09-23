package com.healthyai.app.presentation.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.presentation.ui.component.InfoActionCard
import com.healthyai.app.presentation.ui.theme.HealthyAITheme

/**
 * Thẻ hướng dẫn người dùng khi app chưa đọc được dữ liệu sức khỏe:
 * cấp quyền đọc Health Connect, cài mới, hoặc cập nhật Health Connect.
 *
 * Người gọi chỉ nên hiển thị thẻ này khi `HomeUiState.showHealthConnectCard == true`.
 */
@Composable
fun HealthConnectPermissionCard(
    status: HealthConnectStatus,
    onRequestPermission: () -> Unit,
    onOpenHealthConnectStore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val card = when (status) {
        HealthConnectStatus.UPDATE_REQUIRED -> HealthConnectCardCopy(
            icon = Icons.Default.SystemUpdate,
            title = "Health Connect cần được cập nhật",
            description = "Phiên bản Health Connect trên máy đã cũ nên HealthyAI chưa đọc được dữ liệu. " +
                "Hãy cập nhật để xem bước chân, giấc ngủ và nhịp tim.",
            actionLabel = "Cập nhật Health Connect",
            action = onOpenHealthConnectStore
        )

        HealthConnectStatus.NOT_INSTALLED -> HealthConnectCardCopy(
            icon = Icons.Default.HealthAndSafety,
            title = "Chưa cài Health Connect",
            description = "Cài Health Connect để HealthyAI đọc bước chân, giấc ngủ và nhịp tim. " +
                "Hiện tại các chỉ số đang hiển thị 0.",
            actionLabel = "Cài Health Connect",
            action = onOpenHealthConnectStore
        )

        else -> HealthConnectCardCopy(
            icon = Icons.Default.Favorite,
            title = "Cấp quyền Health Connect",
            description = "HealthyAI cần quyền đọc Bước chân, Giấc ngủ, Nhịp tim và Calories " +
                "để hiển thị chỉ số sức khỏe của bạn.",
            actionLabel = "Cấp quyền Health Connect",
            action = onRequestPermission
        )
    }

    InfoActionCard(
        icon = card.icon,
        title = card.title,
        description = card.description,
        actionLabel = card.actionLabel,
        onAction = card.action,
        modifier = modifier
    )
}

/** Nội dung hiển thị của thẻ theo từng trạng thái Health Connect. */
private data class HealthConnectCardCopy(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val actionLabel: String,
    val action: () -> Unit
)

@Preview(showBackground = true, backgroundColor = 0xFFF6FFF5)
@Composable
private fun HealthConnectPermissionCardPreview() {
    HealthyAITheme {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HealthConnectPermissionCard(
                status = HealthConnectStatus.AVAILABLE,
                onRequestPermission = {},
                onOpenHealthConnectStore = {}
            )
            HealthConnectPermissionCard(
                status = HealthConnectStatus.NOT_INSTALLED,
                onRequestPermission = {},
                onOpenHealthConnectStore = {}
            )
        }
    }
}
