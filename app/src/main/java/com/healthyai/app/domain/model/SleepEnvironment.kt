package com.healthyai.app.domain.model

/** Ảnh chụp môi trường quanh điện thoại — đầu vào để app tự suy ra ngủ hay thức, không cần Health Connect và đọc cảm biến không cần quyền. */
data class SleepEnvironment(
    val screenInteractive: Boolean,
    val lightLux: Float,
    val maxMotionG: Float
)
