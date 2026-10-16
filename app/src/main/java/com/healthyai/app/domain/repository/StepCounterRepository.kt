package com.healthyai.app.domain.repository

import com.healthyai.app.domain.model.StepCounterStatus
import kotlinx.coroutines.flow.Flow

/** Đếm bước chân bằng cảm biến của thiết bị, tự đo trên máy nên không cần Health Connect. */
interface StepCounterRepository {

    /** Trạng thái đọc tức thời: máy có cảm biến không, đã có quyền chưa. */
    fun getStatus(): StepCounterStatus

    /** Số bước hôm nay (đã trừ ngày trước và xử lý khởi động lại); phát giá trị gần nhất rồi cập nhật theo cảm biến, chỉ phát `0` khi chưa đủ điều kiện. */
    fun observeTodaySteps(): Flow<Long>

    /** Đọc một lần số bước hôm nay cho worker và widget; chờ tối đa [timeoutMs] rồi trả giá trị đã lưu nên không bao giờ treo. */
    suspend fun readTodaySteps(timeoutMs: Long = DEFAULT_READ_TIMEOUT_MS): Long

    companion object {
        /** Thời gian chờ mẫu cảm biến đầu tiên khi worker đọc số bước. */
        const val DEFAULT_READ_TIMEOUT_MS = 2_000L

        /** Không chờ mẫu cảm biến, trả ngay số bước đã lưu; dùng cho widget để tránh launcher hiện khung trắng. */
        const val NO_WAIT_MS = 0L
    }
}
