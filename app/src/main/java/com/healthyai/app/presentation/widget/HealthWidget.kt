package com.healthyai.app.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.healthyai.app.MainActivity
import com.healthyai.app.R
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.repository.SettingsRepository
import com.healthyai.app.domain.repository.StepCounterRepository
import com.healthyai.app.domain.repository.SleepTrackerRepository
import com.healthyai.app.domain.usecase.health.GetHealthDataUseCase
import com.healthyai.app.domain.usecase.health.GetLatestRecordedHeartRateUseCase
import com.healthyai.app.domain.usecase.health.GetStoredHealthDataUseCase
import com.healthyai.app.domain.usecase.health.TodayHealthSummary
import com.healthyai.app.presentation.ui.format.formatHeartRate
import com.healthyai.app.presentation.ui.format.formatSleepDuration
import com.healthyai.app.presentation.ui.format.formatStepsExact
import com.healthyai.app.presentation.ui.theme.WidgetContentDark
import com.healthyai.app.presentation.ui.theme.WidgetContentLight
import com.healthyai.app.presentation.ui.theme.WidgetDividerDark
import com.healthyai.app.presentation.ui.theme.WidgetDividerLight
import com.healthyai.app.presentation.ui.theme.WidgetHeartDark
import com.healthyai.app.presentation.ui.theme.WidgetHeartLight
import com.healthyai.app.presentation.ui.theme.WidgetMutedDark
import com.healthyai.app.presentation.ui.theme.WidgetMutedLight
import com.healthyai.app.presentation.ui.theme.WidgetSleepDark
import com.healthyai.app.presentation.ui.theme.WidgetSleepLight
import com.healthyai.app.presentation.ui.theme.WidgetStepsDark
import com.healthyai.app.presentation.ui.theme.WidgetStepsLight
import com.healthyai.app.presentation.ui.theme.WidgetSurfaceDark
import com.healthyai.app.presentation.ui.theme.WidgetSurfaceLight
import com.healthyai.app.util.runCatchingCancellable
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Màu dùng chung của widget; không dùng GlanceTheme vì mặc định là màu động (tím) lệch bản sắc app. */
private val Content = ColorProvider(WidgetContentLight, WidgetContentDark)

/** Chữ phụ: nhãn "Hôm nay". */
private val Muted = ColorProvider(WidgetMutedLight, WidgetMutedDark)

/** Màu nhấn của từng chỉ số (icon + nhãn): bước chân — xanh lá, giấc ngủ — xanh ngọc, nhịp tim — đỏ. */
private val StepsAccent = ColorProvider(WidgetStepsLight, WidgetStepsDark)
private val SleepAccent = ColorProvider(WidgetSleepLight, WidgetSleepDark)
private val HeartAccent = ColorProvider(WidgetHeartLight, WidgetHeartDark)

/** Widget "Sức khỏe hôm nay": ba chỉ số bước chân, giấc ngủ, nhịp tim; bấm để mở app. */
class HealthWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface HealthWidgetEntryPoint {
        fun stepCounterRepository(): StepCounterRepository
        fun sleepTrackerRepository(): SleepTrackerRepository
        fun settingsRepository(): SettingsRepository
        fun getHealthDataUseCase(): GetHealthDataUseCase
        fun getStoredHealthDataUseCase(): GetStoredHealthDataUseCase
        fun getLatestRecordedHeartRateUseCase(): GetLatestRecordedHeartRateUseCase
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            HealthWidgetEntryPoint::class.java
        )

        // Đọc dữ liệu một lần lúc vẽ: Glance không quan sát Flow nên số liệu là bản chụp tại thời điểm vẽ.
        val summary = readSummary(entryPoint)

        provideContent {
            WidgetContent(summary)
        }
    }

    /**
     * Ghép ba chỉ số bằng đúng luật của thẻ "Tóm tắt sức khỏe" ([TodayHealthSummary.build]): đọc
     * **cùng nguồn** mà thẻ đang chọn — switch "Dùng dữ liệu Health Connect" BẬT thì đọc thẳng Health
     * Connect, TẮT (mặc định) thì đọc bản chụp đã lưu trong app — rồi mới rơi về số app tự đo.
     */
    private suspend fun readSummary(entryPoint: HealthWidgetEntryPoint): TodayHealthSummary {
        val stepCounter = entryPoint.stepCounterRepository()
        val stepCounterStatus = stepCounter.getStatus()
        val sleepTracker = entryPoint.sleepTrackerRepository()
        val latestRecordedHeartRate = entryPoint.getLatestRecordedHeartRateUseCase()

        val sensorSteps = if (stepCounterStatus.isReady) {
            stepCounter.readTodaySteps(StepCounterRepository.NO_WAIT_MS)
        } else {
            0L
        }
        val sensorSleepHours = runCatchingCancellable {
            sleepTracker.readLastNightSleepHours()
        }.getOrDefault(0.0)

        return TodayHealthSummary.build(
            primary = readPrimaryHealthData(entryPoint),
            sensorSteps = sensorSteps,
            sensorSleepHours = sensorSleepHours,
            recordedHeartRate = latestRecordedHeartRate()
        )
    }

    /**
     * Nguồn số liệu đang chọn trên Trang chủ. Đọc lỗi coi như rỗng để widget vẫn hiện được phần số
     * app tự đo (bước chân từ cảm biến, giờ ngủ ước lượng, nhịp tim đã ghi) thay vì trắng cả widget.
     */
    private suspend fun readPrimaryHealthData(entryPoint: HealthWidgetEntryPoint): HealthData {
        val useHealthConnect = runCatchingCancellable {
            entryPoint.settingsRepository().getHealthConnectData().enabled
        }.getOrDefault(false)

        return if (useHealthConnect) {
            runCatchingCancellable { entryPoint.getHealthDataUseCase()() }.getOrNull()
                ?.getOrNull() ?: HealthData()
        } else {
            runCatchingCancellable { entryPoint.getStoredHealthDataUseCase()() }.getOrDefault(HealthData())
        }
    }

    @Composable
    private fun WidgetContent(summary: TodayHealthSummary) {
        val context = LocalContext.current

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(day = WidgetSurfaceLight, night = WidgetSurfaceDark)
                // Bo góc do widget tự vẽ để Android 11 trở xuống vẫn là thẻ bo góc.
                .cornerRadius(24.dp)
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .clickable(actionStartActivity<MainActivity>()),
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            HeaderRow(todayLabel = context.getString(R.string.widget_today))

            Spacer(modifier = GlanceModifier.height(8.dp))
            // Đường kẻ mảnh: dùng `Spacer` có nền thay vì `Box` rỗng (Glance yêu cầu `Box` phải có nội dung).
            Spacer(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(day = WidgetDividerLight, night = WidgetDividerDark)
            )
            Spacer(modifier = GlanceModifier.height(12.dp))

            // Ba cột chia đều bề ngang; minWidth (250dp) trong health_widget_info.xml chặn khung hẹp.
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                StatCell(
                    modifier = GlanceModifier.defaultWeight(),
                    iconRes = R.drawable.ic_widget_steps,
                    value = formatStepsExact(summary.steps),
                    label = context.getString(R.string.widget_steps_label),
                    accent = StepsAccent
                )
                StatCell(
                    modifier = GlanceModifier.defaultWeight(),
                    iconRes = R.drawable.ic_widget_sleep,
                    value = if (summary.sleepHours > 0.0) {
                        formatSleepDuration(summary.sleepHours)
                    } else {
                        // Chưa có số nào → "--" thay vì "0h 0m" (số 0 bị đọc thành "ngủ 0 giờ").
                        context.getString(R.string.widget_no_value)
                    },
                    label = context.getString(R.string.widget_sleep_label),
                    accent = SleepAccent
                )
                StatCell(
                    modifier = GlanceModifier.defaultWeight(),
                    iconRes = R.drawable.ic_widget_heart,
                    value = formatHeartRate(summary.heartRateBpm),
                    label = context.getString(R.string.widget_heart_label),
                    accent = HeartAccent
                )
            }
        }
    }

    /** Dòng đầu: tên app (đậm, màu nhấn) + nhãn "Hôm nay" nói rõ số liệu là của ngày nào. */
    @Composable
    private fun HeaderRow(todayLabel: String) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = LocalContext.current.getString(R.string.widget_brand),
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = StepsAccent
                ),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1
            )
            Text(
                text = todayLabel,
                style = TextStyle(fontSize = 11.sp, color = Muted),
                maxLines = 1
            )
        }
    }

    /** Một cột chỉ số: icon → số đo (đậm, cỡ lớn) → nhãn (nhỏ, đúng màu nhấn của chỉ số). */
    @Composable
    private fun StatCell(
        modifier: GlanceModifier,
        iconRes: Int,
        value: String,
        label: String,
        accent: ColorProvider
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Image(
                provider = ImageProvider(iconRes),
                // Icon chỉ để nhận diện; không đặt contentDescription vì nhãn chữ bên dưới đã đủ nghĩa.
                contentDescription = null,
                modifier = GlanceModifier.size(22.dp)
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = value,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    textAlign = TextAlign.Center,
                    color = Content
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = label,
                style = TextStyle(fontSize = 10.sp, textAlign = TextAlign.Center, color = accent),
                maxLines = 1
            )
        }
    }
}

class HealthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HealthWidget()
}
