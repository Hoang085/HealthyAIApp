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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.items
import com.healthyai.app.domain.model.DailyConsistency
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.SleepStageBreakdown
import com.healthyai.app.domain.model.Suggestion
import com.healthyai.app.domain.model.SuggestionPriority
import com.healthyai.app.domain.model.VitalityScore
import com.healthyai.app.presentation.ui.theme.HealthyAITheme
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Màn Phân tích.
 *
 * [onOpenHabitLog] được gọi khi người dùng bấm nút hành động trong thẻ gợi ý AI (ví dụ
 * **"Ghi nhận uống nước"**) — màn hình chỉ phát danh mục của gợi ý, còn việc điều hướng sang tab
 * Thói quen do [com.healthyai.app.presentation.navigation.AppNavigation] lo (xem mục 3 #14).
 */
@Composable
fun InsightsScreen(
    onOpenHabitLog: (HabitCategory) -> Unit,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    InsightsScreenContent(uiState = uiState, onOpenHabitLog = onOpenHabitLog)
}

@Composable
fun InsightsScreenContent(
    uiState: InsightsUiState,
    onOpenHabitLog: (HabitCategory) -> Unit
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

            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF2E7D32))
                    }
                }
            } else {
                // Hiển thị danh sách gợi ý cá nhân hóa từ AI Rule Engine
                if (uiState.suggestions.isNotEmpty()) {
                    items(uiState.suggestions, key = { it.id }) { suggestion ->
                        AiTipCard(
                            suggestion = suggestion,
                            onActionClick = { onOpenHabitLog(suggestion.category) }
                        )
                    }
                } else {
                    item {
                        NoSuggestionsCard()
                    }
                }

                item {
                    VitalityScoreCard(uiState.vitality)
                }

                item {
                    WeeklyConsistencyCard(uiState.weeklyConsistency)
                }

                item {
                    SleepQualityTrendCard(uiState.sleepStages)
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun InsightsHeader() {
    Column {
        Text(
            "Phân tích AI",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp
            )
        )
        Text(
            "Xu hướng sức khỏe của bạn, được phân tích để phát triển.",
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
        SuggestionPriority.HIGH -> Color(0xFFE65100) // Orange 900
        SuggestionPriority.MEDIUM -> Color(0xFF2E7D32)
        SuggestionPriority.LOW -> Color.Gray
    }

    // Nền thẻ lấy từ theme (sáng/tối) rồi phủ một lớp màu ưu tiên mờ → giữ được tín hiệu mức độ
    // ưu tiên mà dark mode không còn thẻ nền sáng (#FFFFFF/#FFEBEE/#FFF3E0) làm mất chữ (mục 3 #18).
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerLow
    val priorityTint = priorityColor.copy(alpha = 0.12f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (suggestion.priority == SuggestionPriority.CRITICAL) 
            androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(priorityTint, surfaceColor)
                    )
                )
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(priorityColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (suggestion.category) {
                            HabitCategory.SLEEP -> Icons.Default.Bedtime
                            HabitCategory.WATER -> Icons.Default.WaterDrop
                            HabitCategory.EXERCISE -> Icons.AutoMirrored.Filled.DirectionsRun
                            HabitCategory.MINDFULNESS -> Icons.Default.Psychology
                            else -> Icons.Default.AutoAwesome
                        }
                        Icon(icon, null, tint = priorityColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (suggestion.priority == SuggestionPriority.CRITICAL) "CẢNH BÁO KHẨN CẤP" else "BLOOM AI GỢI Ý",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = priorityColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                
                Text(
                    suggestion.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                
                Text(
                    suggestion.message,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )
                
                if (suggestion.actionLabel != null) {
                    Button(
                        // Chuyển sang tab Thói quen và mở sẵn hộp thoại ghi nhận cho danh mục này
                        onClick = onActionClick,
                        colors = ButtonDefaults.buttonColors(containerColor = priorityColor),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(suggestion.actionLabel, fontWeight = FontWeight.Bold)
                    }
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
                "Mọi thứ đang rất tốt!",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "Bạn đang duy trì thói quen rất đều đặn. Hãy tiếp tục phát huy nhé!",
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
                    "Sự nhất quán hàng tuần",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2E7D32)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thói quen đạt được", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            if (!hasAnyHabit || days.isEmpty()) {
                Text(
                    "Chưa có thói quen nào để theo dõi. Hãy tạo thói quen đầu tiên ở tab Thói quen.",
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
                    "Tỉ lệ thói quen đạt mục tiêu theo từng ngày (7 ngày gần nhất)",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
            }
        }
    }
}

@Composable
private fun VitalityScoreCard(vitality: VitalityScore) {
    val score = vitality.score
    val progressColor = Color(0xFF1B5E20)
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
                "Chỉ số sinh lực",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    // Background circle
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Progress arc (sections)
                    val sections = 8
                    val gap = 5f
                    val sweep = (360f / sections) - gap
                    
                    // Score normalized to 8 sections
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
                    delta > 0 -> "+$delta so với tuần trước" to Color(0xFF2E7D32)
                    delta < 0 -> "$delta so với tuần trước" to Color(0xFFC62828)
                    else -> "Không đổi so với tuần trước" to Color.Gray
                }
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium.copy(color = color)
                )
            }
        }
    }
}

@Composable
private fun SleepQualityTrendCard(breakdown: SleepStageBreakdown) {
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
                    "Xu hướng chất lượng giấc ngủ",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Icon(Icons.Default.Bedtime, null, tint = Color(0xFF81C784))
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            if (!breakdown.hasData) {
                Text(
                    "Chưa có dữ liệu chi tiết pha giấc ngủ. Cần cấp quyền Giấc ngủ cho Health Connect " +
                        "và thiết bị phải ghi được pha Sâu/Nông/REM.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                val total = breakdown.totalMinutes.toFloat()
                SleepBar("Sâu", breakdown.deepMinutes / total, formatSleepMinutes(breakdown.deepMinutes), Color(0xFF1B5E20))
                SleepBar("Nông", breakdown.lightMinutes / total, formatSleepMinutes(breakdown.lightMinutes), Color(0xFF2E7D32))
                SleepBar("REM", breakdown.remMinutes / total, formatSleepMinutes(breakdown.remMinutes), Color(0xFF81C784))

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Tổng ${formatSleepMinutes(breakdown.totalMinutes)} trong 7 đêm gần nhất",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
            }
        }
    }
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
    HealthyAITheme {
        InsightsScreenContent(uiState = InsightsUiState(isLoading = false), onOpenHabitLog = {})
    }
}
