package com.healthyai.app.domain.model

/** Thiết lập đồng bộ nhật ký lên Firestore (mặc định tắt): chỉ tải log tạo từ lúc bật (consentAtMillis) trở đi. */
data class CloudSyncSettings(
    val enabled: Boolean = false,
    val consentAtMillis: Long? = null
)
