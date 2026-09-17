package com.healthyai.app.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import com.healthyai.app.MainActivity
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.repository.HabitRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

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
        
        val habits = repository.getAllActiveHabits().first()
        val firstHabit = habits.firstOrNull()

        provideContent {
            GlanceTheme {
                WidgetContent(firstHabit)
            }
        }
    }

    @Composable
    private fun WidgetContent(habit: Habit?) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            if (habit != null) {
                Text(
                    text = habit.name,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = GlanceTheme.colors.onSurface
                    )
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text = "Mục tiêu: ${habit.targetValue.toInt()} ${habit.unit}",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                
                // Progress Bar mô phỏng bằng Box
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(GlanceTheme.colors.surfaceVariant)
                ) {
                    // Giả định tiến độ 40% bằng cách dùng Row và weight
                    Row(modifier = GlanceModifier.fillMaxSize()) {
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight()
                                .background(GlanceTheme.colors.primary)
                        ) {}
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Spacer(modifier = GlanceModifier.defaultWeight())
                    }
                }
            } else {
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
}

class HabitWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitWidget()
}
