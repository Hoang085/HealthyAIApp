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
 * `areNotificationsEnabled()` là nguồn đúng cho cả 3 tình huống: chưa cấp `POST_NOTIFICATIONS`
 * (Android 13+), người dùng đã tắt thông báo của app trong Settings, hoặc thông báo đang bật.
 */
@Singleton
class NotificationPermissionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationPermissionRepository {

    override fun getStatus(): NotificationPermissionStatus = NotificationPermissionStatus(
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        // Chỉ Android 13+ mới có quyền runtime POST_NOTIFICATIONS cần xin.
        canRequestPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
    )
}
