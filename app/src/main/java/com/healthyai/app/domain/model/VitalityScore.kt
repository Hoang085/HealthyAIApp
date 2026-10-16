package com.healthyai.app.domain.model

/** Chỉ số sinh lực (0–100) kèm mức thay đổi so với tuần trước. */
data class VitalityScore(
    val score: Int = 0,
    val delta: Int? = null,
    val hasData: Boolean = false
)
