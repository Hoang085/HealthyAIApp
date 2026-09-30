package com.healthyai.app.domain.model

import java.time.LocalTime

/**
 * **Khung giờ ngủ do người dùng đặt** — app chỉ đo giấc ngủ trong khung này.
 *
 * Trước đây app tự quyết định khung đêm (18:00 → 12:00 hôm sau); nay người dùng chọn *giờ đi ngủ* và
 * *giờ thức* của mình, còn app chỉ làm phần đo đạc. Nhờ vậy thời gian ngủ không còn bị tính nhầm khi
 * người dùng ngủ muộn/thức khuya, và người dùng hiểu rõ app đang đo khoảng nào.
 *
 * Nửa mở: **có** giờ đi ngủ, **không** có giờ thức (thức dậy đúng 07:00 thì 07:00 không còn là ngủ).
 *
 * @property bedtime Giờ đi ngủ (bắt đầu khung đo).
 * @property wakeTime Giờ thức (kết thúc khung đo).
 */
data class SleepSchedule(
    val bedtime: LocalTime,
    val wakeTime: LocalTime
) {

    /**
     * Khung có **vắt qua nửa đêm** hay không — đúng khi giờ đi ngủ **muộn hơn** giờ thức
     * (ví dụ 23:00 → 07:00). Khung không vắt (ví dụ 01:00 → 08:00) nằm gọn trong một ngày.
     */
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

    /**
     * Khung có dùng được không. Chỉ chặn trường hợp **trùng giờ** (khung rỗng 0 phút) — mọi khung khác
     * đều là lựa chọn của người dùng, app tôn trọng.
     */
    val isValid: Boolean get() = bedtime != wakeTime

    companion object {
        private const val MINUTES_PER_DAY = 24L * 60L

        /** Khung mặc định khi người dùng chưa đặt gì: **23:00 → 07:00**. */
        val DEFAULT: SleepSchedule = SleepSchedule(LocalTime.of(23, 0), LocalTime.of(7, 0))
    }
}
