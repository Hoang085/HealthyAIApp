package com.healthyai.app.presentation.ui.format

import com.healthyai.app.domain.model.SleepSchedule
import java.time.LocalTime

/**
 * Chuỗi hiển thị **giờ:phút** kiểu 24 giờ (`LocalTime.of(23, 0)` → `"23:00"`).
 *
 * Không dùng `String.format`/`DateTimeFormatter` vì chúng phụ thuộc locale: máy đặt `vi-VN` hay một số
 * locale dùng chữ số khác sẽ cho ra chuỗi khác nhau, còn tự dựng chuỗi thì luôn giống nhau — cùng quy
 * tắc với `formatStepCount`/`formatTargetValue` ở `NumberDisplay.kt`.
 */
fun formatClockTime(time: LocalTime): String =
    "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

/** Khung giờ ngủ dạng `"23:00 – 07:00"`. */
fun formatSleepWindow(schedule: SleepSchedule): String =
    "${formatClockTime(schedule.bedtime)} – ${formatClockTime(schedule.wakeTime)}"

/**
 * Độ dài khung giờ ngủ: `"8 giờ"`, `"7 giờ 30 phút"`, `"45 phút"`.
 *
 * Bỏ phần `0 phút` khi độ dài chẵn giờ, và bỏ phần `0 giờ` khi ngắn hơn 1 giờ.
 */
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
