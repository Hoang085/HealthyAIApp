package com.healthyai.app.presentation.screen.habits.addedit

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthyai.app.R
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HabitFrequency
import com.healthyai.app.domain.model.HabitTimeOfDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHabitScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditHabitViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    // Xóa mềm xong: báo cho người dùng biết dữ liệu vẫn được giữ, rồi quay lại danh sách.
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            Toast.makeText(context, context.getString(R.string.habit_deleted_toast), Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.habit_delete_dialog_title), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(R.string.habit_delete_dialog_message)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteHabit()
                    }
                ) {
                    Text(stringResource(R.string.common_delete), color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (uiState.isEditMode) stringResource(R.string.habit_edit_title) else stringResource(R.string.habit_add_title),
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (uiState.isEditMode) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete), tint = Color.Red)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            Button(
                onClick = { viewModel.saveHabit() },
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32),
                    disabledContainerColor = Color(0xFF2E7D32).copy(alpha = 0.4f)
                )
            ) {
                // Đang ghi DB: nút bị vô hiệu hoá + hiện vòng xoay (chống double-submit)
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (uiState.isEditMode) stringResource(R.string.habit_update_button) else stringResource(R.string.habit_create_button),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Tên thói quen
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.habit_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2E7D32),
                    focusedLabelColor = Color(0xFF2E7D32)
                )
            )

            // Danh mục
            Text(stringResource(R.string.habit_category_label), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HabitCategory.entries.forEach { category ->
                    CategoryItem(
                        category = category,
                        isSelected = uiState.category == category,
                        onClick = { viewModel.onCategoryChange(category) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Mục tiêu
            Text(stringResource(R.string.habit_daily_target_label), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.targetValue,
                    onValueChange = viewModel::onTargetValueChange,
                    modifier = Modifier.width(120.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2E7D32)
                    )
                )
                Text(uiState.unit, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }

            // Thời gian trong ngày
            Text(stringResource(R.string.habit_time_of_day_label), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HabitTimeOfDay.values().forEach { time ->
                    FilterChip(
                        selected = uiState.timeOfDay == time,
                        onClick = { viewModel.onTimeOfDayChange(time) },
                        label = { Text(time.displayName) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Chỉ hỗ trợ "Hàng ngày": logic tiến độ xét theo NGÀY, chọn tuần/tháng chỉ đổi nhãn vô nghĩa.
            Text(stringResource(R.string.habit_frequency_label), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = true,
                    onClick = null, // Chỉ còn một lựa chọn → không có gì để đổi
                    enabled = false,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF2E7D32),
                        disabledSelectedColor = Color(0xFF2E7D32)
                    )
                )
                Text(HabitFrequency.DAILY.displayName, fontSize = 16.sp)
            }
            Text(
                stringResource(R.string.habit_frequency_note),
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Thông báo lỗi
        if (uiState.error != null) {
            AlertDialog(
                onDismissRequest = viewModel::dismissError,
                title = { Text(stringResource(R.string.common_error_title)) },
                text = { Text(uiState.error!!) },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissError) {
                        Text(stringResource(R.string.common_close))
                    }
                }
            )
        }
    }
}

@Composable
fun CategoryItem(
    category: HabitCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFC8E6C9) else Color(0xFFF1F3F5),
        label = "categoryBackground"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF1B5E20) else Color.DarkGray,
        label = "categoryContent"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        label = "categoryScale"
    )

    Column(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(category.emoji, fontSize = 24.sp)
        Text(
            category.displayName,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = contentColor
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}
