package com.healthyai.app.domain.usecase.sleep

import com.healthyai.app.domain.model.SleepEnvironment
import com.healthyai.app.domain.model.SleepSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Unit test cho [SleepTrackerMath] — số giờ ngủ dễ sai: khung giờ, phân loại, cộng dồn, mốc đêm, số phút hiển thị. */
class SleepTrackerMathTest {

    private val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

    /** Đêm đang xét: ngủ đêm 2026-09-23 → thức dậy sáng 2026-09-24. */
    private val today: LocalDate = LocalDate.of(2026, 9, 24)
    private val yesterday: LocalDate = today.minusDays(1)
    private val tomorrow: LocalDate = today.plusDays(1)

    /** Khung giờ ngủ điển hình (và cũng là mặc định của app): 23:00 → 07:00. */
    private val night: SleepSchedule = SleepSchedule(LocalTime.of(23, 0), LocalTime.of(7, 0))

    /** Khung không vắt qua nửa đêm (người ngủ muộn): 01:00 → 08:00. */
    private val lateShift: SleepSchedule = SleepSchedule(LocalTime.of(1, 0), LocalTime.of(8, 0))

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Long =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    /** Môi trường "phòng tối, điện thoại nằm yên, màn hình tắt" — mẫu lý tưởng của một người đang ngủ. */
    private fun sleeping(
        lightLux: Float = 0.5f,
        maxMotionG: Float = 0.005f,
        screenInteractive: Boolean = false
    ) = SleepEnvironment(
        screenInteractive = screenInteractive,
        lightLux = lightLux,
        maxMotionG = maxMotionG
    )

    /** Lấy mẫu nhanh: chỉ quan tâm mốc thời gian, trạng thái "đang ngủ" và khung giờ. */
    private fun sample(
        state: SleepTrackerMath.State?,
        nowMs: Long,
        asleepNow: Boolean,
        schedule: SleepSchedule = night
    ) = SleepTrackerMath.sample(state, nowMs, asleepNow, zone, schedule)

    // Khung giờ ngủ do người dùng đặt

    @Test
    fun `the sleep window follows the bedtime and wake time the user set`() {
        assertFalse("22:59 chưa tới giờ đi ngủ", SleepTrackerMath.isInSleepWindow(at(today, 22, 59), zone, night))
        assertTrue("23:00 bắt đầu khung ngủ", SleepTrackerMath.isInSleepWindow(at(today, 23), zone, night))
        assertTrue("02:00 vẫn trong khung", SleepTrackerMath.isInSleepWindow(at(today, 2), zone, night))
        assertTrue("06:59 còn trong khung", SleepTrackerMath.isInSleepWindow(at(today, 6, 59), zone, night))
        assertFalse("07:00 đã hết khung", SleepTrackerMath.isInSleepWindow(at(today, 7), zone, night))
        assertFalse("buổi chiều không thuộc khung ngủ", SleepTrackerMath.isInSleepWindow(at(today, 15), zone, night))
    }

    @Test
    fun `a window that does not cross midnight stays inside its own day`() {
        assertFalse("00:30 chưa tới giờ đi ngủ", SleepTrackerMath.isInSleepWindow(at(today, 0, 30), zone, lateShift))
        assertTrue("01:00 bắt đầu khung", SleepTrackerMath.isInSleepWindow(at(today, 1), zone, lateShift))
        assertTrue("07:59 còn trong khung", SleepTrackerMath.isInSleepWindow(at(today, 7, 59), zone, lateShift))
        assertFalse("08:00 đã hết khung", SleepTrackerMath.isInSleepWindow(at(today, 8), zone, lateShift))
        assertFalse("buổi tối đã ngoài khung", SleepTrackerMath.isInSleepWindow(at(today, 20), zone, lateShift))
    }

    @Test
    fun `the night key rolls over at the bedtime, not at noon`() {
        assertEquals(
            "Trước giờ đi ngủ thì vẫn là đêm vừa kết thúc sáng nay",
            today.toString(),
            SleepTrackerMath.nightIsoOf(at(today, 22, 59), zone, night)
        )
        assertEquals(
            "Sau giờ đi ngủ thì là đêm mới (ngày sẽ thức dậy là mai)",
            tomorrow.toString(),
            SleepTrackerMath.nightIsoOf(at(today, 23), zone, night)
        )
        assertEquals(
            "Sáng hôm sau vẫn thuộc đêm đó",
            tomorrow.toString(),
            SleepTrackerMath.nightIsoOf(at(tomorrow, 6), zone, night)
        )
        assertEquals(
            "Buổi chiều vẫn là đêm đã kết thúc sáng nay (không bị xoá lúc 12:00 như trước)",
            tomorrow.toString(),
            SleepTrackerMath.nightIsoOf(at(tomorrow, 15), zone, night)
        )
    }

    @Test
    fun `a window that does not cross midnight keeps the same night key all day`() {
        assertEquals(today.toString(), SleepTrackerMath.nightIsoOf(at(today, 0, 30), zone, lateShift))
        assertEquals(today.toString(), SleepTrackerMath.nightIsoOf(at(today, 7), zone, lateShift))
        assertEquals(today.toString(), SleepTrackerMath.nightIsoOf(at(today, 20), zone, lateShift))
        assertEquals("Sang ngày mới thì khoá đêm mới", tomorrow.toString(), SleepTrackerMath.nightIsoOf(at(tomorrow, 0, 30), zone, lateShift))
    }

    // Phân loại "đang ngủ"

    @Test
    fun `a dark still phone with the screen off inside the window is asleep`() {
        assertTrue(SleepTrackerMath.isAsleep(sleeping(), at(today, 23), zone, night))
    }

    @Test
    fun `using the phone means awake even inside the window`() {
        assertFalse(
            "Màn hình đang được dùng → chắc chắn thức",
            SleepTrackerMath.isAsleep(sleeping(screenInteractive = true), at(today, 23), zone, night)
        )
    }

    @Test
    fun `a lit room means awake even when the phone is still`() {
        assertFalse(
            "Còn sáng (đèn/TV) → chưa ngủ",
            SleepTrackerMath.isAsleep(sleeping(lightLux = 40f), at(today, 23), zone, night)
        )
    }

    @Test
    fun `movement means awake even in a dark room`() {
        assertFalse(
            "Điện thoại còn rung lắc → chưa ngủ",
            SleepTrackerMath.isAsleep(sleeping(maxMotionG = 0.4f), at(today, 23), zone, night)
        )
    }

    @Test
    fun `the same signals outside the sleep window are not sleep`() {
        assertFalse(
            "Trước giờ đi ngủ thì chưa tính là ngủ",
            SleepTrackerMath.isAsleep(sleeping(), at(today, 22, 30), zone, night)
        )
        assertFalse(
            "Buổi chiều thì không tính là ngủ dù mọi tín hiệu khác đều đúng",
            SleepTrackerMath.isAsleep(sleeping(), at(today, 15), zone, night)
        )
    }

    // Cộng dồn thời gian ngủ

    @Test
    fun `the first asleep sample only marks the start, it adds no sleep yet`() {
        val result = sample(null, at(yesterday, 23), asleepNow = true)

        assertEquals(0L, result.sleepMinutes)
        assertTrue(result.state.lastWasAsleep)
        assertEquals("Đêm gắn với ngày thức dậy", today.toString(), result.state.nightIso)
    }

    @Test
    fun `two consecutive asleep samples add the whole interval`() {
        val start = sample(null, at(yesterday, 23), asleepNow = true)

        val next = sample(start.state, at(yesterday, 23, 30), asleepNow = true)

        assertEquals("Nửa tiếng giữa hai mẫu đều 'đang ngủ'", 30L, next.sleepMinutes)
    }

    @Test
    fun `a long gap between two asleep samples is credited in full`() {
        // WorkManager có thể bị hoãn trong Doze → khoảng cách giữa hai mẫu có thể vài giờ.
        val start = sample(null, at(yesterday, 23), asleepNow = true)

        val morning = sample(start.state, at(today, 6), asleepNow = true)

        assertEquals("23:00 → 06:00 là 7 tiếng", 7L * 60L, morning.sleepMinutes)
    }

    @Test
    fun `an awake sample breaks the accumulation`() {
        val start = sample(null, at(yesterday, 23), asleepNow = true)
        val woke = sample(start.state, at(yesterday, 23, 30), asleepNow = false)

        assertEquals(0L, woke.sleepMinutes)

        // Ngủ lại 15 phút sau đó → chỉ tính từ mốc "ngủ lại", không tính khoảng đã thức.
        val asleepAgain = sample(woke.state, at(yesterday, 23, 45), asleepNow = true)
        val later = sample(asleepAgain.state, at(yesterday, 23, 55), asleepNow = true)

        assertEquals("Chỉ 10 phút sau khi ngủ lại", 10L, later.sleepMinutes)
    }

    @Test
    fun `a new night resets the total and archives the night that just ended`() {
        val start = sample(null, at(yesterday, 23), asleepNow = true)
        val morning = sample(start.state, at(today, 6), asleepNow = true)
        assertEquals(7L * 60L, morning.sleepMinutes)

        // 23:00 tối nay: đã sang đêm mới → bắt đầu lại từ 0 và chốt lại đêm vừa xong
        val nextNight = sample(morning.state, at(today, 23), asleepNow = true)

        assertEquals(0L, nextNight.sleepMinutes)
        assertEquals(tomorrow.toString(), nextNight.state.nightIso)
        assertEquals(today.toString(), nextNight.archived?.nightIso)
        assertEquals(7L * 60L, nextNight.archived?.sleepMinutes)
    }

    @Test
    fun `an impossible total is clamped to a sane maximum`() {
        // Cảm biến bị kẹt ở trạng thái "đang ngủ" suốt khung → không được sinh ra số vô lý.
        val start = sample(null, at(yesterday, 23), asleepNow = true)
        val end = sample(start.state, at(today, 22, 59), asleepNow = true)

        assertEquals(SleepTrackerMath.MAX_SLEEP_MINUTES, end.sleepMinutes)
    }

    @Test
    fun `a sample older than the previous one never reduces or inflates the total`() {
        val start = sample(null, at(today, 6), asleepNow = true)
        val backInTime = sample(start.state, at(today, 5), asleepNow = true)

        assertTrue("Khoảng thời gian âm không bao giờ được cộng", backInTime.sleepMinutes >= 0L)
    }

    @Test
    fun `changing the user window keeps the sleep already collected for the same night`() {
        // Đổi khung 23:00–07:00 → 01:00–08:00: khoá đêm vẫn "hôm nay" nên số đã đo không được mất.
        val start = sample(null, at(yesterday, 23), asleepNow = true)
        val morning = sample(start.state, at(today, 6), asleepNow = true)

        val afterChange = SleepTrackerMath.sample(
            state = morning.state,
            nowMs = at(today, 9),
            asleepNow = false,
            zone = zone,
            schedule = lateShift
        )

        assertEquals("Đổi khung giữa chừng không làm mất số đã đo", 7L * 60L, afterChange.sleepMinutes)
        assertEquals(today.toString(), afterChange.state.nightIso)
        assertEquals(
            "Vẫn hiển thị đúng số đó sau khi đổi khung",
            7L * 60L,
            SleepTrackerMath.displaySleepMinutes(afterChange.state, afterChange.archived, at(today, 9), zone, lateShift)
        )
    }

    // Số phút nên hiển thị (ô "Ngủ" ở màn Trang chủ)

    @Test
    fun `sleep of the night that just ended stays visible all day`() {
        val start = sample(null, at(yesterday, 23), asleepNow = true)
        val morning = sample(start.state, at(today, 6), asleepNow = true)
        val afternoon = sample(morning.state, at(today, 15), asleepNow = false)

        assertEquals("Mẫu ban ngày không làm mất số của đêm vừa qua", 0L, afternoon.archived?.sleepMinutes ?: 0L)
        assertEquals(
            7L * 60L,
            SleepTrackerMath.displaySleepMinutes(afternoon.state, afternoon.archived, at(today, 15), zone, night)
        )
    }

    @Test
    fun `a bad night is shown as zero instead of reusing an older night`() {
        // Đêm vừa qua chỉ ngủ được 20 phút (hoặc cảm biến không nhận ra) → phải hiện 0, không lấy số đêm cũ.
        val state = SleepTrackerMath.State(
            nightIso = today.toString(),
            sleepMillis = 20L * 60_000L,
            lastSampleMs = at(today, 6),
            lastWasAsleep = false
        )
        val archived = SleepTrackerMath.NightTotal(yesterday.toString(), 8L * 60L * 60_000L)

        assertEquals(
            20L,
            SleepTrackerMath.displaySleepMinutes(state, archived, at(today, 15), zone, night)
        )
        assertEquals(
            "Và toSleepHours coi đó là 0 giờ vì dưới ngưỡng",
            0.0,
            SleepTrackerMath.toSleepHours(
                SleepTrackerMath.displaySleepMinutes(state, archived, at(today, 15), zone, night)
            ),
            0.001
        )
    }

    @Test
    fun `the evening still shows the finished night until the new one is long enough`() {
        val start = sample(null, at(yesterday, 23), asleepNow = true)
        val morning = sample(start.state, at(today, 6), asleepNow = true)
        // 23:05 tối nay: đêm mới bắt đầu (chốt lại đêm vừa xong vào `archived`)
        val newNight = sample(morning.state, at(today, 23, 5), asleepNow = true)
        // 23:20 → đêm mới mới được 15 phút, chưa đủ ngưỡng 45 phút
        val short = sample(newNight.state, at(today, 23, 20), asleepNow = true)

        assertEquals(15L, short.sleepMinutes)
        assertEquals(
            "Đêm mới chưa đủ ngưỡng → vẫn hiện đêm vừa kết thúc",
            7L * 60L,
            SleepTrackerMath.displaySleepMinutes(short.state, newNight.archived, at(today, 23, 20), zone, night)
        )
    }

    @Test
    fun `once the new night is long enough it replaces the finished night`() {
        val start = sample(null, at(yesterday, 23), asleepNow = true)
        val morning = sample(start.state, at(today, 6), asleepNow = true)
        val newNight = sample(morning.state, at(today, 23, 5), asleepNow = true)
        // 00:25 hôm sau: đêm mới đã ngủ 80 phút (vượt ngưỡng 45)
        val longEnough = sample(newNight.state, at(tomorrow, 0, 25), asleepNow = true)

        assertEquals(80L, longEnough.sleepMinutes)
        assertEquals(
            80L,
            SleepTrackerMath.displaySleepMinutes(longEnough.state, newNight.archived, at(tomorrow, 0, 25), zone, night)
        )
    }

    @Test
    fun `an archived night older than the last one is never shown as tonight's sleep`() {
        // Đêm đã chốt là của HÔM KIA (worker không chạy đêm vừa rồi), còn đang ở tối hôm nay.
        val archived = SleepTrackerMath.NightTotal(
            nightIso = yesterday.toString(),
            sleepMillis = 8L * 60L * 60_000L
        )
        val state = SleepTrackerMath.State(
            nightIso = today.toString(),
            sleepMillis = 0L,
            lastSampleMs = at(today, 22),
            lastWasAsleep = false
        )

        assertEquals(
            "Số của một đêm cũ hơn không được hiện như đêm vừa qua",
            0L,
            SleepTrackerMath.displaySleepMinutes(state, archived, at(today, 22), zone, night)
        )
    }

    // Giá trị đọc ra để hiển thị / lưu lịch sử

    @Test
    fun `stored sleep is shown only for the night it belongs to`() {
        val state = SleepTrackerMath.State(
            nightIso = today.toString(),
            sleepMillis = 7L * 60L * 60_000L + 30L * 60_000L, // 7h30
            lastSampleMs = at(today, 6),
            lastWasAsleep = false
        )

        assertEquals(450L, SleepTrackerMath.storedSleepMinutes(state, today.toString()))
        assertEquals("Đã sang đêm mới thì không hiện số của đêm qua", 0L, SleepTrackerMath.storedSleepMinutes(state, tomorrow.toString()))
    }

    @Test
    fun `stored sleep is zero when nothing was stored yet`() {
        assertEquals(0L, SleepTrackerMath.storedSleepMinutes(null, today.toString()))
    }

    @Test
    fun `a short rest is not reported as sleep`() {
        assertEquals(
            "Dưới ${SleepTrackerMath.MIN_SLEEP_MINUTES} phút thì không coi là ngủ",
            0.0,
            SleepTrackerMath.toSleepHours(SleepTrackerMath.MIN_SLEEP_MINUTES - 1L),
            0.001
        )
        assertEquals("Đúng ngưỡng thì đã tính", 0.8, SleepTrackerMath.toSleepHours(45L), 0.001)
    }

    @Test
    fun `sleep hours are rounded to one decimal place`() {
        assertEquals("435 phút = 7,25 giờ → 7,3", 7.3, SleepTrackerMath.toSleepHours(435L), 0.001)
        assertEquals("7 giờ đúng", 7.0, SleepTrackerMath.toSleepHours(420L), 0.001)
    }
}
