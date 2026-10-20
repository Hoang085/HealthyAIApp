package com.healthyai.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun `delay is zero before the first paint`() {
        assertEquals(0L, throttle.delayBeforeRefreshMs(steps = 31L, nowMs = 1_000L))
    }

    @Test
    fun `delay is null when nothing needs painting`() {
        assertNull(throttle.delayBeforeRefreshMs(steps = 0L, nowMs = 1_000L))

        assertTrue(throttle.shouldRefresh(steps = 31L, nowMs = 1_000L))
        assertNull(throttle.delayBeforeRefreshMs(steps = 31L, nowMs = 5_000L))
    }

    @Test
    fun `a blocked count reports the remaining wait so a trailing paint can be scheduled`() {
        // Testcase thật: widget vừa vẽ 31, vài giây sau app đã lên 54.
        assertTrue(throttle.shouldRefresh(steps = 31L, nowMs = 1_000L))

        assertEquals(50_000L, throttle.delayBeforeRefreshMs(steps = 54L, nowMs = 11_000L))
        assertFalse(throttle.shouldRefresh(steps = 54L, nowMs = 11_000L))

        // Hết nhịp: lần vẽ bù được phép vẽ số cuối cùng dù cảm biến không phát thêm mẫu nào.
        assertEquals(0L, throttle.delayBeforeRefreshMs(steps = 54L, nowMs = 61_000L))
        assertTrue(throttle.shouldRefresh(steps = 54L, nowMs = 61_000L))
    }

    @Test
    fun `delay never exceeds one interval even if the clock goes backwards`() {
        assertTrue(throttle.shouldRefresh(steps = 31L, nowMs = 100_000L))

        assertEquals(60_000L, throttle.delayBeforeRefreshMs(steps = 54L, nowMs = 10_000L))
    }

    @Test
    fun `asking for the delay does not record a paint`() {
        throttle.delayBeforeRefreshMs(steps = 31L, nowMs = 1_000L)

        assertTrue(throttle.shouldRefresh(steps = 31L, nowMs = 1_000L))
    }
}
