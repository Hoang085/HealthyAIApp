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
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.healthyai.app.R
import com.healthyai.app.domain.model.HealthConnectDataSettings
import com.healthyai.app.presentation.ui.theme.HealthyAITheme

private val CardGreen = Color(0xFF2E7D32)

/**
 * Thẻ **"Dùng dữ liệu Health Connect"** — switch quyết định **nguồn dữ liệu** cho thẻ "Tóm tắt sức khỏe".
 *
 * - **BẬT**: các ô Bước chân / Ngủ / Tim lấy số **trực tiếp từ Health Connect** (có thể gồm cả dữ liệu
 *   của đồng hồ đeo tay).
 * - **TẮT** (mặc định): lấy **dữ liệu đã lưu trong app** — bản chụp sức khỏe của hôm nay, và cảm biến
 *   tự đo của máy khi bản chụp còn trống.
 *
 * Switch chỉ đổi **nguồn hiển thị**: dữ liệu trên máy vẫn được giữ nguyên và mã hoá.
 */
@Composable
fun HealthDataSourceCard(
    settings: HealthConnectDataSettings,
    onToggle: (Boolean) -> Unit,
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
                        imageVector = if (settings.enabled) {
                            Icons.Default.HealthAndSafety
                        } else {
                            Icons.Default.Smartphone
                        },
                        contentDescription = null,
                        tint = CardGreen
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.health_data_source_title),
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
                    stringResource(R.string.health_data_source_description_on)
                } else {
                    stringResource(R.string.health_data_source_description_off)
                },
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HealthDataSourceCardOffPreview() {
    HealthyAITheme {
        HealthDataSourceCard(
            settings = HealthConnectDataSettings(),
            onToggle = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HealthDataSourceCardOnPreview() {
    HealthyAITheme {
        HealthDataSourceCard(
            settings = HealthConnectDataSettings(enabled = true),
            onToggle = {}
        )
    }
}
