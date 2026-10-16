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

/** Triển khai [NotificationPermissionRepository]; Android 13+ chưa xin quyền vẫn có areNotificationsEnabled() = true nên phải kiểm thêm quyền runtime. */
@Singleton
class NotificationPermissionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationPermissionRepository {

    override fun getStatus(): NotificationPermissionStatus = NotificationPermissionStatus.from(
        sdkInt = Build.VERSION.SDK_INT,
        postNotificationsPermissionGranted = hasPostNotificationsPermission(),
        appNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    )

    /** Quyền runtime POST_NOTIFICATIONS (Android 13+) đã cấp chưa; ≤ 12 luôn false nhưng [NotificationPermissionStatus.from] bỏ qua khi sdkInt < 33. */
    private fun hasPostNotificationsPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
}
