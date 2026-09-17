package com.healthyai.app.presentation.screen.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HabitsScreen(
    onNavigateToAddHabit: () -> Unit,
    onNavigateToEditHabit: (Long) -> Unit,
    viewModel: HabitsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HabitsScreenContent(
        uiState = uiState,
        onFilterSelected = viewModel::setFilter,
        onAddHabitClick = onNavigateToAddHabit,
        onHabitClick = onNavigateToEditHabit,
        onLogHabit = viewModel::logHabit
    )
}

@Composable
fun HabitsScreenContent(
    uiState: HabitsUiState,
    onFilterSelected: (String) -> Unit,
    onAddHabitClick: () -> Unit,
    onHabitClick: (Long) -> Unit,
    onLogHabit: (Habit, Float) -> Unit
) {
    var showLogDialogFor by remember { mutableStateOf<Habit?>(null) }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddHabitClick,
                containerColor = Color(0xFF2E7D32),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm thói quen", modifier = Modifier.size(32.dp))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                HabitHeader()
            }

            item {
                FilterChipsSection(
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = onFilterSelected
                )
            }

            if (uiState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                    }
                }
            } else if (uiState.filteredHabits.isEmpty()) {
                item { EmptyHabitsView() }
            } else {
                items(
                    items = uiState.filteredHabits,
                    key = { it.habit.id }
                ) { habitWithProgress ->
                    HabitCard(
                        habit = habitWithProgress.habit,
                        currentProgress = habitWithProgress.progress,
                        isCompleted = habitWithProgress.isCompleted,
                        onClick = { onHabitClick(habitWithProgress.habit.id) },
                        onLogClick = { showLogDialogFor = habitWithProgress.habit }
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // Dialog ghi nhận thói quen
        showLogDialogFor?.let { habit ->
            var logValue by remember { mutableStateOf(habit.targetValue.toString()) }
            
            AlertDialog(
                onDismissRequest = { showLogDialogFor = null },
                title = { Text("Ghi nhận ${habit.name}") },
                text = {
                    OutlinedTextField(
                        value = logValue,
                        onValueChange = { logValue = it },
                        label = { Text("Số lượng (${habit.unit})") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        logValue.toFloatOrNull()?.let { onLogHabit(habit, it) }
                        showLogDialogFor = null
                    }) {
                        Text("Lưu")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogDialogFor = null }) {
                        Text("Hủy")
                    }
                }
            )
        }
    }
}

@Composable
private fun HabitHeader() {
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("'Hôm nay,' EEEE, dd 'tháng' MM", Locale("vi")))
    Column {
        Text(
            "Theo dõi thói quen",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp
            )
        )
        Text(
            date,
            style = MaterialTheme.typography.bodyLarge.copy(color = Color.Gray)
        )
    }
}

@Composable
private fun FilterChipsSection(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("Tất cả", "Sáng", "Chiều", "Tối")

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF1B5E20),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFFE8F5E9),
                    labelColor = Color(0xFF1B5E20)
                ),
                border = null,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun HabitCard(
    habit: Habit,
    currentProgress: Float,
    isCompleted: Boolean,
    onClick: () -> Unit,
    onLogClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Circle
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) Color(0xFFC8E6C9) else Color(0xFFEDF2F7)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(32.dp))
                } else {
                    // Use simple icons or emojis as in the image
                    val emoji = habit.category.emoji
                    Text(emoji, fontSize = 28.sp)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    habit.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                if (isCompleted) {
                    Text(
                        "Hoàn thành",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                } else {
                    val progressText = formatProgress(habit, currentProgress)
                    Text(
                        progressText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            
            if (isCompleted) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFEEEEEE),
                    modifier = Modifier.height(48.dp).width(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Xong", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = onLogClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(48.dp).width(100.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ghi", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EmptyHabitsView() {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        Text("Chưa có thói quen nào", color = Color.Gray)
    }
}

private fun formatProgress(habit: Habit, current: Float): String {
    return when (habit.category) {
        HabitCategory.WATER -> "${"%.1f".format(current)}L / ${"%.1f".format(habit.targetValue / 1000f)}L"
        HabitCategory.EXERCISE -> "${current.toInt()} / ${habit.targetValue.toInt()} phút"
        HabitCategory.SLEEP -> "${"%.1f".format(current)} / ${"%.1f".format(habit.targetValue)} giờ"
        else -> "${current.toInt()} / ${habit.targetValue.toInt()} ${habit.unit}"
    }
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
fun HabitsScreenPreview() {
    HealthyAITheme {
        HabitsScreenContent(
            uiState = HabitsUiState(
                habits = listOf(
                    HabitWithProgress(
                        Habit(id = 1, name = "Uống nước", category = HabitCategory.WATER, targetValue = 2000f, unit = "ml"),
                        progress = 500f, isCompleted = false
                    )
                ),
                isLoading = false
            ),
            onFilterSelected = {},
            onAddHabitClick = {},
            onHabitClick = {},
            onLogHabit = { _, _ -> }
        )
    }
}
