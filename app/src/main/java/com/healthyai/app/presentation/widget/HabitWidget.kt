package com.healthyai.app.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import com.healthyai.app.MainActivity
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.usecase.habit.DailyHabitProgress
import com.healthyai.app.domain.usecase.habit.HabitProgressLine
import com.healthyai.app.domain.usecase.habit.TodayHabitProgress
import com.healthyai.app.presentation.ui.format.formatHabitProgress
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

/** Số thói quen tối đa hiển thị trên widget — khung mặc định `150dp × 140dp`, không cuộn được. */
private const val MAX_HABIT_LINES = 3

class HabitWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface HabitWidgetEntryPoint {
        fun habitRepository(): HabitRepository
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            HabitWidgetEntryPoint::class.java
        )
        val repository = entryPoint.habitRepository()

        // Tiến độ **ngày**: thói quen đang hoạt động + log hôm nay (`getHabitLogsForPeriod(0)`),
        // xếp thói quen của buổi hiện tại lên đầu (mục 3 #16, #20 — TC-WG-03, TC-WG-12).
        val habits = repository.getAllActiveHabits().first()
        val todayLogs = repository.getHabitLogsForPeriod(0).first()
        val progress = TodayHabitProgress.build(habits, todayLogs)

        provideContent {
            GlanceTheme {
                WidgetContent(progress)
            }
        }
    }

    @Composable
    private fun WidgetContent(progress: DailyHabitProgress) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            if (progress.totalCount == 0) {
                EmptyContent()
            } else {
                ProgressContent(progress)
            }
        }
    }

    /** Chưa có thói quen nào đang hoạt động — giữ nguyên trạng thái rỗng như trước (TC-WG-02). */
    @Composable
    private fun EmptyContent() {
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                Text(
                    text = "Healthy AI",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = GlanceTheme.colors.onSurface
                    )
                )
                Text(
                    text = "Chưa có thói quen",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }
        }
    }

    /**
     * Tiến độ ngày: dòng tổng `x/y đạt mục tiêu` + thanh tiến độ **thật** (số thói quen đạt / tổng số)
     * + danh sách tối đa [MAX_HABIT_LINES] thói quen kèm số đã ghi / mục tiêu.
     */
    @Composable
    private fun ProgressContent(progress: DailyHabitProgress) {
        Text(
            text = "Tiến độ hôm nay",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = GlanceTheme.colors.onSurface
            ),
            maxLines = 1
        )
        Text(
            text = "${progress.completedCount}/${progress.totalCount} thói quen đạt mục tiêu",
            style = TextStyle(
                fontSize = 11.sp,
                color = GlanceTheme.colors.onSurfaceVariant
            ),
            maxLines = 1
        )
        Spacer(modifier = GlanceModifier.height(6.dp))

        LinearProgressIndicator(
            progress = progress.ratio,
            modifier = GlanceModifier.fillMaxWidth().height(8.dp),
            color = GlanceTheme.colors.primary,
            backgroundColor = GlanceTheme.colors.surfaceVariant
        )
        Spacer(modifier = GlanceModifier.height(8.dp))

        progress.lines.take(MAX_HABIT_LINES).forEach { line ->
            HabitRow(line)
        }

        val hiddenCount = progress.totalCount - MAX_HABIT_LINES
        if (hiddenCount > 0) {
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "+$hiddenCount thói quen khác",
                style = TextStyle(
                    fontSize = 11.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                ),
                maxLines = 1
            )
        }
    }

    /** Một dòng: `✓`/`○` + tên thói quen (bên trái) và `đã ghi / mục tiêu` (bên phải). */
    @Composable
    private fun HabitRow(line: HabitProgressLine) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = "${if (line.isCompleted) "✓" else "○"} ${line.habit.name}",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = if (line.isCompleted) {
                        GlanceTheme.colors.onSurfaceVariant
                    } else {
                        GlanceTheme.colors.onSurface
                    }
                ),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1
            )
            Text(
                text = formatHabitProgress(line.habit, line.loggedValue),
                style = TextStyle(
                    fontSize = 11.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                ),
                maxLines = 1
            )
        }
    }
}

class HabitWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitWidget()
}
