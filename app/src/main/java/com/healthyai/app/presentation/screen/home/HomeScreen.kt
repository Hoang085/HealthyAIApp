package com.healthyai.app.presentation.screen.home

import android.content.Context
import android.content.Intent
import android.Manifest
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HealthConnectPermissionStatus
import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.domain.model.NotificationPermissionStatus
import com.healthyai.app.presentation.ui.component.ErrorStateCard
import com.healthyai.app.presentation.ui.theme.HealthyAITheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Package của ứng dụng Health Connect trên Play Store. */
private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Hộp thoại quyền của Health Connect; trả về tập quyền người dùng đã đồng ý.
    val healthConnectLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        viewModel.onHealthConnectPermissionsResult(grantedPermissions)
    }

    // Hộp thoại quyền thông báo (POST_NOTIFICATIONS, Android 13+). Trạng thái thật được đọc lại
    // trong ViewModel nên không cần dùng tham số `granted` của launcher.
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.onNotificationPermissionResult()
    }

    // Người dùng có thể cấp quyền trong app Health Connect hoặc bật thông báo trong Settings rồi
    // quay lại app → kiểm tra lại mỗi khi màn Home hiện ra.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshSystemPermissions()
    }

    HomeScreenContent(
        uiState = uiState,
        onLogout = onLogout,
        onDismissError = viewModel::dismissError,
        onRequestHealthConnectPermission = {
            healthConnectLauncher.launch(uiState.healthConnect.requiredPermissions)
        },
        onOpenHealthConnectStore = { openHealthConnectStore(context) },
        onRequestNotificationPermission = {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        onOpenNotificationSettings = { openAppNotificationSettings(context) }
    )
}

/** Mở Play Store để cài / cập nhật Health Connect; nếu máy không có Play Store thì mở link web. */
private fun openHealthConnectStore(context: Context) {
    val marketUri = Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE")
    val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$HEALTH_CONNECT_PACKAGE")

    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, marketUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, webUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

/**
 * Mở màn cài đặt thông báo của app (dùng khi người dùng đã tắt thông báo ở cấp app —
 * lúc đó xin lại quyền `POST_NOTIFICATIONS` không còn hiệu lực).
 */
private fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    runCatching {
        context.startActivity(intent)
    }.onFailure {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onLogout: () -> Unit,
    onDismissError: () -> Unit,
    onRequestHealthConnectPermission: () -> Unit,
    onOpenHealthConnectStore: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    Scaffold(
        // Nền theo theme (sáng/tối) thay vì #F8F9FA hard-code → dark mode không còn chữ sáng trên nền sáng
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { GreetingHeader(onLogout = onLogout) }
            item { HealthSummaryCard(uiState = uiState) }

            if (uiState.showHealthConnectCard) {
                item {
                    HealthConnectPermissionCard(
                        status = uiState.healthConnect.status,
                        onRequestPermission = onRequestHealthConnectPermission,
                        onOpenHealthConnectStore = onOpenHealthConnectStore
                    )
                }
            }

            if (uiState.showReminderNotificationCard) {
                item {
                    ReminderNotificationCard(
                        status = uiState.notificationPermission,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onOpenNotificationSettings = onOpenNotificationSettings
                    )
                }
            }

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
                items(uiState.habits, key = { it.habit.id }) { item ->
                    HabitItem(habit = item.habit, isCompleted = item.isCompleted)
                }
            }
            
            if (uiState.error != null) {
                item {
                    ErrorStateCard(message = uiState.error, onDismiss = onDismissError)
                }
            }
        }
    }
}

@Composable
private fun GreetingHeader(onLogout: () -> Unit) {
    val greeting = when (LocalTime.now().hour) {
        in 5..11  -> "Chào buổi sáng,"
        in 12..17 -> "Chào buổi chiều,"
        in 18..21 -> "Chào buổi tối,"
        else      -> "Chào buổi đêm,"
    }
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd 'tháng' MM", Locale("vi")))

    // Nút Đăng xuất (mục 3 #19): hỏi lại trước khi thoát phiên để tránh bấm nhầm.
    var showLogoutConfirm by remember { mutableStateOf(false) }

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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = { showLogoutConfirm = true }) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Đăng xuất",
                    tint = MaterialTheme.colorScheme.primary
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

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Đăng xuất") },
            text = {
                Text(
                    "Bạn có chắc muốn đăng xuất? Thói quen và nhật ký trên máy vẫn được giữ nguyên."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    }
                ) {
                    Text("Đăng xuất")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Huỷ")
                }
            }
        )
    }
}

@Composable
private fun HealthSummaryCard(uiState: HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
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
private fun HabitItem(habit: Habit, isCompleted: Boolean) {
    val (icon, color) = habitVisuals[habit.category] ?: (Icons.Default.TaskAlt to Color.Gray)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
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
                    .background(if (isCompleted) Color(0xFFC8E6C9) else color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isCompleted) Color(0xFF1B5E20) else color
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    habit.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (isCompleted) "Hoàn thành" else "Mục tiêu: ${habit.targetValue.toInt()} ${habit.unit}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isCompleted) Color(0xFF4CAF50) else Color.Gray,
                        fontWeight = if (isCompleted) FontWeight.SemiBold else FontWeight.Normal
                    )
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
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Đã hoàn thành",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
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
                    HomeHabitItem(
                        Habit(id = 1, name = "Uống nước", category = HabitCategory.WATER, targetValue = 2000f, unit = "ml"),
                        isCompleted = true
                    ),
                    HomeHabitItem(
                        Habit(id = 2, name = "Thiền buổi sáng", category = HabitCategory.MINDFULNESS, targetValue = 15f, unit = "phút"),
                        isCompleted = false
                    ),
                    HomeHabitItem(
                        Habit(id = 3, name = "Đi bộ buổi tối", category = HabitCategory.EXERCISE, targetValue = 30f, unit = "phút"),
                        isCompleted = false
                    )
                ),
                isLoading = false
            ),
            onDismissError = {},
            onRequestHealthConnectPermission = {},
            onOpenHealthConnectStore = {},
            onRequestNotificationPermission = {},
            onOpenNotificationSettings = {},
            onLogout = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenMissingHealthConnectPermissionPreview() {
    HealthyAITheme {
        HomeScreenContent(
            uiState = HomeUiState(
                healthConnect = HealthConnectPermissionStatus(
                    status = HealthConnectStatus.AVAILABLE,
                    requiredPermissions = setOf("android.permission.health.READ_STEPS"),
                    hasAllPermissions = false
                ),
                isLoading = false
            ),
            onDismissError = {},
            onRequestHealthConnectPermission = {},
            onOpenHealthConnectStore = {},
            onRequestNotificationPermission = {},
            onOpenNotificationSettings = {},
            onLogout = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenNotificationsBlockedPreview() {
    HealthyAITheme {
        HomeScreenContent(
            uiState = HomeUiState(
                notificationPermission = NotificationPermissionStatus(
                    notificationsEnabled = false,
                    canRequestPermission = true
                ),
                isLoading = false
            ),
            onDismissError = {},
            onRequestHealthConnectPermission = {},
            onOpenHealthConnectStore = {},
            onRequestNotificationPermission = {},
            onOpenNotificationSettings = {},
            onLogout = {}
        )
    }
}
