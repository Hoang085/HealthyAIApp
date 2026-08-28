package com.healthyai.app.presentation.screen.habits

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(viewModel: HabitsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thói quen của tôi", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { /* TODO: Filter/Sort */ }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Lọc")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* TODO: Mở dialog thêm thói quen */ },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Thêm thói quen") }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.habits.isEmpty() -> {
                Box(
                    Modifier.padding(paddingValues).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.PlaylistAdd, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.outline)
                        Text("Chưa có thói quen nào", style = MaterialTheme.typography.titleMedium)
                        Text("Nhấn nút + để thêm thói quen đầu tiên", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.padding(paddingValues).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.habits, key = { it.id }) { habit ->
                        HabitItemCard(
                            habit = habit,
                            isJustLogged = uiState.showLogSuccessFor == habit.id,
                            onLogClick = { viewModel.logHabit(habit, habit.targetValue) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) } // FAB clearance
                }
            }
        }
    }
}

@Composable
private fun HabitItemCard(
    habit: Habit,
    isJustLogged: Boolean,
    onLogClick: () -> Unit
) {
    val containerColor = if (isJustLogged)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surface

    val categoryEmoji = habit.category.emoji

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        ListItem(
            headlineContent = {
                Text("$categoryEmoji ${habit.name}", fontWeight = FontWeight.SemiBold)
            },
            supportingContent = {
                Text(
                    "Mục tiêu: ${habit.targetValue} ${habit.unit}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                if (isJustLogged) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Đã ghi nhận", tint = MaterialTheme.colorScheme.primary)
                } else {
                    FilledTonalButton(onClick = onLogClick) {
                        Text("Ghi nhận")
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = containerColor)
        )
    }
}
