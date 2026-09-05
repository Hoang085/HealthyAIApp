package com.healthyai.app.data.local

import androidx.room.TypeConverter
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitFrequency
import com.healthyai.app.domain.model.HabitTimeOfDay

class Converters {
    @TypeConverter
    fun fromHabitCategory(value: HabitCategory): String = value.name

    @TypeConverter
    fun toHabitCategory(value: String): HabitCategory = HabitCategory.valueOf(value)

    @TypeConverter
    fun fromHabitTimeOfDay(value: HabitTimeOfDay): String = value.name

    @TypeConverter
    fun toHabitTimeOfDay(value: String): HabitTimeOfDay = HabitTimeOfDay.valueOf(value)

    @TypeConverter
    fun fromHabitFrequency(value: HabitFrequency): String = value.name

    @TypeConverter
    fun toHabitFrequency(value: String): HabitFrequency = HabitFrequency.valueOf(value)
}
