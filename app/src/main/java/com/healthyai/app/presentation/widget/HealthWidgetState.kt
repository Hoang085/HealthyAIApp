package com.healthyai.app.presentation.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.updateAppWidgetState
import com.healthyai.app.domain.usecase.health.TodayHealthSummary

/**
 * Ba chỉ số của widget được lưu trong **Glance state** của từng widget (mỗi widget một file
 * `appWidget-<id>`) chứ không phải biến trong closure của `provideGlance`.
 *
 * Lý do: Glance **không chạy lại** `provideGlance` khi session đang sống — `update()`/`updateAll()`
 * chỉ gửi event `UpdateGlanceState` để composition đọc lại state rồi vẽ lại. Vì vậy dữ liệu đọc một
 * lần trong `provideGlance` mà render thẳng từ biến là dữ liệu "chết": widget giữ nguyên số của lần
 * vẽ đầu (thường là `0` lúc mới đặt widget) dù app đã đếm thêm bước.
 *
 * Luồng đúng: `HealthWidgetSummaryReader` đọc số mới → ghi vào state dưới đây → `update(glanceId)`;
 * composition đọc state qua `currentState()` nên mỗi lần ghi là một lần widget tự vẽ lại.
 */
internal val WidgetStepsKey = longPreferencesKey("health_widget_steps")

internal val WidgetSleepHoursKey = doublePreferencesKey("health_widget_sleep_hours")

internal val WidgetHeartRateKey = doublePreferencesKey("health_widget_heart_rate_bpm")

/** Số liệu đã lưu của widget; `null` ở cả ba khoá nghĩa là widget chưa từng được ghi state. */
internal fun Preferences.toTodayHealthSummaryOrNull(): TodayHealthSummary? {
    val steps = this[WidgetStepsKey] ?: return null
    val sleepHours = this[WidgetSleepHoursKey] ?: return null
    val heartRateBpm = this[WidgetHeartRateKey] ?: return null
    return TodayHealthSummary(steps = steps, sleepHours = sleepHours, heartRateBpm = heartRateBpm)
}

/**
 * Ghi [summary] vào state của widget [glanceId].
 *
 * Chỉ ghi state — nơi gọi phải tự gọi `update(glanceId)` để widget vẽ lại; tách như vậy để lần vẽ
 * đầu (trong `provideGlance`) không gọi `update()` lồng vào chính session đang chạy.
 */
internal suspend fun writeHealthWidgetSummary(
    context: Context,
    glanceId: GlanceId,
    summary: TodayHealthSummary
) {
    updateAppWidgetState(context, glanceId) { prefs ->
        prefs[WidgetStepsKey] = summary.steps
        prefs[WidgetSleepHoursKey] = summary.sleepHours
        prefs[WidgetHeartRateKey] = summary.heartRateBpm
    }
}
