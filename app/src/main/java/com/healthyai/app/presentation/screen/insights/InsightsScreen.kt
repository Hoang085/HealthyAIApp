package com.healthyai.app.presentation.screen.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.healthyai.app.R
import com.healthyai.app.domain.model.DailyConsistency
import com.healthyai.app.domain.model.DailyHealthSummary
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.SleepQuality
import com.healthyai.app.domain.model.SleepStageBreakdown
import com.healthyai.app.domain.model.SleepTrend
import com.healthyai.app.domain.model.Suggestion
import com.healthyai.app.domain.model.SuggestionPriority
import com.healthyai.app.domain.model.VitalityScore
import com.healthyai.app.domain.usecase.health.SleepTrendMath
import com.healthyai.app.presentation.ui.component.ErrorStateCard
import com.healthyai.app.presentation.ui.format.formatStepCount
import com.healthyai.app.presentation.ui.theme.HealthyAITheme
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.coroutines.delay

/** Màn Phân tích; điều hướng khi bấm nút gợi ý AI do AppNavigation lo, còn lối vào Chính sách bảo mật ở thẻ cuối màn. */
@Composable
fun InsightsScreen(
    onOpenHabitLog: (HabitCategory) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Chi tiết pha giấc ngủ đọc trực tiếp từ Health Connect (không phải Flow) nên đọc lại mỗi lần quay lại màn này.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    InsightsScreenContent(
        uiState = uiState,
        onOpenHabitLog = onOpenHabitLog,
        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
        onDismissError = viewModel::dismissError
    )
}

@Composable
fun InsightsScreenContent(
    uiState: InsightsUiState,
    onOpenHabitLog: (HabitCategory) -> Unit,
    /** Mở màn "Chính sách bảo mật": nói rõ ảnh món ăn được gửi tới Google Gemini. */
    onOpenPrivacyPolicy: () -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                InsightsHeader()
            }

            // Lỗi đọc dữ liệu phải hiện ra chứ không được im lặng.
            uiState.visibleError?.let { message ->
                item {
                    ErrorStateCard(message = message, onDismiss = onDismissError)
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF2E7D32))
                    }
                }
            } else {
                // Feed gợi ý BLOOM AI là một banner trượt tự động (chiều cao cố định) thay cho danh sách thẻ xếp dọc.
                item {
                    if (uiState.suggestions.isNotEmpty()) {
                        AiTipBanner(
                            suggestions = uiState.suggestions,
                            onActionClick = onOpenHabitLog
                        )
                    } else {
                        NoSuggestionsCard()
                    }
                }

                item {
                    VitalityScoreCard(uiState.vitality)
                }

                item {
                    HealthHistoryCard(uiState.healthHistory)
                }

                item {
                    WeeklyConsistencyCard(uiState.weeklyConsistency)
                }

                item {
                    SleepQualityTrendCard(
                        trend = uiState.sleepTrend,
                        breakdown = uiState.sleepStages
                    )
                }
            }

            // Lối vào Chính sách bảo mật của phần AI; nằm ngoài nhánh isLoading nên luôn hiện.
            item {
                AiPrivacyCard(onOpenPrivacyPolicy = onOpenPrivacyPolicy)
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

/** Thẻ "AI & quyền riêng tư" ở cuối tab Phân tích AI — lối vào màn Chính sách bảo mật. */
@Composable
private fun AiPrivacyCard(onOpenPrivacyPolicy: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.insights_privacy_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Text(
                stringResource(R.string.insights_privacy_body),
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
            )
            TextButton(
                onClick = onOpenPrivacyPolicy,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.insights_privacy_open))
            }
        }
    }
}

@Composable
private fun InsightsHeader() {
    Column {
        Text(
            stringResource(R.string.insights_header_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp
            )
        )
        Text(
            stringResource(R.string.insights_header_subtitle),
            style = MaterialTheme.typography.bodyLarge.copy(color = Color.Gray)
        )
    }
}

@Composable
private fun AiTipCard(
    suggestion: Suggestion,
    onActionClick: () -> Unit
) {
    val priorityColor = when (suggestion.priority) {
        SuggestionPriority.CRITICAL -> Color.Red
        SuggestionPriority.HIGH -> Color(0xFFE65100) // Cam 900
        SuggestionPriority.MEDIUM -> Color(0xFF2E7D32)
        SuggestionPriority.LOW -> Color.Gray
    }

    // Nền thẻ lấy từ theme rồi phủ màu ưu tiên mờ, để dark mode không làm mất chữ như thẻ nền sáng cố định.
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLow
    val priorityTint = priorityColor.copy(alpha = 0.12f)

    // fillMaxSize (không phải fillMaxWidth): thẻ là một trang của banner cao cố định nên phải phủ hết chiều cao, tránh banner nhảy khi trượt.
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (suggestion.priority == SuggestionPriority.CRITICAL) 
            androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(priorityTint, surfaceColor)
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(priorityColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (suggestion.category) {
                            HabitCategory.SLEEP -> Icons.Default.Bedtime
                            HabitCategory.WATER -> Icons.Default.WaterDrop
                            HabitCategory.EXERCISE -> Icons.AutoMirrored.Filled.DirectionsRun
                            HabitCategory.MINDFULNESS -> Icons.Default.Psychology
                            HabitCategory.NUTRITION -> Icons.Default.Restaurant
                            else -> Icons.Default.AutoAwesome
                        }
                        Icon(icon, null, tint = priorityColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (suggestion.priority == SuggestionPriority.CRITICAL) stringResource(R.string.insights_ai_tip_critical) else stringResource(R.string.insights_ai_tip_label),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = priorityColor,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Text(
                    suggestion.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    suggestion.message,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (suggestion.actionLabel != null) {
                    // Đẩy nút xuống đáy trang để mọi thẻ trong banner cân đối dù mẹo có nút hay không.
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        // Chuyển sang tab Thói quen và mở sẵn hộp thoại ghi nhận cho danh mục này
                        onClick = onActionClick,
                        colors = ButtonDefaults.buttonColors(containerColor = priorityColor),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            suggestion.actionLabel,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/** Banner gợi ý BLOOM AI: hiển thị một gợi ý mỗi lúc và tự trượt sang gợi ý kế tiếp, trong khối cao cố định. */
@Composable
private fun AiTipBanner(
    suggestions: List<Suggestion>,
    onActionClick: (HabitCategory) -> Unit
) {
    val pageCount = suggestions.size
    // Pager "vô hạn" (số trang rất lớn + lấy phần dư) để banner luôn trượt tới trước, không quay ngược về đầu.
    val pagerState = rememberPagerState(pageCount = { Int.MAX_VALUE })

    // Tự cuộn: chỉ chạy khi có từ 2 gợi ý trở lên và không giành điều khiển khi người dùng đang vuốt tay.
    LaunchedEffect(pageCount) {
        if (pageCount <= 1) return@LaunchedEffect
        while (true) {
            delay(AI_BANNER_AUTO_SCROLL_MS)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(AI_BANNER_HEIGHT)
        ) { page ->
            val suggestion = suggestions[page % pageCount]
            AiTipCard(
                suggestion = suggestion,
                onActionClick = { onActionClick(suggestion.category) }
            )
        }

        if (pageCount > 1) {
            Spacer(modifier = Modifier.height(12.dp))
            // Chấm chỉ báo: người dùng biết còn gợi ý khác để chờ/vuốt tới.
            val activeIndex = pagerState.currentPage % pageCount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pageCount) { index ->
                    val selected = index == activeIndex
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .width(if (selected) 20.dp else 8.dp)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) Color(0xFF2E7D32)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun NoSuggestionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Celebration, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(48.dp))
            Text(
                stringResource(R.string.insights_all_good_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                stringResource(R.string.insights_all_good_message),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
            )
        }
    }
}

@Composable
private fun WeeklyConsistencyCard(days: List<DailyConsistency>) {
    val today = LocalDate.now()
    val hasAnyHabit = days.any { it.totalHabits > 0 }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.insights_weekly_consistency_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2E7D32)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.insights_weekly_consistency_legend), style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            if (!hasAnyHabit || days.isEmpty()) {
                Text(
                    stringResource(R.string.insights_weekly_consistency_empty),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    days.forEach { day ->
                        val isToday = day.date == today
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${(day.ratio * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isToday) Color(0xFF1B5E20) else Color.Gray,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Cột nền xám (mốc 100%) + phần tô xanh theo tỉ lệ đạt được
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(BAR_TRACK_HEIGHT)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(Color(0xFFEEEEEE)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(BAR_TRACK_HEIGHT * day.ratio)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(
                                            if (isToday) Color(0xFF1B5E20) else Color(0xFF81C784)
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                dayLabel(day.date),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isToday) Color(0xFF1B5E20) else Color.Gray,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.insights_weekly_consistency_note),
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
            }
        }
    }
}

@Composable
private fun VitalityScoreCard(vitality: VitalityScore) {
    val trackColor = Color(0xFFEEEEEE)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.insights_vitality_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!vitality.hasData) {
                // Chưa có nguồn nào để chấm điểm → không hiện một con số ảo (trước đây luôn là 50).
                Icon(Icons.Default.Insights, null, tint = Color(0xFF9E9E9E), modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.insights_vitality_no_data),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(R.string.insights_vitality_no_data_hint),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                return@Column
            }

            val score = vitality.score
            // Màu theo dải điểm để con số "biết nói": ≥70 tốt, 40–69 cần chú ý, <40 thấp.
            val progressColor = when {
                score >= 70 -> Color(0xFF2E7D32)
                score >= 40 -> Color(0xFFE65100)
                else -> Color(0xFFC62828)
            }

            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    // Vòng nền
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Cung tiến độ theo đoạn
                    val sections = 8
                    val gap = 5f
                    val sweep = (360f / sections) - gap

                    // Chuẩn hoá điểm thành 8 đoạn
                    val sectionsToFill = (score / 100f * sections).toInt().coerceIn(0, sections)

                    for (i in 0 until sectionsToFill) {
                        drawArc(
                            color = progressColor,
                            startAngle = i * (sweep + gap) - 90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$score",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "/100",
                        style = MaterialTheme.typography.labelLarge.copy(color = Color.Gray)
                    )
                }
            }

            // Chỉ so sánh khi thật sự có dữ liệu 7 ngày trước đó (delta != null)
            vitality.delta?.let { delta ->
                Spacer(modifier = Modifier.height(16.dp))
                val (label, color) = when {
                    delta > 0 -> stringResource(R.string.insights_vitality_delta_up, delta) to Color(0xFF2E7D32)
                    delta < 0 -> stringResource(R.string.insights_vitality_delta_down, delta) to Color(0xFFC62828)
                    else -> stringResource(R.string.insights_vitality_delta_same) to Color.Gray
                }
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium.copy(color = color)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                stringResource(R.string.insights_vitality_note),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
            )
        }
    }
}

/** Thẻ "Lịch sử sức khỏe 7 ngày": biểu đồ bước chân + bảng mỗi ngày một dòng; ngày chưa có dữ liệu hiện "Chưa có dữ liệu" thay vì số 0. */
@Composable
private fun HealthHistoryCard(days: List<DailyHealthSummary>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.insights_health_history_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Icon(Icons.Default.MonitorHeart, null, tint = Color(0xFF2E7D32))
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (days.none { it.hasData }) {
                Text(
                    stringResource(R.string.insights_health_history_empty),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                StepsBarChart(days)
                Spacer(modifier = Modifier.height(24.dp))
                HealthHistoryTable(days)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(R.string.insights_health_history_note),
                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
            )
        }
    }
}

/** Biểu đồ cột số bước 7 ngày; mốc 100% là ngày đi nhiều nhất tuần để cột luôn đọc được. */
@Composable
private fun StepsBarChart(days: List<DailyHealthSummary>) {
    val maxSteps = days.maxOf { it.steps }.coerceAtLeast(1L)
    val today = LocalDate.now()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            val isToday = day.date == today
            val highlight = if (isToday) Color(0xFF1B5E20) else Color.Gray

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (day.steps > 0L) formatStepCount(day.steps) else NO_DATA_CELL,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = highlight,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(BAR_TRACK_HEIGHT)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xFFEEEEEE)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(BAR_TRACK_HEIGHT * (day.steps.toFloat() / maxSteps))
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isToday) Color(0xFF1B5E20) else Color(0xFF81C784))
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    dayLabel(day.date),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = highlight,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }
        }
    }
}

/** Bảng 7 ngày: mỗi ngày một dòng gồm bước chân / giờ ngủ / nhịp tim đã ghi. */
@Composable
private fun HealthHistoryTable(days: List<DailyHealthSummary>) {
    val today = LocalDate.now()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.insights_table_date),
                modifier = Modifier.weight(1.3f),
                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontWeight = FontWeight.Bold)
            )
            HealthHistoryHeaderCell(stringResource(R.string.insights_table_steps), Modifier.weight(1f))
            HealthHistoryHeaderCell(stringResource(R.string.insights_table_sleep), Modifier.weight(1f))
            HealthHistoryHeaderCell(stringResource(R.string.insights_table_heart), Modifier.weight(1f))
        }

        days.forEach { day ->
            HealthHistoryDivider()
            HealthHistoryRow(day = day, isToday = day.date == today)
        }
    }
}

@Composable
private fun HealthHistoryHeaderCell(text: String, modifier: Modifier) {
    Text(
        text,
        modifier = modifier,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontWeight = FontWeight.Bold)
    )
}

@Composable
private fun HealthHistoryRow(day: DailyHealthSummary, isToday: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            dayLabel(day.date) + " " + shortDate(day.date),
            modifier = Modifier.weight(1.3f),
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (isToday) Color(0xFF1B5E20) else Color.Gray,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
        )

        if (!day.hasData) {
            Text(
                stringResource(R.string.insights_table_no_data),
                modifier = Modifier.weight(3f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
            )
        } else {
            HealthHistoryValueCell(formatStepsCell(day.steps), Modifier.weight(1f))
            HealthHistoryValueCell(formatSleepCell(day.sleepHours), Modifier.weight(1f))
            HealthHistoryValueCell(formatHeartRateCell(day.heartRate), Modifier.weight(1f))
        }
    }
}

@Composable
private fun HealthHistoryValueCell(text: String, modifier: Modifier) {
    Text(
        text,
        modifier = modifier,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
    )
}

/** Vạch ngăn giữa hai dòng — tự vẽ để không phụ thuộc phiên bản Material 3. */
@Composable
private fun HealthHistoryDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFEEEEEE))
    )
}

/** Ô chưa ghi được chỉ số nào trong ngày (khác hẳn số 0). */
private const val NO_DATA_CELL = "—"

private fun formatStepsCell(steps: Long): String =
    if (steps > 0L) formatStepCount(steps) else NO_DATA_CELL

/** "7.5" → `"7h 30m"`; ngày không ghi được giờ ngủ → `—`. */
private fun formatSleepCell(hours: Double): String =
    if (hours > 0.0) formatSleepMinutes((hours * 60).roundToLong()) else NO_DATA_CELL

private fun formatHeartRateCell(bpm: Double): String =
    if (bpm > 0.0) bpm.toInt().toString() else NO_DATA_CELL

/** "22/09" — dựng tay để không phụ thuộc locale như `DateTimeFormatter`. */
private fun shortDate(date: LocalDate): String =
    "${date.dayOfMonth.toString().padStart(2, '0')}/${date.monthValue.toString().padStart(2, '0')}"

@Composable
private fun SleepQualityTrendCard(trend: SleepTrend, breakdown: SleepStageBreakdown) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.insights_sleep_trend_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Icon(Icons.Default.Bedtime, null, tint = Color(0xFF81C784))
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!trend.hasData && !breakdown.hasData) {
                // Chưa có giờ ngủ và cũng chưa có dữ liệu pha → hướng dẫn cụ thể cách để có số liệu.
                Text(
                    stringResource(R.string.insights_sleep_no_data),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                if (trend.hasData) {
                    SleepNightsBarChart(trend)

                    Spacer(modifier = Modifier.height(16.dp))

                    val quality = SleepTrendMath.qualityOf(trend.averageHours)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.insights_sleep_average, formatSleepHoursDecimal(trend.averageHours)),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(sleepQualityColor(quality).copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                quality.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = sleepQualityColor(quality),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        stringResource(R.string.insights_sleep_nights_with_data, trend.nightsWithData.size, trend.nights.size),
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                    )
                }

                // Chỉ vẽ thanh chi tiết khi máy có dữ liệu pha; máy chỉ báo tổng thời lượng thì hiện dòng tổng.
                if (breakdown.hasStages) {
                    if (trend.hasData) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            stringResource(R.string.insights_sleep_stages_title),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    val total = breakdown.totalMinutes.toFloat()
                    SleepBar(stringResource(R.string.insights_sleep_stage_deep), breakdown.deepMinutes / total, formatSleepMinutes(breakdown.deepMinutes), Color(0xFF1B5E20))
                    SleepBar(stringResource(R.string.insights_sleep_stage_light), breakdown.lightMinutes / total, formatSleepMinutes(breakdown.lightMinutes), Color(0xFF2E7D32))
                    SleepBar(stringResource(R.string.insights_sleep_stage_rem), breakdown.remMinutes / total, formatSleepMinutes(breakdown.remMinutes), Color(0xFF81C784))

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        stringResource(R.string.insights_sleep_stages_total, formatSleepMinutes(breakdown.totalMinutes)),
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                    )
                } else if (breakdown.hasData) {
                    // Thiết bị chỉ báo STAGE_TYPE_SLEEPING (không chia Sâu/Nông/REM) → vẫn hiện số ngủ thật thay vì thẻ rỗng.
                    if (trend.hasData) {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                    Text(
                        stringResource(
                            R.string.insights_sleep_unstaged_total,
                            formatSleepMinutes(breakdown.unstagedMinutes)
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                    )
                }
            }
        }
    }
}

/** Biểu đồ cột giờ ngủ 7 đêm gần nhất; mốc 100% là đêm ngủ nhiều nhất tuần để cột luôn đọc được. */
@Composable
private fun SleepNightsBarChart(trend: SleepTrend) {
    val maxHours = trend.nights.maxOf { it.hours }.coerceAtLeast(1.0).toFloat()
    val today = LocalDate.now()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        trend.nights.forEach { night ->
            val isToday = night.date == today
            val highlight = if (isToday) Color(0xFF1B5E20) else Color.Gray

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (night.hasData) formatSleepHoursDecimal(night.hours) else NO_DATA_CELL,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = highlight,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(BAR_TRACK_HEIGHT)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xFFEEEEEE)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(BAR_TRACK_HEIGHT * (night.hours.toFloat() / maxHours))
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isToday) Color(0xFF1B5E20) else Color(0xFF81C784))
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    dayLabel(night.date),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = highlight,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }
        }
    }
}

/** Màu theo xếp loại chất lượng giấc ngủ: tốt xanh, chưa đủ cam, thiếu ngủ đỏ, hơi dài xám. */
private fun sleepQualityColor(quality: SleepQuality): Color = when (quality) {
    SleepQuality.GOOD -> Color(0xFF2E7D32)
    SleepQuality.FAIR -> Color(0xFFE65100)
    SleepQuality.SHORT -> Color(0xFFC62828)
    SleepQuality.LONG -> Color.Gray
}

/** "7.5" → `"7h30"` (gọn cho nhãn trên cột); 0 giờ → `"0h"`. */
private fun formatSleepHoursDecimal(hours: Double): String {
    val totalMinutes = (hours * 60).roundToLong()
    return "${totalMinutes / 60}h${(totalMinutes % 60).toString().padStart(2, '0')}"
}

/** Nhãn ngày viết tắt theo tiếng Việt: T2..T7, CN. */
private fun dayLabel(date: LocalDate): String = when (date.dayOfWeek) {
    DayOfWeek.MONDAY -> "T2"
    DayOfWeek.TUESDAY -> "T3"
    DayOfWeek.WEDNESDAY -> "T4"
    DayOfWeek.THURSDAY -> "T5"
    DayOfWeek.FRIDAY -> "T6"
    DayOfWeek.SATURDAY -> "T7"
    DayOfWeek.SUNDAY -> "CN"
}

/** "135" → "2h 15m". */
private fun formatSleepMinutes(minutes: Long): String = "${minutes / 60}h ${minutes % 60}m"

private val BAR_TRACK_HEIGHT = 92.dp

/** Chiều cao cố định của banner gợi ý AI, để banner không "phình" theo độ dài mẹo. */
private val AI_BANNER_HEIGHT = 240.dp

/** Nhịp tự cuộn của banner gợi ý AI — 5 giây đủ để đọc trọn một gợi ý. */
private const val AI_BANNER_AUTO_SCROLL_MS = 5_000L

@Composable
private fun SleepBar(label: String, progress: Float, duration: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.width(60.dp),
            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = color,
            trackColor = Color(0xFFEEEEEE),
            strokeCap = StrokeCap.Round
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            duration,
            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InsightsScreenPreview() {
    // 7 ngày mẫu: có ngày đủ 3 chỉ số, có ngày thiếu nhịp tim, có ngày chưa ghi được gì
    val today = LocalDate.now()
    val history = (6 downTo 0).map { daysAgo ->
        val date = today.minusDays(daysAgo.toLong())
        when (daysAgo) {
            // Ngày chưa ghi được gì → UI hiện "Chưa có dữ liệu"
            1 -> DailyHealthSummary(date = date)
            // Ngày chỉ có bước chân (máy không có nguồn đo giấc ngủ/nhịp tim)
            3 -> DailyHealthSummary(date = date, steps = 4_120L)
            else -> DailyHealthSummary(
                date = date,
                steps = 3_000L + daysAgo * 900L,
                sleepHours = 6.5 + daysAgo * 0.25,
                heartRate = 68.0 + daysAgo
            )
        }
    }
    HealthyAITheme {
        InsightsScreenContent(
            uiState = InsightsUiState(
                suggestions = listOf(
                    Suggestion(
                        id = 1L,
                        title = "💧 Uống một ly nước ngay bây giờ",
                        message = "Cơ thể bạn đang thiếu nước. Một ly 250ml ngay lúc này là bước khởi " +
                            "đầu dễ nhất.",
                        priority = SuggestionPriority.HIGH,
                        category = HabitCategory.WATER,
                        actionLabel = "Ghi nhận uống nước"
                    ),
                    Suggestion(
                        id = 2L,
                        title = "🚶 Đi bộ 10 phút sau bữa tối",
                        message = "Một vòng đi bộ ngắn sau bữa tối là cách nhẹ nhàng để tăng vận động " +
                            "mỗi ngày.",
                        priority = SuggestionPriority.MEDIUM,
                        category = HabitCategory.EXERCISE
                    )
                ),
                vitality = VitalityScore(score = 64, delta = -6, hasData = true),
                healthHistory = history,
                sleepTrend = SleepTrendMath.fromHealthHistory(history),
                isLoading = false
            ),
            onOpenHabitLog = {}
        )
    }
}
