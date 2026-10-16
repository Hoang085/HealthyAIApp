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
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthyai.app.R
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.model.HealthConnectPermissionStatus
import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.domain.model.NotificationPermissionStatus
import com.healthyai.app.domain.model.StepCounterStatus
import com.healthyai.app.presentation.ui.component.ErrorStateCard
import com.healthyai.app.presentation.ui.format.formatHeartRate
import com.healthyai.app.presentation.ui.format.formatSleepDuration
import com.healthyai.app.presentation.ui.format.formatStepCount
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
    /** Ô "Tim" trên thẻ "Tóm tắt sức khỏe" → mở màn đo nhịp tim bằng camera + đèn flash. */
    onMeasureHeartRate: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Hộp thoại quyền của Health Connect trả về tập quyền người dùng đã đồng ý.
    val healthConnectLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        viewModel.onHealthConnectPermissionsResult(grantedPermissions)
    }

    // Hộp thoại quyền thông báo (POST_NOTIFICATIONS, Android 13+); trạng thái thật đọc lại trong ViewModel.
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.onNotificationPermissionResult()
    }

    // Hộp thoại quyền đếm bước chân (ACTIVITY_RECOGNITION, Android 10+); cũng đọc lại trạng thái thật sau khi đóng.
    val stepPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.onStepPermissionResult()
    }

    // Người dùng có thể cấp quyền trong Health Connect hoặc bật thông báo ở Settings rồi quay lại → kiểm tra lại mỗi khi Home hiện ra.
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
        onOpenNotificationSettings = { openAppNotificationSettings(context) },
        onRequestStepPermission = {
            stepPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        },
        onOpenAppSettings = { openAppSettings(context) },
        onEditSleepSchedule = viewModel::openSleepScheduleEditor,
        onMeasureHeartRate = onMeasureHeartRate,
        onCloudSyncToggle = viewModel::setCloudSyncEnabled,
        onHealthDataSourceToggle = viewModel::setHealthConnectDataEnabled
    )

    // Khung giờ ngủ: hộp thoại đặt giờ đi ngủ/giờ thức; trạng thái ở ViewModel để test được bằng unit test JVM.
    uiState.sleepScheduleDraft?.let { draft ->
        SleepScheduleEditorDialog(
            draft = draft,
            error = uiState.sleepScheduleError,
            onDraftChange = viewModel::onSleepScheduleDraftChange,
            onDismiss = viewModel::dismissSleepScheduleEditor,
            onSave = viewModel::saveSleepSchedule
        )
    }
}

/** Mở phần cài đặt quyền của app (khi hộp thoại xin quyền không còn hiện được nữa). */
private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    runCatching { context.startActivity(intent) }
}

/** Mở Play Store để cài/cập nhật Health Connect; máy không có Play Store thì mở link web. */
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

/** Mở màn cài đặt thông báo của app (khi người dùng đã tắt thông báo ở cấp app thì xin lại quyền không còn hiệu lực). */
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
    onOpenNotificationSettings: () -> Unit,
    onRequestStepPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onEditSleepSchedule: () -> Unit,
    /** Bấm ô "Tim" (nút "Đo ngay") → mở màn đo nhịp tim bằng camera + đèn flash. */
    onMeasureHeartRate: () -> Unit,
    // Hai tham số dưới có giá trị mặc định để các @Preview trong file khỏi khai báo lại; HomeScreen luôn truyền đủ.
    onCloudSyncToggle: (Boolean) -> Unit = {},
    /** Switch "Dùng dữ liệu Health Connect" → đổi nguồn số liệu của thẻ "Tóm tắt sức khỏe". */
    onHealthDataSourceToggle: (Boolean) -> Unit = {}
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
            item { GreetingHeader(userName = uiState.userName, onLogout = onLogout) }
            item { HealthSummaryCard(uiState = uiState, onMeasureHeartRate = onMeasureHeartRate) }

            // Switch nguồn dữ liệu: BẬT = lấy số trực tiếp từ Health Connect, TẮT = dùng dữ liệu trong app.
            item {
                HealthDataSourceCard(
                    settings = uiState.healthConnectData,
                    onToggle = onHealthDataSourceToggle
                )
            }

            // Ngay dưới thẻ "Tóm tắt sức khỏe": chỗ người dùng đặt khung giờ ngủ mà app dùng để đo.
            item {
                SleepScheduleCard(
                    schedule = uiState.sleepSchedule,
                    onEdit = onEditSleepSchedule
                )
            }

            // Đồng bộ đám mây: mặc định TẮT, người dùng tự bật.
            item {
                CloudSyncCard(
                    settings = uiState.cloudSync,
                    isSignedIn = uiState.isSignedIn,
                    onToggle = onCloudSyncToggle
                )
            }

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

            // Máy có cảm biến đếm bước nhưng app chưa được cấp quyền ACTIVITY_RECOGNITION
            if (uiState.showStepCounterCard) {
                item {
                    StepCounterPermissionCard(
                        status = uiState.stepCounter,
                        onRequestPermission = onRequestStepPermission,
                        onOpenAppSettings = onOpenAppSettings
                    )
                }
            }

            item {
                Text(
                    stringResource(R.string.home_habits_today),
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
private fun GreetingHeader(userName: String?, onLogout: () -> Unit) {
    // Không để dấu phẩy in sẵn trong câu chào: chỉ thêm khi thực sự có tên ở dòng dưới, tránh chế độ khách hiện dấu phẩy treo lơ lửng.
    val greeting = when (LocalTime.now().hour) {
        in 5..11  -> stringResource(R.string.home_greeting_morning)
        in 12..17 -> stringResource(R.string.home_greeting_afternoon)
        in 18..21 -> stringResource(R.string.home_greeting_evening)
        else      -> stringResource(R.string.home_greeting_night)
    }
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd 'tháng' MM", Locale("vi")))

    // Nút Đăng xuất: hỏi lại trước khi thoát phiên để tránh bấm nhầm.
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
                if (userName != null) "$greeting," else greeting,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
            )
            // Chưa đăng nhập (chế độ khách) → không có dòng tên; trước đây luôn là "Alex" cứng.
            if (userName != null) {
                Text(
                    userName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = Color(0xFF2E7D32)
                    )
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = { showLogoutConfirm = true }) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = stringResource(R.string.home_logout_content_description),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            // Ảnh đại diện tạm
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
            title = { Text(stringResource(R.string.home_logout)) },
            text = {
                Text(
                    stringResource(R.string.home_logout_dialog_message)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    }
                ) {
                    Text(stringResource(R.string.home_logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(stringResource(R.string.common_cancel_khong_dau))
                }
            }
        )
    }
}

@Composable
private fun HealthSummaryCard(uiState: HomeUiState, onMeasureHeartRate: () -> Unit) {
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
                    stringResource(R.string.home_health_summary),
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
                    label = stringResource(R.string.home_label_steps),
                    // Ưu tiên Health Connect; nếu Health Connect không có số thì dùng cảm biến của máy
                    value = formatStepCount(uiState.displaySteps),
                    color = Color(0xFF4CAF50)
                )
                HealthCircleStat(
                    icon = Icons.Default.Bedtime,
                    label = stringResource(R.string.home_label_sleep),
                    // Ưu tiên Health Connect (có thể từ đồng hồ đeo tay); không có số thì dùng ước lượng từ cảm biến điện thoại.
                    value = formatSleepDuration(uiState.displaySleepHours),
                    color = Color(0xFF2E4032)
                )
                HealthCircleStat(
                    icon = Icons.Default.Favorite,
                    label = stringResource(R.string.home_label_heart),
                    // Ưu tiên số Health Connect trong ngày, rồi nhịp tim ghi trong ngày; chưa có số nào thì hiện "-- bpm" thay vì "0 bpm".
                    value = formatHeartRate(uiState.displayHeartRate),
                    color = Color(0xFF4CAF50),
                    actionLabel = stringResource(R.string.home_measure_now),
                    onClick = onMeasureHeartRate
                )
            }
        }
    }
}

@Composable
private fun HealthCircleStat(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    /** Nhãn hành động hiện dưới số đo (ví dụ "Đo ngay"); `null` = ô chỉ để xem. */
    actionLabel: String? = null,
    onClick: (() -> Unit)? = null
) {
    // Ô có hành động bọc clickable (kèm role = Button cho trình đọc màn hình); clip + bo góc đặt trước clickable để gợn sóng không tràn ra ngoài.
    val interactiveModifier = if (onClick != null) {
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = interactiveModifier.padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
        if (actionLabel != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )
        }
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
                    text = if (isCompleted) stringResource(R.string.home_habit_completed) else stringResource(R.string.home_habit_target, habit.targetValue.toInt(), habit.unit),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isCompleted) Color(0xFF4CAF50) else Color.Gray,
                        fontWeight = if (isCompleted) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            }
            // Ô đánh dấu hoàn thành dạng vòng tròn
            if (isCompleted) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFF2E7D32), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(R.string.home_habit_completed_content_description),
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
            Text(stringResource(R.string.home_add_first_habit), fontWeight = FontWeight.Bold)
        }
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
                // Đã đăng nhập → lời chào 2 dòng với tên thật của phiên
                userName = "alex.healthyai",
                isLoading = false
            ),
            onDismissError = {},
            onRequestHealthConnectPermission = {},
            onOpenHealthConnectStore = {},
            onRequestNotificationPermission = {},
            onOpenNotificationSettings = {},
            onRequestStepPermission = {},
            onOpenAppSettings = {},
            onEditSleepSchedule = {},
            onMeasureHeartRate = {},
            onLogout = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenGuestPreview() {
    HealthyAITheme {
        HomeScreenContent(
            // Chế độ khách → userName = null → KHÔNG có dòng tên (trước đây luôn hiện "Alex")
            uiState = HomeUiState(
                habits = listOf(
                    HomeHabitItem(
                        Habit(id = 1, name = "Uống nước", category = HabitCategory.WATER, targetValue = 2000f, unit = "ml"),
                        isCompleted = false
                    )
                ),
                userName = null,
                isLoading = false
            ),
            onDismissError = {},
            onRequestHealthConnectPermission = {},
            onOpenHealthConnectStore = {},
            onRequestNotificationPermission = {},
            onOpenNotificationSettings = {},
            onRequestStepPermission = {},
            onOpenAppSettings = {},
            onEditSleepSchedule = {},
            onMeasureHeartRate = {},
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
            onRequestStepPermission = {},
            onOpenAppSettings = {},
            onEditSleepSchedule = {},
            onMeasureHeartRate = {},
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
            onRequestStepPermission = {},
            onOpenAppSettings = {},
            onEditSleepSchedule = {},
            onMeasureHeartRate = {},
            onLogout = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenStepCounterCardPreview() {
    HealthyAITheme {
        HomeScreenContent(
            // Máy có cảm biến đếm bước nhưng chưa cấp quyền → hiện thẻ "Bật đếm bước chân"
            uiState = HomeUiState(
                stepCounter = StepCounterStatus(sensorAvailable = true, permissionGranted = false),
                isLoading = false
            ),
            onDismissError = {},
            onRequestHealthConnectPermission = {},
            onOpenHealthConnectStore = {},
            onRequestNotificationPermission = {},
            onOpenNotificationSettings = {},
            onRequestStepPermission = {},
            onOpenAppSettings = {},
            onEditSleepSchedule = {},
            onMeasureHeartRate = {},
            onLogout = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenStepsFromSensorPreview() {
    HealthyAITheme {
        HomeScreenContent(
            // Health Connect không có số bước → hiển thị số bước từ cảm biến của máy
            uiState = HomeUiState(
                stepCounter = StepCounterStatus(sensorAvailable = true, permissionGranted = true),
                sensorSteps = 4230L,
                isLoading = false
            ),
            onDismissError = {},
            onRequestHealthConnectPermission = {},
            onOpenHealthConnectStore = {},
            onRequestNotificationPermission = {},
            onOpenNotificationSettings = {},
            onRequestStepPermission = {},
            onOpenAppSettings = {},
            onEditSleepSchedule = {},
            onMeasureHeartRate = {},
            onLogout = {}
        )
    }
}