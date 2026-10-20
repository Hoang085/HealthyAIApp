package com.healthyai.app.presentation.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.healthyai.app.R
import com.healthyai.app.domain.model.StepCounterStatus
import com.healthyai.app.presentation.ui.component.InfoActionCard
import com.healthyai.app.presentation.ui.theme.HealthyAITheme

/** Thẻ xin quyền ACTIVITY_RECOGNITION để app tự đếm bước; kèm nút mở cài đặt vì Android có thể không hiện hộp thoại nữa. */
@Composable
fun StepCounterPermissionCard(
    status: StepCounterStatus,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        InfoActionCard(
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            title = stringResource(R.string.step_counter_title),
            description = stringResource(R.string.step_counter_description),
            actionLabel = stringResource(R.string.step_counter_action),
            onAction = onRequestPermission
        )

        if (!status.permissionGranted) {
            TextButton(
                onClick = onOpenAppSettings,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = stringResource(R.string.step_counter_manual_hint),
                    color = Color.Gray,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6FFF5)
@Composable
private fun StepCounterPermissionCardPreview() {
    HealthyAITheme {
        StepCounterPermissionCard(
            status = StepCounterStatus(sensorAvailable = true, permissionGranted = false),
            onRequestPermission = {},
            onOpenAppSettings = {},
            modifier = Modifier
        )
    }
}
