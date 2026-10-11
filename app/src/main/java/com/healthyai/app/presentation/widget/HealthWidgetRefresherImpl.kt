package com.healthyai.app.presentation.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.healthyai.app.domain.widget.HealthWidgetRefresher
import com.healthyai.app.util.runCatchingCancellable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "HealthWidgetRefresher"

/**
 * Bản triển khai thật của [HealthWidgetRefresher] bằng Glance.
 *
 * `updateAll()` vẽ lại **tất cả** widget "Sức khỏe hôm nay" đang có trên màn hình chính; mỗi lần vẽ sẽ
 * chạy lại `HealthWidget.provideGlance()` nên số liệu được đọc mới. Nếu người dùng chưa thêm widget nào
 * thì `updateAll()` không làm gì (danh sách `GlanceId` rỗng).
 */
@Singleton
class HealthWidgetRefresherImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : HealthWidgetRefresher {

    override suspend fun refresh() {
        runCatchingCancellable { HealthWidget().updateAll(context) }
            .onFailure { Log.w(TAG, "Không vẽ lại được widget", it) }
    }
}
