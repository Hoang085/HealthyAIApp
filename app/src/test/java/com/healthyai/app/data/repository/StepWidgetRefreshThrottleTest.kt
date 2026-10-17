package com.healthyai.app.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Test cho [StepWidgetRefreshThrottle]: chỉ vẽ lại widget khi số bước đổi và đã đủ nhịp một phút. */
class StepWidgetRefreshThrottleTest {

    private val throttle = StepWidgetRefreshThrottle(minIntervalMs = 60_000L)

    @Test
    fun `the first count with steps is painted right away`() {
        assertTrue(throttle.shouldRefresh(steps = 120L, nowMs = 1_000L))
    }

    @Test
    fun `the same count is never painted twice`() {
        assertTrue(throttle.shouldRefresh(steps = 120L, nowMs = 1_000L))

        assertFalse(
            "Cảm biến phát lại đúng số cũ (ví dụ màn Home mở lại) thì widget không có gì đổi",
            throttle.shouldRefresh(steps = 120L, nowMs = 200_000L)
        )
    }

    @Test
    fun `a changed count inside the interval waits for the next slot`() {
        assertTrue(throttle.shouldRefresh(steps = 120L, nowMs = 1_000L))

        assertFalse(
            "Mới vẽ lại xong mà số chỉ nhích thêm vài bước thì chờ",
            throttle.shouldRefresh(steps = 140L, nowMs = 30_000L)
        )
        assertTrue(
            "Đủ một phút kể từ lần vẽ trước thì vẽ lại với số mới nhất",
            throttle.shouldRefresh(steps = 150L, nowMs = 61_000L)
        )
    }

    @Test
    fun `a count of zero is never painted`() {
        assertFalse(throttle.shouldRefresh(steps = 0L, nowMs = 1_000L))
        assertFalse(throttle.shouldRefresh(steps = 0L, nowMs = 100_000L))
    }

    @Test
    fun `a smaller count after the day rolls over is still painted`() {
        assertTrue(throttle.shouldRefresh(steps = 5_000L, nowMs = 1_000L))

        // Sang ngày mới bộ đếm về 0 rồi bắt đầu lại; lần vẽ trước đã đủ lâu nên số mới được vẽ ngay.
        assertTrue(throttle.shouldRefresh(steps = 12L, nowMs = 3_600_000L))
    }
}
