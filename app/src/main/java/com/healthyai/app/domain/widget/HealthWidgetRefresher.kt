package com.healthyai.app.domain.widget

/** Cổng để tầng Domain báo widget "Sức khỏe hôm nay" vẽ lại khi dữ liệu sức khỏe thay đổi. */
interface HealthWidgetRefresher {

    /** Yêu cầu widget đọc lại dữ liệu và vẽ lại; không bao giờ ném lỗi để không làm hỏng luồng ghi dữ liệu. */
    suspend fun refresh()
}
