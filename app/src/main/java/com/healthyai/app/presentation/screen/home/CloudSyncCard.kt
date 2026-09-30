package com.healthyai.app.presentation.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.healthyai.app.R
import com.healthyai.app.domain.model.CloudSyncSettings
import com.healthyai.app.presentation.ui.theme.HealthyAITheme

private val CardGreen = Color(0xFF2E7D32)

/**
 * Thẻ **"Đồng bộ đám mây"** — nơi duy nhất bật/tắt việc tải nhật ký thói quen lên Firestore.
 *
 * **Mặc định TẮT** (mục 3 #31): chưa bật thì app chỉ lưu dữ liệu trên máy. Khi bật, app ghi một
 * **mốc đồng ý** và chỉ tải những log tạo **sau** mốc đó — bật xong **không** đẩy lịch sử cũ lên.
 *
 * Ở chế độ khách (chưa đăng nhập) thẻ vẫn cho bật, nhưng có thêm dòng nhắc rằng đồng bộ chỉ chạy
 * khi có tài khoản, để người dùng không hiểu nhầm là dữ liệu đã được sao lưu.
 */
@Composable
fun CloudSyncCard(
    settings: CloudSyncSettings,
    isSignedIn: Boolean,
    onToggle: (Boolean) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(CardGreen.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (settings.enabled) Icons.Default.CloudUpload else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = CardGreen
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.cloud_sync_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = CardGreen)
                )
            }

            Text(
                text = if (settings.enabled) {
                    stringResource(R.string.cloud_sync_description_on)
                } else {
                    stringResource(R.string.cloud_sync_description_off)
                },
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
            )

            if (settings.enabled && !isSignedIn) {
                Text(
                    text = stringResource(R.string.cloud_sync_guest_note),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE65100))
                )
            }

            TextButton(
                onClick = onOpenPrivacyPolicy,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.cloud_sync_open_privacy_policy))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CloudSyncCardOffPreview() {
    HealthyAITheme {
        CloudSyncCard(
            settings = CloudSyncSettings(),
            isSignedIn = true,
            onToggle = {},
            onOpenPrivacyPolicy = {},
            modifier = Modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CloudSyncCardGuestOnPreview() {
    HealthyAITheme {
        CloudSyncCard(
            settings = CloudSyncSettings(enabled = true, consentAtMillis = 1_700_000_000_000L),
            isSignedIn = false,
            onToggle = {},
            onOpenPrivacyPolicy = {},
            modifier = Modifier
        )
    }
}
