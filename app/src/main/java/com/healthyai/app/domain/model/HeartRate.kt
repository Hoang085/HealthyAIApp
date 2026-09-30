package com.healthyai.app.domain.model

/**
 * Phân loại nhịp tim **lúc nghỉ** theo khoảng thường dùng cho người trưởng thành (60–100 bpm).
 *
 * Đây là phân loại **tham khảo** cho một lần đo tại chỗ (ngồi yên, chưa vận động), **không** phải
 * chẩn đoán y khoa: nhịp tim phụ thuộc tuổi, thể trạng, thuốc đang dùng, và cảm xúc.
 */
enum class HeartRateZone(val displayName: String) {
    LOW("Thấp"),
    NORMAL("Bình thường"),
    HIGH("Cao")
}

/**
 * Kết quả một lần đo nhịp tim bằng **camera + đèn flash** của điện thoại (PPG).
 *
 * @property bpm Nhịp tim đo được (nhịp/phút).
 * @property quality Độ tin cậy của kết quả 0..1 (xem `PpgAnalyzer`): nhịp càng đều và tín hiệu càng
 *   có tính chu kỳ thì càng gần 1.
 * @property zone Phân loại tham khảo theo [HeartRateZone].
 * @property measuredAt Thời điểm chốt kết quả (epoch ms).
 */
data class HeartRateResult(
    val bpm: Int,
    val quality: Float,
    val zone: HeartRateZone,
    val measuredAt: Long = System.currentTimeMillis()
)
