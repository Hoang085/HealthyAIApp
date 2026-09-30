package com.healthyai.app.domain.model

/**
 * Một lần "chụp" môi trường quanh điện thoại — đầu vào để app **tự suy ra** người dùng đang ngủ hay
 * thức, không cần Health Connect và không cần ứng dụng sức khỏe nào khác.
 *
 * Cả ba tín hiệu đều đọc được **không cần quyền**:
 * - trạng thái màn hình ([screenInteractive]) — `PowerManager.isInteractive`;
 * - độ sáng môi trường ([lightLux]) — `Sensor.TYPE_LIGHT`;
 * - độ bất động ([maxMotionG]) — `Sensor.TYPE_ACCELEROMETER`.
 *
 * Việc phân loại "ngủ" nằm ở hàm thuần `SleepTrackerMath.isAsleep` (test được trên JVM).
 *
 * @property screenInteractive Màn hình đang bật và có thể nhận tương tác hay không. Điện thoại đang
 *   được dùng thì **chắc chắn** người dùng đang thức.
 * @property lightLux Độ sáng môi trường đo được (lux). Máy **không có** cảm biến ánh sáng trả `0f`
 *   (coi như "tối") — khi đó hai tín hiệu còn lại (màn hình tắt + bất động) vẫn phải đúng.
 * @property maxMotionG Độ lệch **lớn nhất** khỏi 1g của gia tốc trong cửa sổ lấy mẫu (đơn vị `g`).
 *   Điện thoại nằm yên trên đệm cho giá trị rất nhỏ (≈ 0); cầm lên / đi lại cho giá trị lớn.
 */
data class SleepEnvironment(
    val screenInteractive: Boolean,
    val lightLux: Float,
    val maxMotionG: Float
)
