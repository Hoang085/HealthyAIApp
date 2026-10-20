package com.healthyai.app.presentation.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.healthyai.app.domain.widget.HealthWidgetRefresher
import com.healthyai.app.util.runCatchingCancellable
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "HealthWidgetRefresher"

/**
 * Vẽ lại widget "Sức khỏe hôm nay" bằng số liệu **đọc mới** tại thời điểm gọi.
 *
 * Không dùng `HealthWidget().updateAll(context)` trần: hàm đó chỉ gửi yêu cầu vẽ lại cho session
 * đang chạy, mà session **không chạy lại `provideGlance`** ⇒ composition vẫn dùng ảnh chụp dữ liệu
 * cũ, widget đứng im ở số của lần vẽ đầu. Vì vậy ở đây làm đúng hai bước:
 * 1. ghi số liệu mới vào **Glance state** của từng widget ([writeHealthWidgetSummary]);
 * 2. gọi `update(glanceId)` để Glance nạp lại state và vẽ lại (nếu widget chưa có session — ví dụ
 *    tiến trình vừa bị hệ thống giết — thì `update` sẽ chạy `provideGlance` và đọc số liệu luôn).
 */
@Singleton
class HealthWidgetRefresherImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    /**
     * [Lazy] để **phá vòng phụ thuộc**: [HealthWidgetSummaryReader] cần `StepCounterRepository`, mà
     * `StepCounterRepositoryImpl` lại cần [HealthWidgetRefresher] (để vẽ lại widget khi bước chân
     * đổi). Dagger cắt vòng được bằng `Lazy`/`Provider` ở một cạnh; reader chỉ được tạo ở lần
     * `refresh` đầu tiên nên không cần có sẵn lúc dựng đồ thị.
     */
    private val summaryReader: Lazy<HealthWidgetSummaryReader>
) : HealthWidgetRefresher {

    override suspend fun refresh() {
        runCatchingCancellable {
            val summary = summaryReader.get().read()
            val widget = HealthWidget()
            GlanceAppWidgetManager(context)
                .getGlanceIds(HealthWidget::class.java)
                .forEach { glanceId ->
                    writeHealthWidgetSummary(context, glanceId, summary)
                    widget.update(context, glanceId)
                }
        }.onFailure { Log.w(TAG, "Không vẽ lại được widget", it) }
    }
}
