package com.healthyai.app.presentation.screen.habits

import androidx.annotation.StringRes
import com.healthyai.app.R
import com.healthyai.app.domain.model.HabitTimeOfDay

/** Bộ lọc thói quen; timeOfDay null = hiện tất cả, nhãn lấy từ strings.xml qua [labelRes]. */
enum class HabitFilter(
    @StringRes val labelRes: Int,
    val timeOfDay: HabitTimeOfDay?
) {
    ALL(R.string.habits_filter_all, null),
    MORNING(R.string.habits_filter_morning, HabitTimeOfDay.MORNING),
    AFTERNOON(R.string.habits_filter_afternoon, HabitTimeOfDay.AFTERNOON),
    EVENING(R.string.habits_filter_evening, HabitTimeOfDay.EVENING)
}
