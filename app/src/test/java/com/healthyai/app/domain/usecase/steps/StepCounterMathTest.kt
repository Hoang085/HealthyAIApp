package com.healthyai.app.domain.usecase.steps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Unit test cho [StepCounterMath] — phần dễ sai nhất khi tự đếm bước bằng `TYPE_STEP_COUNTER`.
 *
 * Hai nhóm hành vi được chốt ở đây:
 * 1. **Chỉ đếm lúc app đang chạy**: app đang mở / còn trong tác vụ (chạy nền) thì đếm tiếp; đóng hẳn
 *    app rồi mở lại thì khoảng thời gian ở giữa **không** được cộng, nhưng số đã đếm trong ngày vẫn giữ.
 * 2. **Các ca khó của con số luỹ kế**: sang ngày mới trong lúc phiên đang chạy (nội suy mốc 00:00) và
 *    bộ đếm bị reset (máy khởi động lại) không làm số bước tụt.
 *
 * Mọi mốc thời gian đều được tính bằng tay để test không phụ thuộc thời gian chạy thật.
 */
class StepCounterMathTest {

    private val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
    private val today: LocalDate = LocalDate.of(2026, 9, 24)
    private val yesterday: LocalDate = today.minusDays(1)

    /** Hai "phiên app" khác nhau: mỗi lần tiến trình app được tạo lại là một phiên mới. */
    private val sessionA = "session-a"
    private val sessionB = "session-b"

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Long =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun sample(
        state: StepCounterMath.State?,
        cumulative: Long,
        nowMs: Long,
        sessionId: String = sessionA
    ) = StepCounterMath.sample(state, cumulative, nowMs, sessionId, zone)

    // ── Chỉ đếm từ lúc app chạy ────────────────────────────────

    @Test
    fun `first sample starts counting from now and does not back fill the steps since boot`() {
        val result = sample(state = null, cumulative = 2_500L, nowMs = at(today, 10))

        assertEquals("Bước đi trước khi app chạy lần đầu KHÔNG được tính", 0L, result.todaySteps)
        assertEquals(2_500L, result.state.baseline)
        assertEquals(sessionA, result.state.sessionId)
    }

    @Test
    fun `steps accumulate across samples within the same session`() {
        val first = sample(null, 1_000L, at(today, 10))
        assertEquals(0L, first.todaySteps)

        val second = sample(first.state, 1_300L, at(today, 11))
        assertEquals(300L, second.todaySteps)
        assertEquals(today.toString(), second.state.dateIso)
    }

    @Test
    fun `reopening the app in the same day keeps the steps already counted`() {
        // Phiên A: đi 500 bước trong lúc app chạy.
        val sessionOne = sample(null, 1_000L, at(today, 10))
        val stillSessionOne = sample(sessionOne.state, 1_500L, at(today, 10, 30))
        assertEquals(500L, stillSessionOne.todaySteps)

        // Đóng hẳn app, đi thêm 800 bước (app KHÔNG chạy), rồi mở lại → phiên B.
        val sessionTwo = sample(stillSessionOne.state, 2_300L, at(today, 12), sessionId = sessionB)
        assertEquals("Số đã đếm trong ngày vẫn giữ", 500L, sessionTwo.todaySteps)
        assertEquals("Bắt đầu tính lại từ mẫu hiện tại", 2_300L, sessionTwo.state.baseline)

        // Trong phiên B: chỉ 200 bước mới được cộng thêm.
        val laterInSessionTwo = sample(sessionTwo.state, 2_500L, at(today, 12, 30), sessionId = sessionB)
        assertEquals(700L, laterInSessionTwo.todaySteps)
    }

    @Test
    fun `steps walked while the app was closed are never counted`() {
        val opened = sample(null, 4_000L, at(today, 9))

        // Người dùng đi bộ 3 000 bước trong lúc app đã bị đóng hẳn.
        val reopened = sample(opened.state, 7_000L, at(today, 15), sessionId = sessionB)

        assertEquals("Đóng app thì không đếm tiếp", 0L, reopened.todaySteps)

        val afterTwoHundredSteps = sample(reopened.state, 7_200L, at(today, 15, 30), sessionId = sessionB)
        assertEquals("Mở lại app thì đếm lại từ đầu", 200L, afterTwoHundredSteps.todaySteps)
    }

    @Test
    fun `a session id written by an older app version is treated as another session`() {
        val stored = StepCounterMath.State(
            dateIso = today.toString(),
            sessionId = "", // bản cũ chưa lưu phiên
            baseline = 1_000L,
            offset = 0L,
            lastCumulative = 1_400L,
            lastSampleMs = at(today, 10),
            todaySteps = 400L
        )

        val result = sample(stored, 5_000L, at(today, 12))

        assertEquals("Khoảng thời gian không rõ app có chạy hay không thì không tính", 400L, result.todaySteps)
        assertEquals(5_000L, result.state.baseline)
    }

    @Test
    fun `reopening the app in a new day starts the day from zero`() {
        val lastNight = sample(null, 8_000L, at(yesterday, 23))

        val thisMorning = sample(lastNight.state, 8_600L, at(today, 9), sessionId = sessionB)

        assertEquals("Sang ngày mới thì bắt đầu lại từ 0", 0L, thisMorning.todaySteps)
        assertEquals(8_600L, thisMorning.state.baseline)
        assertEquals(today.toString(), thisMorning.state.dateIso)
    }

    // ── Sang ngày mới trong lúc app vẫn đang chạy ──────────────

    @Test
    fun `day rollover inside a running session estimates the midnight baseline by interpolation`() {
        // 23:00 hôm qua (phiên đang chạy): luỹ kế 1 000 — mốc bắt đầu đếm là 1 000.
        val lastNight = sample(null, 1_000L, at(yesterday, 23))
        assertEquals(0L, lastNight.todaySteps)

        // 01:00 hôm nay: luỹ kế 2 500. Mốc 00:00 nằm giữa 2 mẫu (cách nhau 2 giờ) → 1 000 + 750 = 1 750
        val thisMorning = sample(lastNight.state, 2_500L, at(today, 1))

        assertEquals("Chỉ tính phần đi sau 00:00", 750L, thisMorning.todaySteps)
        assertEquals(1_750L, thisMorning.state.baseline)
        assertEquals(0L, thisMorning.state.offset)
        assertEquals(today.toString(), thisMorning.state.dateIso)
    }

    @Test
    fun `a counter reset inside the same session keeps the steps already walked`() {
        val beforeReset = sample(null, 2_000L, at(today, 10))
        val walked = sample(beforeReset.state, 2_400L, at(today, 10, 30))
        assertEquals(400L, walked.todaySteps)

        // Bộ đếm phần cứng reset (ví dụ máy khởi động lại) → luỹ kế nhỏ hơn mẫu trước.
        val afterReset = sample(walked.state, 150L, at(today, 12))
        assertEquals(400L, afterReset.todaySteps)

        val laterSameDay = sample(afterReset.state, 350L, at(today, 13))
        assertEquals(600L, laterSameDay.todaySteps)
    }

    @Test
    fun `an abnormal counter value never produces negative steps`() {
        val state = StepCounterMath.State(
            dateIso = today.toString(),
            sessionId = sessionA,
            baseline = 5_000L,
            offset = 0L,
            lastCumulative = 5_000L,
            lastSampleMs = at(today, 10),
            todaySteps = 0L
        )

        val result = sample(state, 4_000L, at(today, 11))

        assertTrue("Số bước không bao giờ âm", result.todaySteps >= 0L)
    }

    @Test
    fun `a session started several days later still returns a sane value`() {
        val old = sample(null, 3_000L, at(today.minusDays(3), 22))

        val result = sample(old.state, 9_000L, at(today, 10), sessionId = sessionB)

        assertEquals(today.toString(), result.state.dateIso)
        assertEquals("App đã đóng mấy ngày ⇒ hôm nay bắt đầu lại", 0L, result.todaySteps)
        assertEquals(9_000L, result.state.baseline)
    }

    // ── Giá trị hiển thị ngay khi mở app ───────────────────────

    @Test
    fun `stored steps are shown only when they belong to today`() {
        val state = StepCounterMath.State(
            dateIso = today.toString(),
            sessionId = sessionA,
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
