package com.healthyai.app.domain.widget

/**
 * Cổng (port) để tầng Domain báo cho **widget màn hình chính** biết dữ liệu thói quen vừa thay đổi.
 *
 * Vì sao cần: `HabitWidget.provideGlance()` chỉ đọc DB **một lần** lúc widget được vẽ
 * (`getAllActiveHabits().first()`) và `habit_widget_info.xml` đặt `updatePeriodMillis="0"` để tiết
 * kiệm pin, nên hệ thống **không** tự vẽ lại widget. Nếu không ai gọi `updateAll()`, widget đứng yên
 * ở dữ liệu cũ cho tới khi người dùng gỡ ra thêm lại (mục 3 #15 — TC-WG-04).
 *
 * Domain chỉ biết interface này; implementation thật (Glance `updateAll`) nằm ở tầng Presentation
 * (`presentation/widget/HabitWidgetRefresherImpl`) và được bind trong `di/AppModule`.
 */
interface HabitWidgetRefresher {

    /**
     * Yêu cầu widget đọc lại bảng `habits` từ Room và vẽ lại nội dung.
     *
     * **Không bao giờ ném lỗi**: lỗi vẽ widget không được phép làm hỏng luồng ghi dữ liệu
     * (nếu ném, màn Thêm/Sửa thói quen sẽ báo "Lỗi khi lưu" dù dữ liệu đã vào DB).
     */
    suspend fun refresh()
}
