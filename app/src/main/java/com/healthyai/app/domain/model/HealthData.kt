package com.healthyai.app.domain.model

/**
 * Dữ liệu tổng hợp từ Health Connect (bước chân, giấc ngủ, nhịp tim...).
 * Được đọc từ Android OS thông qua Health Connect API (không qua mạng).
 */
data class HealthData(
    val steps: Long = 0,
    val sleepHours: Double = 0.0,
    val heartRate: Double = 0.0,
    val activeCalories: Double = 0.0,
    val recordedAt: Long = System.currentTimeMillis()
)
