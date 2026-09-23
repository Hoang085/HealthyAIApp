package com.healthyai.app.domain.model

/**
 * Chỉ số sinh lực kèm mức thay đổi so với tuần trước.
 *
 * [delta] = null nghĩa là chưa có dữ liệu của 7 ngày trước đó → UI ẩn phần so sánh
 * thay vì hiển thị một con số cố định.
 */
data class VitalityScore(
    val score: Int = 0,
    val delta: Int? = null
)
