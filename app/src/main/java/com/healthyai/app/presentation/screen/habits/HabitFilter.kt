package com.healthyai.app.presentation.screen.habits

import androidx.annotation.StringRes
import com.healthyai.app.R
import com.healthyai.app.domain.model.HabitTimeOfDay

/**
 * Bộ lọc thói quen trên màn Thói quen.
 *
 * Trước đây bộ lọc là **chuỗi tiếng Việt viết cứng** ("Tất cả"/"Sáng"/"Chiều"/"Tối") vừa dùng làm
 * khoá so sánh trong `HabitsViewModel` vừa là nhãn hiển thị, nên không thể dịch và rất dễ lệch khi đổi
 * chữ (phụ lục C #41). Nay khoá là enum, nhãn nằm ở `res/values/strings.xml` qua [labelRes].
 *
 * @property timeOfDay giá trị `timeOfDay` cần lọc; `null` = không lọc (hiện tất cả).
 */
enum class HabitFilter(
    @StringRes val labelRes: Int,
    val timeOfDay: HabitTimeOfDay?
) {
    ALL(R.string.habits_filter_all, null),
    MORNING(R.string.habits_filter_morning, HabitTimeOfDay.MORNING),
    AFTERNOON(R.string.habits_filter_afternoon, HabitTimeOfDay.AFTERNOON),
    EVENING(R.string.habits_filter_evening, HabitTimeOfDay.EVENING)
}
