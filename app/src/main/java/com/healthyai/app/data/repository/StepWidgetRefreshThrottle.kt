package com.healthyai.app.data.repository

/**
 * Cổng chặn nhịp vẽ lại widget "Sức khỏe hôm nay" theo bước chân.
 *
 * Chỉ cho qua khi số bước thực sự đổi **và** đã cách lần vẽ trước ít nhất [minIntervalMs]: widget
 * bám sát số đang hiện trên app mà không phải vẽ lại theo từng mẫu cảm biến (mỗi lần vẽ lại là một
 * lần dựng lại toàn bộ Glance nên rất tốn pin).
 *
 * Không thread-safe; dùng từ một luồng thu cảm biến duy nhất của [StepCounterRepositoryImpl].
 */
class StepWidgetRefreshThrottle(
    private val minIntervalMs: Long = DEFAULT_MIN_INTERVAL_MS
) {

    /** Số bước của lần vẽ lại gần nhất; `null` = chưa vẽ lại lần nào từ lúc tiến trình chạy. */
    private var lastSteps: Long? = null

    /** Thời điểm vẽ lại gần nhất (ms); chỉ có nghĩa khi [lastSteps] khác `null`. */
    private var lastRefreshMs = 0L

    /** `true` nếu nên vẽ lại widget lúc [nowMs] với số bước [steps]; khi trả `true` thì ghi nhận luôn lần vẽ này. */
    fun shouldRefresh(steps: Long, nowMs: Long): Boolean {
        // Chưa có bước nào để hiện thì widget không có gì đổi.
        if (steps <= 0L) return false

        // Cảm biến phát lại cùng một số (ví dụ ngay sau khi màn Home mở lại) → không vẽ lại.
        if (lastSteps == steps) return false

        // Mới vẽ lại xong mà số bước chỉ nhích thêm vài bước → chờ đủ nhịp rồi vẽ một lần.
        if (lastSteps != null && nowMs - lastRefreshMs < minIntervalMs) return false

        lastSteps = steps
        lastRefreshMs = nowMs
        return true
    }

    companion object {
        /** Khoảng cách tối thiểu giữa hai lần vẽ lại widget vì bước chân (1 phút). */
        const val DEFAULT_MIN_INTERVAL_MS = 60_000L
    }
}
