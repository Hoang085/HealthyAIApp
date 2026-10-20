package com.healthyai.app.domain.model

/** Dữ liệu sức khỏe tổng hợp đọc từ Health Connect API (không qua mạng): bước chân, giấc ngủ, nhịp tim. */
data class HealthData(
    val steps: Long = 0,
    val sleepHours: Double = 0.0,
    val heartRate: Double = 0.0,
    val activeCalories: Double = 0.0,
    val recordedAt: Long = System.currentTimeMillis()
)
