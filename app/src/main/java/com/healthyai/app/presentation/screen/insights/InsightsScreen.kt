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
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.Suggestion
import com.healthyai.app.domain.model.SuggestionPriority
import com.healthyai.app.presentation.ui.theme.HealthyAITheme

@Composable
fun InsightsScreen(viewModel: InsightsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    InsightsScreenContent(uiState = uiState)
}

@Composable
fun InsightsScreenContent(uiState: InsightsUiState) {
    Scaffold(
        containerColor = Color(0xFFF8F9FA)
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
                        AiTipCard(suggestion)
                    }
                } else {
                    item {
                        NoSuggestionsCard()
                    }
                }

                item {
                    VitalityScoreCard(uiState.vitalityScore)
                }

                item {
                    WeeklyConsistencyCard()
                }

                item {
                    SleepQualityTrendCard()
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
private fun AiTipCard(suggestion: Suggestion) {
    val priorityColor = when (suggestion.priority) {
        SuggestionPriority.CRITICAL -> Color.Red
        SuggestionPriority.HIGH -> Color(0xFFE65100) // Orange 900
        SuggestionPriority.MEDIUM -> Color(0xFF2E7D32)
        SuggestionPriority.LOW -> Color.Gray
    }

    val backgroundColor = when (suggestion.priority) {
        SuggestionPriority.CRITICAL -> Color(0xFFFFEBEE)
        SuggestionPriority.HIGH -> Color(0xFFFFF3E0)
        else -> Color.White
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (suggestion.priority == SuggestionPriority.CRITICAL) 
            androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(backgroundColor, Color(0xFFE8F5E9).copy(alpha = 0.3f))
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
                        onClick = { /* TODO: Điều hướng đến thói quen tương ứng */ },
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
private fun WeeklyConsistencyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Placeholder for Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw horizontal line
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEach { day ->
                    Text(day, style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                }
            }
        }
    }
}

@Composable
private fun VitalityScoreCard(score: Int) {
    val progressColor = Color(0xFF1B5E20)
    val trackColor = Color(0xFFEEEEEE)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "+5 so với tuần trước",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
            )
        }
    }
}

@Composable
private fun SleepQualityTrendCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
            
            SleepBar("Sâu", 0.3f, "2h 15m", Color(0xFF1B5E20))
            SleepBar("Nông", 0.6f, "4h 30m", Color(0xFF2E7D32))
            SleepBar("REM", 0.2f, "1h 10m", Color(0xFF81C784))
        }
    }
}

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
        InsightsScreenContent(uiState = InsightsUiState(isLoading = false))
    }
}
