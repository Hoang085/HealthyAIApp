package com.healthyai.app.domain.usecase.steps

import java.time.Instant
import java.time.ZoneId

/** Quy đổi số luỹ kế của TYPE_STEP_COUNTER thành số bước hôm nay, chỉ tính phần app đang chạy. */
object StepCounterMath {

    /** Trạng thái lưu giữa các lần đọc, lưu trong SharedPreferences. */
    data class State(
        val dateIso: String,
        val sessionId: String,
        val baseline: Long,
        val offset: Long,
        val lastCumulative: Long,
        val lastSampleMs: Long,
        val todaySteps: Long
    )

    data class Result(val state: State, val todaySteps: Long)

    /** Xử lý một mẫu cảm biến và trả về số bước đã đếm được trong hôm nay. */
    fun sample(
        state: State?,
        cumulative: Long,
        nowMs: Long,
        sessionId: String,
        zone: ZoneId
    ): Result {
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val todayIso = today.toString()

        if (state == null) {
            // Chưa có gì để so sánh: bắt đầu đếm từ chính mẫu này (không suy diễn ngược về trước).
            return result(todayIso, sessionId, baseline = cumulative, offset = 0L, cumulative, nowMs)
        }

        val newDay = state.dateIso != todayIso
        // Bộ đếm luỹ kế nhỏ hơn mẫu trước ⇒ máy (hoặc cảm biến) đã khởi động lại.
        val counterReset = cumulative < state.lastCumulative

        val baseline: Long
        val offset: Long
        when {
            // App đã bị đóng rồi mở lại: đếm lại từ mẫu hiện tại, giữ phần đã đếm trong ngày.
            state.sessionId != sessionId -> {
                baseline = cumulative
                offset = if (newDay) 0L else carriedOverSteps(state)
            }

            // Cùng một phiên đang chạy mà sang ngày mới: nội suy mốc 00:00 giữa hai mẫu của phiên này.
            newDay -> {
                offset = 0L
                baseline = if (counterReset) {
                    cumulative
                } else {
                    interpolateAt(
                        t0 = state.lastSampleMs,
                        c0 = state.lastCumulative,
                        t1 = nowMs,
                        c1 = cumulative,
                        targetMs = today.atStartOfDay(zone).toInstant().toEpochMilli()
                    )
                }
            }

            // Cùng phiên, cùng ngày, nhưng bộ đếm vừa bị reset: giữ lại phần đã đi trước đó.
            counterReset -> {
                baseline = cumulative
                offset = carriedOverSteps(state)
            }

            else -> {
                baseline = state.baseline
                offset = state.offset
            }
        }

        return result(todayIso, sessionId, baseline, offset, cumulative, nowMs)
    }

    /** Số bước nên hiển thị ngay khi mở app; chỉ dùng giá trị đã lưu nếu thuộc đúng hôm nay. */
    fun storedTodaySteps(state: State?, todayIso: String): Long =
        if (state != null && state.dateIso == todayIso) state.todaySteps else 0L

    /** Số bước đã đếm được trong ngày tính tới mẫu cuối của [state] (dùng khi baseline đổi). */
    private fun carriedOverSteps(state: State): Long =
        state.offset + (state.lastCumulative - state.baseline).coerceAtLeast(0L)

    private fun result(
        todayIso: String,
        sessionId: String,
        baseline: Long,
        offset: Long,
        cumulative: Long,
        nowMs: Long
    ): Result {
        val todaySteps = (offset + (cumulative - baseline)).coerceAtLeast(0L)
        return Result(
            state = State(
                dateIso = todayIso,
                sessionId = sessionId,
                baseline = baseline,
                offset = offset,
                lastCumulative = cumulative,
                lastSampleMs = nowMs,
                todaySteps = todaySteps
            ),
            todaySteps = todaySteps
        )
    }

    /** Nội suy tuyến tính giá trị luỹ kế tại targetMs giữa hai mẫu, kẹp trong khoảng [c0,c1]. */
    private fun interpolateAt(t0: Long, c0: Long, t1: Long, c1: Long, targetMs: Long): Long {
        if (t1 <= t0 || c0 == c1) return c1
        val clamped = targetMs.coerceIn(t0, t1)
        val estimate = c0 + ((c1 - c0).toDouble() * (clamped - t0).toDouble() / (t1 - t0).toDouble()).toLong()
        return estimate.coerceIn(minOf(c0, c1), maxOf(c0, c1))
    }
}
