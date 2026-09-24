package com.healthyai.app.domain.usecase.steps

import java.time.Instant
import java.time.ZoneId

/**
 * Toán quy đổi cảm biến đếm bước thành **"số bước hôm nay"**.
 *
 * Cảm biến `TYPE_STEP_COUNTER` của Android cho ta một con số **luỹ kế từ lúc máy khởi động**
 * (ví dụ "12 345 bước kể từ khi bật máy"), chứ không cho biết "hôm nay đi được bao nhiêu". Hàm thuần
 * này giải quyết đúng ba ca khó của bài toán đó (được test kỹ ở `StepCounterMathTest`):
 *
 * 1. **Sang ngày mới** — cần mốc luỹ kế tại 00:00 hôm nay, nhưng app có thể không chạy lúc nửa đêm.
 *    Cách xử lý: **nội suy tuyến tính** giữa mẫu cuối của ngày hôm trước và mẫu đầu tiên của hôm nay.
 * 2. **Máy khởi động lại** — bộ đếm **reset về 0**, nên nếu không xử lý thì số bước trong ngày sẽ
 *    tụt xuống. Cách xử lý: phần đã đếm trước khi khởi động lại được giữ trong [State.offset].
 * 3. **Máy khởi động trong hôm nay** — mọi bước từ lúc boot đều thuộc hôm nay, nên baseline = 0
 *    (chính xác hơn là bắt đầu đếm từ 0 ở lần mở app đầu tiên).
 *
 * Hàm không phụ thuộc Android (chỉ cần `java.time`) nên chạy được trong unit test JVM.
 */
object StepCounterMath {

    /**
     * Trạng thái lưu giữa các lần đọc (một bản ghi trong `SharedPreferences`).
     *
     * @property dateIso Ngày (ISO `yyyy-MM-dd`) mà [baseline]/[offset] đang thuộc về.
     * @property baseline Giá trị luỹ kế của cảm biến được coi là **"0 bước của ngày"**.
     * @property offset Số bước **đã đi trong ngày** trước lần khởi động lại máy gần nhất.
     * @property lastCumulative Mẫu luỹ kế gần nhất (để phát hiện khởi động lại và để nội suy).
     * @property lastSampleMs Thời điểm (epoch ms) của mẫu gần nhất.
     * @property todaySteps Số bước hôm nay tính ở lần đọc gần nhất — dùng để hiện ngay khi mở app.
     */
    data class State(
        val dateIso: String,
        val baseline: Long,
        val offset: Long,
        val lastCumulative: Long,
        val lastSampleMs: Long,
        val todaySteps: Long
    )

    data class Result(val state: State, val todaySteps: Long)

    /**
     * Xử lý một mẫu cảm biến và trả về số bước hôm nay.
     *
     * @param state Trạng thái đã lưu trước đó, `null` nếu lần đầu chạy.
     * @param cumulative Giá trị luỹ kế vừa đọc từ cảm biến.
     * @param nowMs Thời điểm đọc mẫu (epoch ms) — truyền vào để test được.
     * @param bootTimeMs Thời điểm máy khởi động (đồng hồ tường), thường là
     *   `System.currentTimeMillis() - SystemClock.elapsedRealtime()`.
     * @param zone Múi giờ dùng để xác định ranh giới ngày.
     */
    fun sample(
        state: State?,
        cumulative: Long,
        nowMs: Long,
        bootTimeMs: Long,
        zone: ZoneId
    ): Result {
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val todayIso = today.toString()
        val startOfTodayMs = today.atStartOfDay(zone).toInstant().toEpochMilli()

        if (state == null) {
            // Lần đầu chạy: nếu máy khởi động trong hôm nay thì mọi bước từ lúc boot là của hôm nay;
            // nếu máy đã bật từ hôm trước thì chỉ bắt đầu đếm từ thời điểm này (không biết mốc 00:00).
            val baseline = if (bootTimeMs >= startOfTodayMs) 0L else cumulative
            return result(todayIso, baseline, offset = 0L, cumulative, nowMs)
        }

        val newDay = state.dateIso != todayIso
        val rebooted = cumulative < state.lastCumulative

        val baseline: Long
        val offset: Long
        when {
            newDay -> {
                offset = 0L
                baseline = when {
                    // Máy khởi động lại trong hôm nay → mọi bước từ lúc boot đều thuộc hôm nay.
                    bootTimeMs >= startOfTodayMs -> 0L
                    // Vừa khởi động lại mà không rõ mốc nào → đếm lại từ mẫu đầu tiên của hôm nay.
                    rebooted -> cumulative
                    // Bình thường: nội suy giá trị luỹ kế tại 00:00 hôm nay giữa hai mẫu.
                    else -> interpolateAt(
                        t0 = state.lastSampleMs,
                        c0 = state.lastCumulative,
                        t1 = nowMs,
                        c1 = cumulative,
                        targetMs = startOfTodayMs
                    )
                }
            }

            rebooted -> {
                // Cùng ngày nhưng máy vừa khởi động lại: giữ lại phần đã đi trước khi reboot.
                baseline = cumulative
                offset = state.offset + (state.lastCumulative - state.baseline).coerceAtLeast(0L)
            }

            else -> {
                baseline = state.baseline
                offset = state.offset
            }
        }

        return result(todayIso, baseline, offset, cumulative, nowMs)
    }

    /**
     * Số bước nên hiển thị **ngay khi mở app** (trước khi cảm biến kịp phát mẫu mới).
     *
     * Chỉ dùng giá trị đã lưu nếu nó thuộc **đúng hôm nay** — nếu đã sang ngày mới thì trả `0` thay vì
     * hiện số của hôm qua.
     */
    fun storedTodaySteps(state: State?, todayIso: String): Long =
        if (state != null && state.dateIso == todayIso) state.todaySteps else 0L

    private fun result(
        todayIso: String,
        baseline: Long,
        offset: Long,
        cumulative: Long,
        nowMs: Long
    ): Result {
        val todaySteps = (offset + (cumulative - baseline)).coerceAtLeast(0L)
        return Result(
            state = State(
                dateIso = todayIso,
                baseline = baseline,
                offset = offset,
                lastCumulative = cumulative,
                lastSampleMs = nowMs,
                todaySteps = todaySteps
            ),
            todaySteps = todaySteps
        )
    }

    /**
     * Ước lượng giá trị luỹ kế tại [targetMs] bằng nội suy tuyến tính giữa hai mẫu
     * `(t0, c0)` → `(t1, c1)`, luôn nằm trong khoảng `[min(c0,c1), max(c0,c1)]` để không sinh ra
     * giá trị vô lý khi dữ liệu mẫu bất thường.
     */
    private fun interpolateAt(t0: Long, c0: Long, t1: Long, c1: Long, targetMs: Long): Long {
        if (t1 <= t0 || c0 == c1) return c1
        val clamped = targetMs.coerceIn(t0, t1)
        val estimate = c0 + ((c1 - c0).toDouble() * (clamped - t0).toDouble() / (t1 - t0).toDouble()).toLong()
        return estimate.coerceIn(minOf(c0, c1), maxOf(c0, c1))
    }
}
