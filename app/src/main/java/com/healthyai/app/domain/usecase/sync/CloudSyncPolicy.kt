package com.healthyai.app.domain.usecase.sync

import com.healthyai.app.domain.model.CloudSyncSettings
import com.healthyai.app.domain.model.HabitLog

/**
 * **Cổng riêng tư** cho việc tải nhật ký thói quen lên Firestore (mục 3 #31).
 *
 * Trước đây `SyncWorker` đẩy **mọi** log có `isDirty = true` lên đám mây ngay khi có phiên đăng
 * nhập — mà mọi log mặc định đều `isDirty = true`, nên một người dùng đã ghi nhật ký ở **chế độ
 * khách** rồi đăng nhập sẽ bị đẩy **toàn bộ lịch sử** lên đám mây mà không hề được hỏi.
 *
 * Nay việc tải lên phải qua hai điều kiện:
 * 1. người dùng đã **bật** đồng bộ đám mây ([CloudSyncSettings.enabled]);
 * 2. log đó được tạo **sau** mốc đồng ý ([CloudSyncSettings.consentAtMillis]).
 *
 * Hàm thuần Kotlin nên test được trên JVM, và là **một chỗ duy nhất** quyết định log nào được phép
 * rời khỏi thiết bị.
 */
object CloudSyncPolicy {

    /**
     * Danh sách log **được phép** tải lên theo [settings].
     *
     * Trả về **rỗng** (không tải gì) khi người dùng chưa bật đồng bộ, hoặc khi thiếu mốc đồng ý —
     * thiếu dữ liệu thì chọn hướng **an toàn** (không tải) thay vì tải hết.
     */
    fun logsToUpload(logs: List<HabitLog>, settings: CloudSyncSettings): List<HabitLog> {
        if (!settings.enabled) return emptyList()
        val consentAt = settings.consentAtMillis ?: return emptyList()
        return logs.filter { it.loggedAt >= consentAt }
    }
}
