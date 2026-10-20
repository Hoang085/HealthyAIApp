package com.healthyai.app.presentation.ui.format

import com.healthyai.app.domain.model.SleepSchedule
import java.time.LocalTime

/** Chuỗi giờ:phút kiểu 24 giờ ("23:00"), tự dựng chuỗi vì String.format phụ thuộc locale. */
fun formatClockTime(time: LocalTime): String =
    "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

/** Khung giờ ngủ dạng `"23:00 – 07:00"`. */
fun formatSleepWindow(schedule: SleepSchedule): String =
    "${formatClockTime(schedule.bedtime)} – ${formatClockTime(schedule.wakeTime)}"

/** Độ dài khung giờ ngủ ("8 giờ", "7 giờ 30 phút"); bỏ phần 0 giờ/0 phút cho gọn. */
fun formatSleepWindowDuration(minutes: Long): String {
    val safe = minutes.coerceAtLeast(0L)
    val hours = safe / 60L
    val rest = safe % 60L

    return when {
        hours == 0L -> "$rest phút"
        rest == 0L -> "$hours giờ"
        else -> "$hours giờ $rest phút"
    }
}
