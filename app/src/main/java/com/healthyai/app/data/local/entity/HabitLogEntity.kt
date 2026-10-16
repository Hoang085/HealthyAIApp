package com.healthyai.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import com.healthyai.app.domain.model.HabitCategory

/** Room Entity bảng "habit_logs"; isDirty = true nghĩa là chưa đồng bộ lên cloud (Last Write Wins). */
@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE // Xóa habit → tự xóa log liên quan
        )
    ],
    indices = [Index(value = ["habitId"]), Index(value = ["loggedAt"])]
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val habitName: String,
    val category: HabitCategory,
    val value: Float,
    val unit: String,
    val targetValueAtTime: Float, // Lưu mục tiêu tại thời điểm log để phân tích lịch sử
    val loggedAt: Long = System.currentTimeMillis(),
    val note: String = "",
    val isDirty: Boolean = true  // Offline-first: true = chưa sync lên Firebase
)
