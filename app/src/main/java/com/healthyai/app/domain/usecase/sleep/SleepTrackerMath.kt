package com.healthyai.app.domain.usecase.sleep

import com.healthyai.app.domain.model.SleepEnvironment
import com.healthyai.app.domain.model.SleepSchedule
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Ước lượng số giờ ngủ từ cảm biến điện thoại (màn hình/tối/bất động) theo khung giờ ngủ người dùng đặt. */
object SleepTrackerMath {

    /** Dưới ngưỡng này (lux) coi là phòng tối. Để thấp nhằm tránh nhận nhầm phòng có đèn/TV là đang ngủ. */
    const val DARK_LUX = 5f

    /** Độ lệch gia tốc lớn nhất (đơn vị `g`) còn coi là "điện thoại bất động". */
    const val STILL_MOTION_G = 0.05f

    /** Dưới ngưỡng này không coi là một giấc ngủ (lọc nhiễu: ngồi yên một lúc không phải ngủ). */
    const val MIN_SLEEP_MINUTES = 45L

    /** Trần hợp lý cho một đêm ngủ (chặn dữ liệu vô lý khi cảm biến bị kẹt/điện thoại bị bỏ quên). */
    const val MAX_SLEEP_MINUTES = 12L * 60L

    private const val MILLIS_PER_MINUTE = 60_000L

    /** Trạng thái cộng dồn của đêm đang xét, lưu trong SharedPreferences. */
    data class State(
        val nightIso: String,
        val sleepMillis: Long,
        val lastSampleMs: Long,
        val lastWasAsleep: Boolean
    ) {
        val sleepMinutes: Long get() = sleepMillis / MILLIS_PER_MINUTE
    }

    /** Một đêm đã chốt, giữ lại để ô Ngủ ở Trang chủ không nhảy về 0 khi đêm mới vừa bắt đầu. */
    data class NightTotal(val nightIso: String, val sleepMillis: Long) {
        val sleepMinutes: Long get() = sleepMillis / MILLIS_PER_MINUTE
    }

    data class Result(
        val state: State,
        val sleepMillis: Long,
        /** Đêm vừa khép lại ở mẫu này; `null` nếu mẫu này không làm chuyển đêm. */
        val archived: NightTotal? = null
    ) {
        val sleepMinutes: Long get() = sleepMillis / MILLIS_PER_MINUTE
    }

    /** Đêm mà thời điểm nowMs thuộc về, theo khung giờ người dùng đặt. */
    fun nightIsoOf(nowMs: Long, zone: ZoneId, schedule: SleepSchedule): String {
        val local = Instant.ofEpochMilli(nowMs).atZone(zone)
        val date = if (schedule.wrapsMidnight && local.toLocalTime() >= schedule.bedtime) {
            local.toLocalDate().plusDays(1)
        } else {
            local.toLocalDate()
        }
        return date.toString()
    }

    /** [nowMs] có nằm trong khung giờ ngủ của người dùng hay không. */
    fun isInSleepWindow(nowMs: Long, zone: ZoneId, schedule: SleepSchedule): Boolean =
        schedule.contains(Instant.ofEpochMilli(nowMs).atZone(zone).toLocalTime())

    /** Ba tín hiệu của [environment] có đủ để kết luận "đang ngủ" tại thời điểm [nowMs] hay không. */
    fun isAsleep(
        environment: SleepEnvironment,
        nowMs: Long,
        zone: ZoneId,
        schedule: SleepSchedule
    ): Boolean =
        isInSleepWindow(nowMs, zone, schedule) &&
            !environment.screenInteractive &&
            environment.lightLux < DARK_LUX &&
            environment.maxMotionG < STILL_MOTION_G

    /** Cập nhật trạng thái với một mẫu mới, cộng dồn thời gian giữa hai mẫu đều "đang ngủ". */
    fun sample(
        state: State?,
        nowMs: Long,
        asleepNow: Boolean,
        zone: ZoneId,
        schedule: SleepSchedule
    ): Result {
        val night = nightIsoOf(nowMs, zone, schedule)
        val previous = state?.takeIf { it.nightIso == night }

        val archived = if (state != null && previous == null && state.sleepMillis > 0L) {
            NightTotal(nightIso = state.nightIso, sleepMillis = state.sleepMillis)
        } else {
            null
        }

        // Chỉ cộng phần thời gian nằm giữa hai mẫu "đang ngủ" liên tiếp.
        val carriedMillis = if (previous != null && previous.lastWasAsleep && asleepNow) {
            (nowMs - previous.lastSampleMs).coerceAtLeast(0L)
        } else {
            0L
        }

        val totalMillis = ((previous?.sleepMillis ?: 0L) + carriedMillis)
            .coerceIn(0L, MAX_SLEEP_MINUTES * MILLIS_PER_MINUTE)

        return Result(
            state = State(
                nightIso = night,
                sleepMillis = totalMillis,
                lastSampleMs = nowMs,
                lastWasAsleep = asleepNow
            ),
            sleepMillis = totalMillis,
            archived = archived
        )
    }

    /** Số phút ngủ của đúng đêm nightIso theo trạng thái đã lưu; 0 nếu trạng thái thuộc đêm khác. */
    fun storedSleepMinutes(state: State?, nightIso: String): Long =
        if (state != null && state.nightIso == nightIso) state.sleepMinutes else 0L

    /** Số phút ngủ nên hiển thị; trong khung ngủ chưa đủ ngưỡng thì giữ số đêm vừa kết thúc để không nhảy về 0. */
    fun displaySleepMinutes(
        state: State?,
        archived: NightTotal?,
        nowMs: Long,
        zone: ZoneId,
        schedule: SleepSchedule
    ): Long {
        val currentNight = nightIsoOf(nowMs, zone, schedule)
        val ongoing = storedSleepMinutes(state, currentNight)

        if (!isInSleepWindow(nowMs, zone, schedule)) return ongoing
        if (ongoing >= MIN_SLEEP_MINUTES) return ongoing

        val finishedNight = LocalDate.parse(currentNight).minusDays(1).toString()
        val finished = storedSleepMinutes(state, finishedNight).takeIf { it > 0L }
            ?: archived?.takeIf { it.nightIso == finishedNight }?.sleepMinutes

        return if (finished != null && finished >= MIN_SLEEP_MINUTES) finished else ongoing
    }

    /** Đổi số phút ngủ thành số giờ (làm tròn 1 chữ số thập phân); dưới ngưỡng thì trả 0.0. */
    fun toSleepHours(minutes: Long): Double {
        if (minutes < MIN_SLEEP_MINUTES) return 0.0
        return Math.round(minutes / 60.0 * 10.0) / 10.0
    }
}
