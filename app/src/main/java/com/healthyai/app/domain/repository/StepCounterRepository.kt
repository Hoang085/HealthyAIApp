package com.healthyai.app.domain.repository

import com.healthyai.app.domain.model.StepCounterStatus
import kotlinx.coroutines.flow.Flow

/**
 * Đếm bước chân bằng cảm biến của thiết bị.
 *
 * Khác với `HealthDataRepository` (đọc số liệu **đã có sẵn** trong Health Connect), repository này
 * **tự đo** trên máy: nên app vẫn có số bước khi máy không cài ứng dụng sức khỏe nào và người dùng
 * không cấp quyền Health Connect.
 */
interface StepCounterRepository {

    /** Trạng thái đọc tức thời: máy có cảm biến không, đã có quyền chưa. */
    fun getStatus(): StepCounterStatus

    /**
     * Số bước **hôm nay** (đã trừ phần của ngày hôm trước và xử lý khởi động lại máy).
     *
     * Phát ngay giá trị đã biết gần nhất để UI không trống, sau đó phát lại mỗi khi cảm biến cập nhật.
     * Khi chưa đủ điều kiện ([StepCounterStatus.isReady] = `false`) flow chỉ phát `0` rồi kết thúc —
     * UI hiển thị thẻ xin quyền thay vì số liệu.
     */
    fun observeTodaySteps(): Flow<Long>

    /**
     * Đọc **một lần** số bước hôm nay từ cảm biến (dùng cho worker ghi lịch sử vào Room).
     *
     * Chờ tối đa [timeoutMs] cho mẫu cảm biến đầu tiên; hết thời gian chờ (hoặc chưa đủ điều kiện)
     * thì trả về giá trị đã lưu gần nhất — nhờ vậy worker không bao giờ treo.
     */
    suspend fun readTodaySteps(timeoutMs: Long = DEFAULT_READ_TIMEOUT_MS): Long

    companion object {
        /** Thời gian chờ mẫu cảm biến đầu tiên khi worker đọc số bước. */
        const val DEFAULT_READ_TIMEOUT_MS = 2_000L
    }
}
