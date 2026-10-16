package com.healthyai.app.domain.model

/** Nguồn số liệu cho thẻ "Tóm tắt sức khỏe": mặc định TẮT để hiển thị dữ liệu app đã ghi, bật để lấy số mới nhất từ Health Connect. */
data class HealthConnectDataSettings(
    val enabled: Boolean = false
)
