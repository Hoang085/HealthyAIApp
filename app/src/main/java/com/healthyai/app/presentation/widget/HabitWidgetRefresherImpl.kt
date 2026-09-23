package com.healthyai.app.presentation.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.healthyai.app.domain.widget.HabitWidgetRefresher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "HabitWidgetRefresher"

/**
 * Bản triển khai thật của [HabitWidgetRefresher] bằng Glance.
 *
 * `updateAll()` vẽ lại **tất cả** widget "Tiến độ thói quen" đang có trên màn hình chính; mỗi lần vẽ
 * sẽ chạy lại `HabitWidget.provideGlance()` nên dữ liệu được đọc mới từ Room. Nếu người dùng chưa
 * thêm widget nào thì `updateAll()` không làm gì (danh sách `GlanceId` rỗng).
 */
@Singleton
class HabitWidgetRefresherImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : HabitWidgetRefresher {

    override suspend fun refresh() {
        runCatching { HabitWidget().updateAll(context) }
            .onFailure { Log.w(TAG, "Không vẽ lại được widget", it) }
    }
}
