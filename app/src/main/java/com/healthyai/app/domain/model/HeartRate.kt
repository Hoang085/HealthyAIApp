package com.healthyai.app.domain.model

/** Phân loại nhịp tim lúc nghỉ (60–100 bpm ở người trưởng thành); chỉ mang tính tham khảo, không phải chẩn đoán y khoa. */
enum class HeartRateZone(val displayName: String) {
    LOW("Thấp"),
    NORMAL("Bình thường"),
    HIGH("Cao")
}

/** Kết quả một lần đo nhịp tim bằng camera + đèn flash của điện thoại (PPG). */
data class HeartRateResult(
    val bpm: Int,
    val quality: Float,
    val zone: HeartRateZone,
    val measuredAt: Long = System.currentTimeMillis()
)
