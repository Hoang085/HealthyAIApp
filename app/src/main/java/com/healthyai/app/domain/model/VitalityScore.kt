package com.healthyai.app.domain.model

/**
 * Chỉ số sinh lực (0–100) kèm mức thay đổi so với tuần trước.
 *
 * - [hasData] = `false` nghĩa là **chưa đủ dữ liệu để chấm điểm** (chưa có thói quen nào, chưa có bước
 *   chân và chưa có giờ ngủ) — UI hiện lời nhắc thay vì một con số ảo.
 * - [delta] = `null` nghĩa là **7 ngày trước đó không có quan sát nào** → UI ẩn phần so sánh thay vì
 *   hiển thị một con số cố định.
 */
data class VitalityScore(
    val score: Int = 0,
    val delta: Int? = null,
    val hasData: Boolean = false
)
