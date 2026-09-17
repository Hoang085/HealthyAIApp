package com.healthyai.app.presentation.screen.habits.addedit

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (uiState.isEditMode) "Sửa thói quen" else "Thêm thói quen mới",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (uiState.isEditMode) {
                        IconButton(onClick = { viewModel.deleteHabit() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color.Red)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text(
                    if (uiState.isEditMode) "Cập nhật" else "Tạo thói quen",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = Color(0xFFF8F9FA)
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
                label = { Text("Tên thói quen (ví dụ: Chạy bộ buổi sáng)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2E7D32),
                    focusedLabelColor = Color(0xFF2E7D32)
                )
            )

            // Danh mục
            Text("Danh mục", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
            Text("Mục tiêu hàng ngày", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
            Text("Thời gian thực hiện", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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

            // Tần suất
            Text("Tần suất", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            HabitFrequency.values().forEach { freq ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.onFrequencyChange(freq) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = uiState.frequency == freq,
                        onClick = { viewModel.onFrequencyChange(freq) },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2E7D32))
                    )
                    Text(freq.displayName, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Thông báo lỗi
        if (uiState.error != null) {
            AlertDialog(
                onDismissRequest = viewModel::dismissError,
                title = { Text("Lỗi") },
                text = { Text(uiState.error!!) },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissError) {
                        Text("Đóng")
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
