package com.healthyai.app.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.healthyai.app.domain.model.NotificationPermissionStatus
import com.healthyai.app.domain.repository.NotificationPermissionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Triển khai [NotificationPermissionRepository] trên `NotificationManagerCompat`.
 *
 * Lớp này **chỉ đọc các tín hiệu thô** của hệ thống; việc suy ra trạng thái nằm ở
 * [NotificationPermissionStatus.from] — hàm thuần đó mới là chỗ khoá được hành vi "thẻ Bật nhắc nhở
 * hàng ngày" bằng unit test JVM (KB-36/37/38).
 *
 * ⚠️ Vì sao chỉ `areNotificationsEnabled()` là **không đủ**: trên Android 13+, app chưa tạo notification
 * channel nào và chưa từng xin quyền thì cờ thông báo cấp app **chưa bị đặt**, nên hàm này trả `true`.
 * Hệ quả thực tế đã gặp (KB-36/37/38): thẻ **không bao giờ hiện** trên bản cài mới vì
 * `notificationsEnabled` bị coi là `true`. Đó là lý do phải kiểm thêm quyền runtime trên API 33+.
 */
@Singleton
class NotificationPermissionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationPermissionRepository {

    override fun getStatus(): NotificationPermissionStatus = NotificationPermissionStatus.from(
        sdkInt = Build.VERSION.SDK_INT,
        postNotificationsPermissionGranted = hasPostNotificationsPermission(),
        appNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    )

    /**
     * Quyền runtime `POST_NOTIFICATIONS` (chỉ có từ Android 13+) đã được cấp chưa.
     *
     * Trên Android ≤ 12 luôn trả `false` — nhưng [NotificationPermissionStatus.from] **bỏ qua** giá trị
     * này khi `sdkInt < 33`, nên kết quả vẫn đúng (bản cũ không có quyền runtime để xin).
     */
    private fun hasPostNotificationsPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
}
