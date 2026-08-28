package com.healthyai.app.presentation.screen.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthyai.app.domain.model.Suggestion
import com.healthyai.app.domain.model.SuggestionPriority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(viewModel: InsightsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gợi ý AI", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Làm mới")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text("Đang phân tích dữ liệu...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Phân tích ${uiState.lastAnalyzedDays} ngày qua",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (uiState.suggestions.isEmpty()) {
                    item { AllGoodCard() }
                } else {
                    items(uiState.suggestions, key = { it.id }) { suggestion ->
                        SuggestionCard(suggestion = suggestion)
                    }
                }
            }
        }
    }
}

@Composable
private fun AllGoodCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🎉", style = MaterialTheme.typography.displayLarge)
            Text(
                "Bạn đang làm rất tốt!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Không có cảnh báo nào. Hãy tiếp tục duy trì thói quen tốt của mình nhé!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun SuggestionCard(suggestion: Suggestion) {
    val (containerColor, contentColor) = when (suggestion.priority) {
        SuggestionPriority.CRITICAL -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        SuggestionPriority.HIGH     -> Color(0xFFFFF3E0) to Color(0xFFBF360C)
        SuggestionPriority.MEDIUM   -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        SuggestionPriority.LOW      -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    val priorityLabel = when (suggestion.priority) {
        SuggestionPriority.CRITICAL -> "🚨 Khẩn cấp"
        SuggestionPriority.HIGH     -> "⚠️ Quan trọng"
        SuggestionPriority.MEDIUM   -> "💡 Gợi ý"
        SuggestionPriority.LOW      -> "ℹ️ Thông tin"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(suggestion.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = contentColor, modifier = Modifier.weight(1f))
                Text(priorityLabel, style = MaterialTheme.typography.labelSmall, color = contentColor)
            }
            Text(suggestion.message, style = MaterialTheme.typography.bodyMedium, color = contentColor)
            suggestion.actionLabel?.let { label ->
                TextButton(onClick = { /* TODO: Action */ }) {
                    Text(label, color = contentColor, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.padding(start = 4.dp).size(16.dp), tint = contentColor)
                }
            }
        }
    }
}
