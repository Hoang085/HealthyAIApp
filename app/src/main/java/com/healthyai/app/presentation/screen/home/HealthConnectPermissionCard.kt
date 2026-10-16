package com.healthyai.app.presentation.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.healthyai.app.R
import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.presentation.ui.component.InfoActionCard
import com.healthyai.app.presentation.ui.theme.HealthyAITheme

/** Thẻ hướng dẫn cấp quyền / cài / cập nhật Health Connect khi app chưa đọc được dữ liệu; chỉ hiện khi showHealthConnectCard == true. */
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
            title = stringResource(R.string.health_connect_update_title),
            description = stringResource(R.string.health_connect_update_description),
            actionLabel = stringResource(R.string.health_connect_update_action),
            action = onOpenHealthConnectStore
        )

        HealthConnectStatus.NOT_INSTALLED -> HealthConnectCardCopy(
            icon = Icons.Default.HealthAndSafety,
            title = stringResource(R.string.health_connect_missing_title),
            description = stringResource(R.string.health_connect_missing_description),
            actionLabel = stringResource(R.string.health_connect_missing_action),
            action = onOpenHealthConnectStore
        )

        else -> HealthConnectCardCopy(
            icon = Icons.Default.Favorite,
            title = stringResource(R.string.health_connect_permission_title),
            description = stringResource(R.string.health_connect_permission_description),
            actionLabel = stringResource(R.string.health_connect_permission_action),
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
