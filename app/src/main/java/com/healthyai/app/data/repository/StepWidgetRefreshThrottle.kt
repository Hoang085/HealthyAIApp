package com.healthyai.app.data.repository

/**
 * Cổng chặn nhịp vẽ lại widget "Sức khỏe hôm nay" theo bước chân.
 *
 * Chỉ cho qua khi số bước thực sự đổi **và** đã cách lần vẽ trước ít nhất [minIntervalMs]: widget
 * bám sát số đang hiện trên app mà không phải vẽ lại theo từng mẫu cảm biến (mỗi lần vẽ lại là một
 * lần dựng lại toàn bộ Glance nên rất tốn pin).
 *
 * Thread-safe (`@Synchronized`): được gọi từ luồng thu cảm biến và từ lần vẽ bù hẹn giờ của
 * [StepCounterRepositoryImpl].
 */
class StepWidgetRefreshThrottle(
    private val minIntervalMs: Long = DEFAULT_MIN_INTERVAL_MS
) {

    /** Số bước của lần vẽ lại gần nhất; `null` = chưa vẽ lại lần nào từ lúc tiến trình chạy. */
    private var lastSteps: Long? = null

    /** Thời điểm vẽ lại gần nhất (ms); chỉ có nghĩa khi [lastSteps] khác `null`. */
    private var lastRefreshMs = 0L

    /** `true` nếu nên vẽ lại widget lúc [nowMs] với số bước [steps]; khi trả `true` thì ghi nhận luôn lần vẽ này. */
    @Synchronized
    fun shouldRefresh(steps: Long, nowMs: Long): Boolean {
        if (delayBeforeRefreshMs(steps, nowMs) != 0L) return false

        lastSteps = steps
        lastRefreshMs = nowMs
        return true
    }

    /**
     * Còn phải chờ bao lâu (ms) mới được vẽ lại [steps]: `0` = vẽ được ngay, `null` = không cần vẽ.
     *
     * Không ghi nhận gì. Nơi gọi dùng số này để **hẹn một lần vẽ bù** khi đang bị chặn; nếu không,
     * số cuối cùng trước khi người dùng dừng đi sẽ không bao giờ lên widget (cảm biến chỉ phát mẫu
     * khi có bước mới).
     */
    @Synchronized
    fun delayBeforeRefreshMs(steps: Long, nowMs: Long): Long? {
        // Chưa có bước nào để hiện thì widget không có gì đổi.
        if (steps <= 0L) return null

        // Cảm biến phát lại cùng một số (ví dụ ngay sau khi màn Home mở lại) → không vẽ lại.
        if (lastSteps == steps) return null

        // Chưa vẽ lần nào từ lúc tiến trình chạy → vẽ ngay.
        if (lastSteps == null) return 0L

        // Mới vẽ lại xong mà số bước chỉ nhích thêm vài bước → chờ đủ nhịp rồi vẽ một lần.
        // Kẹp trong [0, minIntervalMs] để đồng hồ máy bị chỉnh lùi cũng không chờ quá một nhịp.
        val elapsed = nowMs - lastRefreshMs
        return (minIntervalMs - elapsed).coerceIn(0L, minIntervalMs)
    }

    companion object {
        /** Khoảng cách tối thiểu giữa hai lần vẽ lại widget vì bước chân (1 phút). */
        const val DEFAULT_MIN_INTERVAL_MS = 60_000L
    }
}
