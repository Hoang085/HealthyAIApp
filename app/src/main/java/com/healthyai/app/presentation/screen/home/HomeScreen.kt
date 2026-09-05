package com.healthyai.app.presentation.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.presentation.ui.theme.HealthyAITheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            viewModel.dismissError()
        }
    }

    HomeScreenContent(uiState = uiState)
}

@Composable
fun HomeScreenContent(uiState: HomeUiState) {
    Scaffold(
        containerColor = Color(0xFFF8F9FA) // Restore Light gray background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { GreetingHeader() }
            item { HealthSummaryCard(uiState = uiState) }
            item {
                Text(
                    "Thói quen hôm nay",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                )
            }

            if (uiState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                    }
                }
            } else if (uiState.habits.isEmpty()) {
                item { EmptyHabitsCard() }
            } else {
                items(uiState.habits) { habit ->
                    // Mocking completion for the sake of UI design (as in the image)
                    val isCompleted = habit.name.contains("Thiền", ignoreCase = true)
                    HabitItem(habit, isCompleted = isCompleted)
                }
            }
        }
    }
}

@Composable
private fun GreetingHeader() {
    val greeting = when (LocalTime.now().hour) {
        in 5..11  -> "Chào buổi sáng,"
        in 12..17 -> "Chào buổi chiều,"
        in 18..21 -> "Chào buổi tối,"
        else      -> "Chào buổi đêm,"
    }
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd 'tháng' MM", Locale("vi")))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                date,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
            )
            Text(
                greeting,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
            )
            Text(
                "Alex",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = Color(0xFF2E7D32)
                )
            )
        }
        // Avatar Placeholder
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(40.dp).align(Alignment.Center),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun HealthSummaryCard(uiState: HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tóm tắt sức khỏe",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HealthCircleStat(
                    icon = Icons.Default.DirectionsWalk,
                    label = "Bước chân",
                    value = "${(uiState.healthData.steps / 1000.0).let { "%.1f".format(it) }}k",
                    color = Color(0xFF4CAF50)
                )
                HealthCircleStat(
                    icon = Icons.Default.Bedtime,
                    label = "Ngủ",
                    value = formatSleep(uiState.healthData.sleepHours),
                    color = Color(0xFF2E4032)
                )
                HealthCircleStat(
                    icon = Icons.Default.Favorite,
                    label = "Tim",
                    value = "${uiState.healthData.heartRate.toInt()} bpm",
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun HealthCircleStat(icon: ImageVector, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
        Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun HabitItem(habit: Habit, isCompleted: Boolean = false) {
    val (icon, color) = habitVisuals[habit.category] ?: (Icons.Default.TaskAlt to Color.Gray)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    habit.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "Mục tiêu: ${habit.targetValue.toInt()} ${habit.unit}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )
            }
            // Checkbox Circle
            if (isCompleted) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFF2E7D32), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .border(2.dp, Color.LightGray, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun EmptyHabitsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.AddCircle, null, modifier = Modifier.size(48.dp), tint = Color(0xFF2E7D32))
            Text("Thêm thói quen đầu tiên", fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatSleep(hours: Double): String {
    val h = hours.toInt()
    val m = ((hours - h) * 60).toInt()
    return "${h}h ${m}m"
}

private val habitVisuals = mapOf(
    HabitCategory.WATER to (Icons.Default.WaterDrop to Color(0xFF2196F3)),
    HabitCategory.MINDFULNESS to (Icons.Default.SelfImprovement to Color(0xFFFF9800)),
    HabitCategory.EXERCISE to (Icons.AutoMirrored.Filled.DirectionsRun to Color(0xFFE91E63)),
    HabitCategory.SLEEP to (Icons.Default.Bedtime to Color(0xFF673AB7)),
    HabitCategory.NUTRITION to (Icons.Default.Restaurant to Color(0xFF4CAF50))
)

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HealthyAITheme {
        HomeScreenContent(
            uiState = HomeUiState(
                habits = listOf(
                    Habit(id = 1, name = "Uống nước", category = HabitCategory.WATER, targetValue = 2000f, unit = "ml"),
                    Habit(id = 2, name = "Thiền buổi sáng", category = HabitCategory.MINDFULNESS, targetValue = 15f, unit = "phút"),
                    Habit(id = 3, name = "Đi bộ buổi tối", category = HabitCategory.EXERCISE, targetValue = 30f, unit = "phút")
                ),
                isLoading = false
            )
        )
    }
}
