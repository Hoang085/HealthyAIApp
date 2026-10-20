package com.healthyai.app.domain.usecase.sync

import com.healthyai.app.domain.model.CloudSyncSettings
import com.healthyai.app.domain.model.HabitLog

/** Cổng riêng tư quyết định log nào được tải lên Firestore: chỉ khi đã bật đồng bộ và tạo sau mốc đồng ý. */
object CloudSyncPolicy {

    /** Danh sách log được phép tải lên; trả rỗng (hướng an toàn) khi chưa bật đồng bộ hoặc thiếu mốc đồng ý. */
    fun logsToUpload(logs: List<HabitLog>, settings: CloudSyncSettings): List<HabitLog> {
        if (!settings.enabled) return emptyList()
        val consentAt = settings.consentAtMillis ?: return emptyList()
        return logs.filter { it.loggedAt >= consentAt }
    }
}
