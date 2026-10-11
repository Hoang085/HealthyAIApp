package com.healthyai.app.domain.widget

/**
 * Cổng (port) để tầng Domain báo cho **widget màn hình chính "Sức khỏe hôm nay"** biết dữ liệu sức khỏe
 * vừa thay đổi.
 *
 * Vì sao cần: `HealthWidget.provideGlance()` chỉ đọc dữ liệu **một lần** lúc widget được vẽ và
 * `health_widget_info.xml` đặt `updatePeriodMillis` = 30 phút (mức nhỏ nhất Android chấp nhận) để tiết
 * kiệm pin, nên nếu không ai gọi `updateAll()` thì sau khi người dùng vừa đo nhịp tim hoặc vừa đồng bộ
 * xong, widget vẫn hiện số cũ tới nửa tiếng.
 *
 * Domain chỉ biết interface này; implementation thật (Glance `updateAll`) nằm ở tầng Presentation
 * (`presentation/widget/HealthWidgetRefresherImpl`) và được bind trong `di/AppModule`.
 */
interface HealthWidgetRefresher {

    /**
     * Yêu cầu widget đọc lại dữ liệu sức khỏe hôm nay và vẽ lại nội dung.
     *
     * **Không bao giờ ném lỗi**: lỗi vẽ widget không được phép làm hỏng luồng ghi dữ liệu (nếu ném,
     * việc đồng bộ sức khỏe hoặc lần đo nhịp tim sẽ báo thất bại dù dữ liệu đã vào DB).
     */
    suspend fun refresh()
}
