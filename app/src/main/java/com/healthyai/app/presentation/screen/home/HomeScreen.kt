package com.healthyai.app.presentation.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Hiển thị snackbar khi có lỗi
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            viewModel.dismissError()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { GreetingHeader() }
        item { HealthSummaryCard(uiState = uiState) }
        item {
            Text(
                "Thói quen hôm nay",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        when {
            uiState.isLoading -> item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                }
            }
            uiState.habits.isEmpty() -> item { EmptyHabitsCard() }
            else -> items(uiState.habits) { HabitCard(it) }
        }
    }
}

@Composable
private fun GreetingHeader() {
    val greeting = when (LocalTime.now().hour) {
        in 5..11  -> "Chào buổi sáng ☀️"
        in 12..17 -> "Chào buổi chiều 🌤️"
        in 18..21 -> "Chào buổi tối 🌙"
        else      -> "Chào buổi đêm ⭐"
    }
    val today = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale("vi")))

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(greeting, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(today, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HealthSummaryCard(uiState: HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        if (!uiState.isHealthConnectAvailable) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.HealthAndSafety, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Kết nối Health Connect",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Đồng bộ bước chân, giấc ngủ và nhịp tim từ thiết bị",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Sức khỏe hôm nay 📊",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HealthStatItem(Icons.Filled.DirectionsWalk, "${uiState.healthData.steps}", "bước chân")
                    HealthStatItem(Icons.Filled.Bedtime, String.format("%.1f", uiState.healthData.sleepHours), "giờ ngủ")
                    HealthStatItem(Icons.Filled.Favorite, "${uiState.healthData.heartRate.toInt()}", "bpm")
                }
            }
        }
    }
}

@Composable
private fun HealthStatItem(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun HabitCard(habit: Habit) {
    val (icon, _) = habitIconMap[habit.category] ?: (Icons.Filled.FitnessCenter to "")
    Card(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(habit.name, fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text("Mục tiêu: ${habit.targetValue} ${habit.unit}", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) }
        )
    }
}

@Composable
private fun EmptyHabitsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.AddCircle, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Bắt đầu hành trình sức khỏe!", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("Chuyển sang tab Thói quen để thêm thói quen đầu tiên.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val habitIconMap = mapOf(
    HabitCategory.SLEEP       to (Icons.Filled.Bedtime to "💤"),
    HabitCategory.WATER       to (Icons.Filled.WaterDrop to "💧"),
    HabitCategory.EXERCISE    to (Icons.Filled.FitnessCenter to "🏃"),
    HabitCategory.NUTRITION   to (Icons.Filled.Restaurant to "🥗"),
    HabitCategory.MINDFULNESS to (Icons.Filled.SelfImprovement to "🧘"),
)
