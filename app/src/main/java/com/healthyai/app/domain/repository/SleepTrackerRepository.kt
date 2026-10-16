package com.healthyai.app.domain.repository

/** Đo giờ ngủ bằng cảm biến (ánh sáng + gia tốc + trạng thái màn hình) trong khung giờ ngủ đã đặt; là nguồn dự phòng khi không có Health Connect. */
interface SleepTrackerRepository {

    /** Số giờ ngủ của đêm vừa qua; trả `0.0` khi thiếu cảm biến, chưa có mẫu, hoặc chưa đủ ngưỡng coi là ngủ. */
    suspend fun readLastNightSleepHours(): Double

    /** Đọc môi trường và cập nhật phiên ngủ theo khung giờ đã đặt; trả số phút ngủ của đêm đang xét. */
    suspend fun sampleAndStore(): Long
}
