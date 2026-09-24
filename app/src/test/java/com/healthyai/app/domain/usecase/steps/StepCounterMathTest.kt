package com.healthyai.app.domain.usecase.steps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Unit test cho [StepCounterMath] — phần dễ sai nhất khi tự đếm bước bằng `TYPE_STEP_COUNTER`:
 * sang ngày mới (nội suy mốc 00:00) và máy khởi động lại (bộ đếm reset về 0).
 *
 * Mọi mốc thời gian đều được tính bằng tay để test không phụ thuộc thời gian chạy thật.
 */
class StepCounterMathTest {

    private val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
    private val today: LocalDate = LocalDate.of(2026, 9, 24)
    private val yesterday: LocalDate = today.minusDays(1)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Long =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    // ── Lần chạy đầu tiên ──────────────────────────────────────

    @Test
    fun `first run after a reboot earlier today counts every step since boot`() {
        val result = StepCounterMath.sample(
            state = null,
            cumulative = 2_500L,
            nowMs = at(today, 10),
            bootTimeMs = at(today, 7), // máy bật lúc 07:00 hôm nay
            zone = zone
        )

        // Mọi bước từ lúc boot đều thuộc hôm nay → baseline 0
        assertEquals(2_500L, result.todaySteps)
        assertEquals(0L, result.state.baseline)
    }

    @Test
    fun `first run when the device booted yesterday starts counting from now`() {
        val result = StepCounterMath.sample(
            state = null,
            cumulative = 9_000L,
            nowMs = at(today, 10),
            bootTimeMs = at(yesterday, 7), // máy đã bật từ hôm qua → không biết mốc 00:00
            zone = zone
        )

        assertEquals(0L, result.todaySteps)
        assertEquals(9_000L, result.state.baseline)
    }

    // ── Trong cùng một ngày ────────────────────────────────────

    @Test
    fun `steps accumulate across samples within the same day`() {
        val first = StepCounterMath.sample(null, 1_000L, at(today, 10), at(today, 7), zone)
        assertEquals(1_000L, first.todaySteps)

        val second = StepCounterMath.sample(first.state, 1_300L, at(today, 11), at(today, 7), zone)
        assertEquals(1_300L, second.todaySteps)
        assertEquals(today.toString(), second.state.dateIso)
    }

    @Test
    fun `a reboot in the same day keeps the steps already walked`() {
        val beforeReboot = StepCounterMath.sample(null, 2_000L, at(today, 10), at(today, 7), zone)

        // Máy khởi động lại lúc 11:00 → bộ đếm về 0 và đếm lại từ đầu
        val afterReboot = StepCounterMath.sample(
            state = beforeReboot.state,
            cumulative = 150L,
            nowMs = at(today, 12),
            bootTimeMs = at(today, 11),
            zone = zone
        )

        assertEquals(2_000L, afterReboot.todaySteps)

        val laterSameDay = StepCounterMath.sample(
            state = afterReboot.state,
            cumulative = 350L,
            nowMs = at(today, 13),
            bootTimeMs = at(today, 11),
            zone = zone
        )
        assertEquals(2_200L, laterSameDay.todaySteps)
    }

    @Test
    fun `an abnormal counter value never produces negative steps`() {
        val state = StepCounterMath.State(
            dateIso = today.toString(),
            baseline = 5_000L,
            offset = 0L,
            lastCumulative = 5_000L,
            lastSampleMs = at(today, 10),
            todaySteps = 0L
        )

        val result = StepCounterMath.sample(state, 4_000L, at(today, 11), at(today, 7), zone)

        assertTrue("Số bước không bao giờ âm", result.todaySteps >= 0L)
    }

    // ── Sang ngày mới ──────────────────────────────────────────

    @Test
    fun `day rollover estimates the midnight baseline by interpolation`() {
        // 23:00 hôm qua: 1 000 bước (luỹ kế từ lúc boot 07:00 hôm qua)
        val lastNight = StepCounterMath.sample(null, 1_000L, at(yesterday, 23), at(yesterday, 7), zone)
        assertEquals(1_000L, lastNight.todaySteps)

        // 01:00 hôm nay: luỹ kế 2 500. Mốc 00:00 nằm giữa 2 mẫu (cách nhau 2 giờ) → 1 000 + 750 = 1 750
        val thisMorning = StepCounterMath.sample(
            state = lastNight.state,
            cumulative = 2_500L,
            nowMs = at(today, 1),
            bootTimeMs = at(yesterday, 7),
            zone = zone
        )

        assertEquals("Chỉ tính phần đi sau 00:00", 750L, thisMorning.todaySteps)
        assertEquals(1_750L, thisMorning.state.baseline)
        assertEquals(0L, thisMorning.state.offset)
        assertEquals(today.toString(), thisMorning.state.dateIso)
    }

    @Test
    fun `day rollover after a reboot in the new day counts steps since boot`() {
        val lastNight = StepCounterMath.sample(null, 8_000L, at(yesterday, 23), at(yesterday, 7), zone)

        // Máy khởi động lại lúc 08:00 hôm nay → mọi bước từ lúc boot là của hôm nay
        val afterReboot = StepCounterMath.sample(
            state = lastNight.state,
            cumulative = 50L,
            nowMs = at(today, 9),
            bootTimeMs = at(today, 8),
            zone = zone
        )

        assertEquals(50L, afterReboot.todaySteps)
        assertEquals(0L, afterReboot.state.baseline)
    }

    @Test
    fun `day rollover with a gap of several days still returns a sane value`() {
        val old = StepCounterMath.sample(null, 3_000L, at(today.minusDays(3), 22), at(today.minusDays(3), 7), zone)

        val result = StepCounterMath.sample(
            state = old.state,
            cumulative = 9_000L,
            nowMs = at(today, 10),
            bootTimeMs = at(today.minusDays(3), 7),
            zone = zone
        )

        assertEquals(today.toString(), result.state.dateIso)
        assertTrue("Baseline phải nằm giữa hai mẫu", result.state.baseline in 3_000L..9_000L)
        assertTrue("Số bước hôm nay không âm", result.todaySteps >= 0L)
    }

    // ── Giá trị hiển thị ngay khi mở app ───────────────────────

    @Test
    fun `stored steps are shown only when they belong to today`() {
        val state = StepCounterMath.State(
            dateIso = today.toString(),
            baseline = 0L,
            offset = 0L,
            lastCumulative = 4_230L,
            lastSampleMs = at(today, 9),
            todaySteps = 4_230L
        )

        assertEquals(4_230L, StepCounterMath.storedTodaySteps(state, today.toString()))
        assertEquals("Sang ngày mới thì không hiện số của hôm qua", 0L, StepCounterMath.storedTodaySteps(state, yesterday.toString()))
    }

    @Test
    fun `stored steps are zero when nothing was stored yet`() {
        assertEquals(0L, StepCounterMath.storedTodaySteps(null, today.toString()))
    }
}
