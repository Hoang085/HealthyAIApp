package com.healthyai.app.presentation.screen.habits

import android.widget.Toast
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthyai.app.R
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.presentation.ui.component.ErrorStateCard
import com.healthyai.app.presentation.ui.format.formatHabitProgress
import com.healthyai.app.presentation.ui.format.formatTargetValue
import com.healthyai.app.presentation.ui.theme.HealthyAITheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HabitsScreen(
    onNavigateToAddHabit: () -> Unit,
    onNavigateToEditHabit: (Long) -> Unit,
    /**
     * Danh mục cần mở sẵn hộp thoại ghi nhận, do `AppNavigation` đọc từ `SavedStateHandle` của
     * `NavBackStackEntry` **đang hiện** và truyền xuống (xem `KEY_LOG_CATEGORY`); `null` khi không có
     * yêu cầu nào.
     */
    pendingLogCategory: String? = null,
    /** Đã nhận yêu cầu → `AppNavigation` xoá khoá khỏi `SavedStateHandle` để không mở lại hộp thoại. */
    onPendingLogCategoryConsumed: () -> Unit = {},
    viewModel: HabitsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Yêu cầu ghi nhận từ màn khác (mục 3 #14 — nút trong thẻ gợi ý AI; phụ lục C #22 — nút
    // "Thêm vào nhật ký" của màn Ghi chép): chuyển danh mục cho ViewModel rồi báo đã nhận để khoá
    // trong SavedStateHandle được xoá — nhờ vậy quay lại tab Thói quen lần sau không mở lại hộp thoại.
    LaunchedEffect(pendingLogCategory) {
        val category = pendingLogCategory ?: return@LaunchedEffect
        viewModel.onLogRequest(category)
        onPendingLogCategoryConsumed()
    }

    // Phản hồi sau khi ghi nhận: `showLogSuccessFor` là sự kiện một lần → hiện Toast rồi xoá cờ,
    // nhờ vậy mỗi lần ghi (kể cả ghi lại cùng một thói quen) đều có thông báo.
    LaunchedEffect(uiState.showLogSuccessFor) {
        val habitId = uiState.showLogSuccessFor ?: return@LaunchedEffect
        val habitName = uiState.habits.firstOrNull { it.habit.id == habitId }?.habit?.name
        viewModel.onLogSuccessShown()
        Toast.makeText(
            context,
            if (habitName != null) context.getString(R.string.habits_log_success_named, habitName) else context.getString(R.string.habits_log_success),
            Toast.LENGTH_SHORT
        ).show()
    }

    HabitsScreenContent(
        uiState = uiState,
        onFilterSelected = viewModel::setFilter,
        onAddHabitClick = onNavigateToAddHabit,
        onHabitClick = onNavigateToEditHabit,
        onLogHabit = viewModel::logHabit,
        onLogRequestHandled = viewModel::onLogRequestHandled,
        onDismissError = viewModel::dismissError
    )
}

@Composable
fun HabitsScreenContent(
    uiState: HabitsUiState,
    onFilterSelected: (HabitFilter) -> Unit,
    onAddHabitClick: () -> Unit,
    onHabitClick: (Long) -> Unit,
    onLogHabit: (Habit, Float) -> Unit,
    onLogRequestHandled: () -> Unit,
    onDismissError: () -> Unit
) {
    var showLogDialogFor by remember { mutableStateOf<Habit?>(null) }
    val context = LocalContext.current

    // Yêu cầu ghi nhận từ màn khác (mục 3 #14 — nút trong thẻ gợi ý AI; phụ lục C #22 — nút
    // "Thêm vào nhật ký" của màn Ghi chép): mở sẵn hộp thoại ghi nhận của thói quen đúng danh mục;
    // nếu chưa có thói quen nào thuộc danh mục đó thì nhắc tạo mới.
    LaunchedEffect(uiState.logRequest) {
        when (val request = uiState.logRequest) {
            null -> Unit
            is LogRequest.Open -> {
                showLogDialogFor = request.habit
                onLogRequestHandled()
            }
            is LogRequest.Missing -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.habits_no_matching_habit),
                    Toast.LENGTH_LONG
                ).show()
                onLogRequestHandled()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddHabitClick,
                containerColor = Color(0xFF2E7D32),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.habits_add_content_description), modifier = Modifier.size(32.dp))
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

            // Lỗi ghi nhận / đọc dữ liệu (ví dụ ghi log thất bại) — trước đây `uiState.error`
            // không được hiển thị ở màn này (phụ lục C #10).
            if (uiState.error != null) {
                item {
                    ErrorStateCard(message = uiState.error, onDismiss = onDismissError)
                }
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
            // Giá trị mặc định = mục tiêu, hiển thị dạng số sạch ("2000" thay vì "2000.0")
            var logValue by remember { mutableStateOf(formatTargetValue(habit.targetValue)) }
            // Nhập rỗng / không phải số → giữ hộp thoại mở và hiện lỗi dưới ô nhập (thay vì im lặng)
            var errorMessage by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { showLogDialogFor = null },
                title = { Text(stringResource(R.string.habits_log_dialog_title, habit.name)) },
                text = {
                    OutlinedTextField(
                        value = logValue,
                        onValueChange = {
                            logValue = it
                            errorMessage = null // người dùng sửa lại → xoá thông báo lỗi cũ
                        },
                        label = { Text(stringResource(R.string.habits_log_amount_label, habit.unit)) },
                        isError = errorMessage != null,
                        supportingText = errorMessage?.let { message ->
                            { Text(message, color = MaterialTheme.colorScheme.error) }
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        when (val result = parseLogValue(logValue)) {
                            is LogValueResult.Invalid -> errorMessage = result.message
                            is LogValueResult.Valid -> {
                                onLogHabit(habit, result.value)
                                showLogDialogFor = null
                            }
                        }
                    }) {
                        Text(stringResource(R.string.common_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogDialogFor = null }) {
                        Text(stringResource(R.string.common_cancel))
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
            stringResource(R.string.habits_header_title),
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
    selectedFilter: HabitFilter,
    onFilterSelected: (HabitFilter) -> Unit
) {
    val filters = HabitFilter.entries

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(stringResource(filter.labelRes)) },
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
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
                        stringResource(R.string.habits_completed),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                } else {
                    val progressText = formatHabitProgress(habit, currentProgress)
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
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(48.dp).width(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.common_done), color = Color.Gray, fontWeight = FontWeight.Bold)
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
                    Text(stringResource(R.string.habits_log_button), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EmptyHabitsView() {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.habits_empty_title), color = Color.Gray)
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
            onLogHabit = { _, _ -> },
            onLogRequestHandled = {},
            onDismissError = {}
        )
    }
}
