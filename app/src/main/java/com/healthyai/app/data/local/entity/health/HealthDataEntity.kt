package com.healthyai.app.data.local.entity.health

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.healthyai.app.domain.model.HealthData

/** Lưu dữ liệu sức khỏe đã đồng bộ từ Health Connect để theo dõi lịch sử và xu hướng. */
@Entity(tableName = "health_data_history")
data class HealthDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val steps: Long,
    val sleepHours: Double,
    val heartRate: Double,
    val activeCalories: Double,
    val recordedAt: Long // Timestamp khi dữ liệu được ghi nhận (thường là cuối ngày)
) {
    fun toDomain() = HealthData(
        steps = steps,
        sleepHours = sleepHours,
        heartRate = heartRate,
        activeCalories = activeCalories,
        recordedAt = recordedAt
    )

    companion object {
        fun fromDomain(data: HealthData) = HealthDataEntity(
            steps = data.steps,
            sleepHours = data.sleepHours,
            heartRate = data.heartRate,
            activeCalories = data.activeCalories,
            recordedAt = data.recordedAt
        )
    }
}
