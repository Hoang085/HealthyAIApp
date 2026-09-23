package com.healthyai.app.presentation.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Thẻ lỗi đỏ dùng chung cho các màn hình có thao tác ghi dữ liệu: hiện [message] kèm nút đóng
 * ([onDismiss]). Dùng ở màn Trang chủ (đọc dữ liệu sức khỏe) và màn Thói quen (ghi nhận thất bại).
 */
@Composable
fun ErrorStateCard(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Error, null, tint = Color.Red)
            Spacer(modifier = Modifier.width(12.dp))
            Text(message, modifier = Modifier.weight(1f), color = Color.Red)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = Color.Red)
            }
        }
    }
}
