package com.healthyai.app.presentation.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.healthyai.app.domain.model.StepCounterStatus
import com.healthyai.app.presentation.ui.component.InfoActionCard
import com.healthyai.app.presentation.ui.theme.HealthyAITheme

/**
 * Thẻ xin quyền `ACTIVITY_RECOGNITION` để app **tự đếm bước** bằng cảm biến của điện thoại.
 *
 * Chỉ hiển thị khi máy **có** cảm biến đếm bước nhưng app chưa được cấp quyền
 * (xem `HomeUiState.showStepCounterCard`).
 *
 * Nếu người dùng đã từ chối quá số lần cho phép, Android sẽ **không hiện hộp thoại nữa** — vì vậy
 * thẻ luôn kèm nút phụ mở thẳng phần cài đặt quyền của app.
 */
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
            title = "Bật đếm bước chân",
            description = "HealthyAI đọc số bước trực tiếp từ cảm biến đếm bước của điện thoại — " +
                "không cần cài thêm ứng dụng sức khỏe nào. Bật lên để thẻ \"Tóm tắt sức khỏe\" " +
                "có số bước của bạn.",
            actionLabel = "Bật đếm bước chân",
            onAction = onRequestPermission
        )

        if (!status.permissionGranted) {
            TextButton(
                onClick = onOpenAppSettings,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "Không thấy hộp thoại? Mở cài đặt quyền",
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
