package com.healthyai.app.domain.model

import java.time.LocalTime

/** Khung giờ ngủ do người dùng đặt; khung nửa mở nên giờ thức không tính là đang ngủ. */
data class SleepSchedule(
    val bedtime: LocalTime,
    val wakeTime: LocalTime
) {

    /** Khung có vắt qua nửa đêm hay không (giờ đi ngủ muộn hơn giờ thức, ví dụ 23:00 → 07:00). */
    val wrapsMidnight: Boolean get() = bedtime > wakeTime

    /** [time] có nằm trong khung ngủ hay không. */
    fun contains(time: LocalTime): Boolean =
        if (wrapsMidnight) {
            time >= bedtime || time < wakeTime
        } else {
            time >= bedtime && time < wakeTime
        }

    /** Số phút của khung, tính theo chiều kim đồng hồ từ [bedtime] tới [wakeTime]. */
    val durationMinutes: Long
        get() {
            val minutes = (wakeTime.toSecondOfDay() - bedtime.toSecondOfDay()) / 60
            return if (minutes > 0) minutes.toLong() else minutes.toLong() + MINUTES_PER_DAY
        }

    /** Khung dùng được không; chỉ chặn trường hợp trùng giờ (khung rỗng), mọi khung khác đều là lựa chọn hợp lệ. */
    val isValid: Boolean get() = bedtime != wakeTime

    companion object {
        private const val MINUTES_PER_DAY = 24L * 60L

        /** Khung mặc định khi người dùng chưa đặt gì: 23:00 → 07:00. */
        val DEFAULT: SleepSchedule = SleepSchedule(LocalTime.of(23, 0), LocalTime.of(7, 0))
    }
}
