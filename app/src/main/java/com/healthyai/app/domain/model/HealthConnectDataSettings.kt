package com.healthyai.app.domain.model

/**
 * Thiết lập **nguồn dữ liệu sức khỏe của thẻ "Tóm tắt sức khỏe"** trên màn Trang chủ — mặc định **TẮT**.
 *
 * Switch do người dùng bật/tắt ngay trên Trang chủ:
 * - [enabled] = `true`: thẻ "Tóm tắt sức khỏe" dùng số liệu **trực tiếp từ Health Connect** (bước chân,
 *   giờ ngủ, nhịp tim — có thể gồm cả dữ liệu của đồng hồ đeo tay).
 * - [enabled] = `false` (mặc định): dùng **dữ liệu đã lưu trong app** — bản chụp sức khỏe của hôm nay
 *   trong `health_data_history`, và khi bản chụp còn trống thì lấy nguồn tự đo của máy (cảm biến đếm
 *   bước / bộ ước lượng giấc ngủ) làm dự phòng.
 *
 * Chọn mặc định **TẮT** để app hiển thị đúng dữ liệu nó đã ghi; người dùng chủ động bật khi muốn lấy
 * số mới nhất từ Health Connect.
 */
data class HealthConnectDataSettings(
    val enabled: Boolean = false
)
