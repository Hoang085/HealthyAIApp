package com.healthyai.app.presentation.screen.heartrate

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthyai.app.R
import com.healthyai.app.data.heartrate.CameraPpgAnalyzer
import com.healthyai.app.domain.model.HeartRateResult
import com.healthyai.app.domain.model.HeartRateZone
import com.healthyai.app.presentation.ui.component.ErrorStateCard
import com.healthyai.app.presentation.ui.theme.HealthyAITheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val CardGreen = Color(0xFF2E7D32)
private val HeartRed = Color(0xFFE53935)

/**
 * Màn **đo nhịp tim bằng camera + đèn flash** (PPG).
 *
 * Cách hoạt động: đèn flash chiếu sáng xuyên qua mô ngón tay, camera ghi lại sự thay đổi rất nhỏ về sắc
 * độ đỏ giữa các khung hình theo từng đợt máu dồn về đầu ngón tay; thuật toán `PpgAnalyzer` (tầng
 * domain) phân tích chu kỳ đổi màu đó để ra nhịp tim.
 *
 * Màn hình chỉ lo **phần cứng** (xin quyền, bật camera sau, bật đèn flash, đưa từng khung hình cho
 * ViewModel) và **phần hướng dẫn** cho người dùng đang đo; toàn bộ máy trạng thái nằm ở
 * [HeartRateViewModel] (test được trên JVM).
 */
@Composable
fun HeartRateScreen(
    onBack: () -> Unit,
    viewModel: HeartRateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) viewModel.onCameraFailed("ứng dụng chưa được cấp quyền Camera")
    }

    // Đang đo mà màn hình tắt thì camera cũng ngừng cho khung hình → giữ màn hình luôn sáng.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // Đèn flash + camera chỉ bật khi còn việc: đang chờ người dùng đặt ngón tay, hoặc đang đo.
    val cameraActive = hasCameraPermission && uiState.phase != HeartRatePhase.FINISHED

    HeartRateScreenContent(
        uiState = uiState,
        hasCameraPermission = hasCameraPermission,
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        onStart = viewModel::startMeasurement,
        onStop = viewModel::stopMeasurement,
        onRetry = viewModel::restart,
        onBack = onBack,
        onDismissError = viewModel::dismissError,
        previewContent = { modifier ->
            HeartRateCamera(
                active = cameraActive,
                onSample = viewModel::onPpgSample,
                onFlashUnitAvailable = { available ->
                    if (!available) viewModel.onFlashUnavailable()
                },
                onError = viewModel::onCameraFailed,
                modifier = modifier
            )
        }
    )
}

@Composable
fun HeartRateScreenContent(
    uiState: HeartRateUiState,
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onDismissError: () -> Unit,
    /** Vùng xem trước camera; mặc định là hình minh hoạ (dùng cho preview trong IDE). */
    previewContent: @Composable (Modifier) -> Unit = { modifier -> CameraPlaceholder(modifier) }
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ScreenHeader(onBack = onBack, subtitle = subtitleFor(uiState.phase))

            // Camera nằm ở **một vị trí cố định** trong cây giao diện và chỉ rời đi khi đã có kết quả
            // (`FINISHED`). Điều này rất quan trọng: nếu camera chỉ được compose ở nhánh "chờ" thì vừa
            // bấm "Bắt đầu đo" là nó bị gỡ khỏi giao diện ⇒ CameraX gỡ camera + tắt đèn flash và màn
            // đo **không còn khung hình nào** để phân tích. Khi đang đo, khung xem trước được **thu
            // nhỏ** (người dùng đang che camera nên không cần nhìn) nhưng **vẫn giữ kích thước hợp lệ**
            // — không ẩn hẳn — để bề mặt xem trước của CameraX luôn cấu hình được.
            if (hasCameraPermission && uiState.phase != HeartRatePhase.FINISHED) {
                val isIdle = uiState.phase == HeartRatePhase.IDLE
                previewContent(
                    Modifier
                        .fillMaxWidth()
                        .height(if (isIdle) 160.dp else 88.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
            }

            when {
                !hasCameraPermission -> CameraPermissionCard(onRequestPermission)

                uiState.phase == HeartRatePhase.IDLE -> {
                    InstructionStepsCard()
                    MeasurementButton(
                        label = stringResource(R.string.heart_rate_start),
                        icon = Icons.Default.Favorite,
                        container = HeartRed,
                        onClick = onStart
                    )
                }

                uiState.phase == HeartRatePhase.MEASURING -> {
                    MeasurementPanel(uiState)
                    GuidanceCard(uiState.guidance)
                    MeasurementButton(
                        label = stringResource(R.string.heart_rate_stop),
                        icon = Icons.Default.Refresh,
                        container = MaterialTheme.colorScheme.surfaceVariant,
                        content = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onStop
                    )
                }

                else -> ResultSection(
                    uiState = uiState,
                    onRetry = onRetry,
                    onDone = onBack
                )
            }

            uiState.error?.let { message ->
                ErrorStateCard(message = message, onDismiss = onDismissError)
            }

            MedicalDisclaimer()
        }
    }
}

@Composable
private fun subtitleFor(phase: HeartRatePhase): String = when (phase) {
    HeartRatePhase.IDLE -> stringResource(R.string.heart_rate_phase_idle)
    HeartRatePhase.MEASURING -> stringResource(R.string.heart_rate_phase_measuring)
    HeartRatePhase.FINISHED -> stringResource(R.string.heart_rate_phase_finished)
}
// ── Khung màn hình ────────────────────────────────────────────

@Composable
private fun ScreenHeader(onBack: () -> Unit, subtitle: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                stringResource(R.string.heart_rate_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp
                )
            )
        }
        Text(
            subtitle,
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
        )
    }
}

// ── Hướng dẫn (khi chưa đo) ───────────────────────────────────

/**
 * Hướng dẫn 3 bước — phần người dùng cần đọc **trước khi** bấm đo.
 */
@Composable
private fun InstructionStepsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TouchApp, null, tint = CardGreen, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.heart_rate_howto_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            InstructionStep(
                number = 1,
                text = stringResource(R.string.heart_rate_howto_step_1)
            )
            InstructionStep(
                number = 2,
                text = stringResource(R.string.heart_rate_howto_step_2)
            )
            InstructionStep(
                number = 3,
                text = stringResource(R.string.heart_rate_howto_step_3)
            )

            Surface(
                color = CardGreen.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FlashOn, null, tint = CardGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.heart_rate_flash_note),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructionStep(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(CardGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number.toString(),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Xin quyền camera ──────────────────────────────────────────

@Composable
private fun CameraPermissionCard(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Filled.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = CardGreen
            )
            Text(stringResource(R.string.common_camera_permission_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.heart_rate_permission_description),
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CardGreen)
            ) {
                Text(stringResource(R.string.common_grant_camera_permission))
            }
        }
    }
}

// ── Bảng đo đang chạy ─────────────────────────────────────────

@Composable
private fun MeasurementPanel(uiState: HeartRateUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MeasurementRing(
                progress = uiState.progress,
                bpm = uiState.liveBpm,
                secondsToMinimum = uiState.secondsToMinimum,
                beats = uiState.beats
            )

            PpgWaveform(
                values = uiState.waveform,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )

            SignalStrengthBar(strength = uiState.signalStrength)

            Text(
                stringResource(R.string.heart_rate_auto_save_note),
                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MeasurementRing(
    progress: Float,
    bpm: Int?,
    secondsToMinimum: Int,
    beats: Int
) {
    val pulse by rememberInfiniteTransition(label = "nhip-tim")
        .animateFloat(
            initialValue = 1f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900),
                repeatMode = RepeatMode.Reverse
            ),
            label = "nhip-tim-dap"
        )

    Box(modifier = Modifier.size(196.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

            drawArc(
                color = Color.LightGray.copy(alpha = 0.4f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            if (progress > 0f) {
                drawArc(
                    color = HeartRed,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = HeartRed,
                modifier = Modifier.size((22 * pulse).dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = bpm?.toString() ?: "--",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 44.sp
                )
            )
            Text(
                stringResource(R.string.heart_rate_bpm_unit),
                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (secondsToMinimum > 0) {
                    stringResource(R.string.heart_rate_seconds_left, secondsToMinimum)
                } else {
                    stringResource(R.string.heart_rate_beats_seen, beats)
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    color = CardGreen,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

/** Dạng sóng mạch đập gần đây (đã chuẩn hoá `-1..1` ở tầng phân tích). */
@Composable
private fun PpgWaveform(values: List<Float>, modifier: Modifier = Modifier) {
    val lineColor = CardGreen
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas

        val middle = size.height / 2f
        val amplitude = middle * 0.85f
        val stepX = size.width / (values.size - 1)

        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = middle - value.coerceIn(-1f, 1f) * amplitude
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun SignalStrengthBar(strength: Float) {
    // Cùng ngưỡng với `HeartRateUiState.guidance` nên thanh "Tín hiệu" và câu hướng dẫn luôn khớp nhau.
    val weak = strength < HeartRateUiState.MIN_SIGNAL_STRENGTH
    val color = if (weak) Color(0xFFE65100) else CardGreen

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.heart_rate_signal_label), style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
            Text(
                strengthLabel(strength),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { strength.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Color.LightGray.copy(alpha = 0.35f)
        )
    }
}

@Composable
private fun strengthLabel(strength: Float): String = when {
    strength >= HeartRateUiState.GOOD_SIGNAL_STRENGTH -> stringResource(R.string.heart_rate_signal_strong)
    strength >= HeartRateUiState.MIN_SIGNAL_STRENGTH -> stringResource(R.string.heart_rate_signal_medium)
    strength > 0f -> stringResource(R.string.heart_rate_signal_weak)
    else -> stringResource(R.string.heart_rate_signal_dark)
}

// ── Hướng dẫn trong lúc đo ────────────────────────────────────

/**
 * Thẻ hướng dẫn **đổi theo tình trạng đo thật** — đây là phần giúp người dùng biết phải làm gì trong
 * lúc đang đo (đặt ngón tay, ấn vừa, giữ yên, sắp xong).
 */
@Composable
private fun GuidanceCard(guidance: HeartRateGuidance) {
    val (icon, title, description, color) = guidanceVisuals(guidance)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
                )
            }
        }
    }
}

private data class GuidanceVisual(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: Color
)

@Composable
private fun guidanceVisuals(guidance: HeartRateGuidance): GuidanceVisual = when (guidance) {
    HeartRateGuidance.PLACE_FINGER -> GuidanceVisual(
        icon = Icons.Default.TouchApp,
        title = stringResource(R.string.heart_rate_hint_no_finger_title),
        description = stringResource(R.string.heart_rate_hint_no_finger_description),
        color = Color(0xFFE65100)
    )

    HeartRateGuidance.WEAK_SIGNAL -> GuidanceVisual(
        icon = Icons.Default.Warning,
        title = stringResource(R.string.heart_rate_hint_weak_title),
        description = stringResource(R.string.heart_rate_hint_weak_description),
        color = Color(0xFFE65100)
    )

    HeartRateGuidance.ALMOST_DONE -> GuidanceVisual(
        icon = Icons.Default.Favorite,
        title = stringResource(R.string.heart_rate_hint_almost_title),
        description = stringResource(R.string.heart_rate_hint_almost_description),
        color = CardGreen
    )

    HeartRateGuidance.MEASURING, HeartRateGuidance.READY_TO_START -> GuidanceVisual(
        icon = Icons.Default.CheckCircle,
        title = stringResource(R.string.heart_rate_hint_measuring_title),
        description = stringResource(R.string.heart_rate_hint_measuring_description),
        color = CardGreen
    )
}

// ── Kết quả ───────────────────────────────────────────────────

@Composable
private fun ResultSection(
    uiState: HeartRateUiState,
    onRetry: () -> Unit,
    onDone: () -> Unit
) {
    val result = uiState.result
    if (result == null) {
        CannotMeasureCard(onRetry = onRetry, onDone = onDone)
        return
    }

    ResultCard(result = result, isSaving = uiState.isSaving, saved = uiState.saved)

    MeasurementButton(
        label = stringResource(R.string.heart_rate_measure_again),
        icon = Icons.Default.Refresh,
        container = MaterialTheme.colorScheme.surfaceVariant,
        content = MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = onRetry
    )
    MeasurementButton(
        label = stringResource(R.string.common_done),
        icon = Icons.Default.CheckCircle,
        container = CardGreen,
        onClick = onDone
    )
}

@Composable
private fun ResultCard(result: HeartRateResult, isSaving: Boolean, saved: Boolean) {
    val zoneColor = when (result.zone) {
        HeartRateZone.NORMAL -> CardGreen
        HeartRateZone.LOW, HeartRateZone.HIGH -> Color(0xFFE65100)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Favorite, null, tint = HeartRed, modifier = Modifier.size(32.dp))
            Text(
                result.bpm.toString(),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 56.sp
                )
            )
            Text(stringResource(R.string.heart_rate_bpm_unit), style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray))

            Surface(color = zoneColor.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp)) {
                Text(
                    stringResource(R.string.heart_rate_zone_label, result.zone.displayName.lowercase()),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = zoneColor,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Text(
                stringResource(R.string.heart_rate_quality, (result.quality * 100).toInt()),
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when {
                    isSaving -> stringResource(R.string.heart_rate_saving)
                    saved -> stringResource(R.string.heart_rate_saved)
                    else -> stringResource(R.string.heart_rate_not_saved)
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    color = if (saved) CardGreen else Color.Gray,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun CannotMeasureCard(onRetry: () -> Unit, onDone: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Warning, null, tint = Color(0xFFE65100), modifier = Modifier.size(40.dp))
            Text(
                stringResource(R.string.heart_rate_failed_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                stringResource(R.string.heart_rate_failed_description),
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                textAlign = TextAlign.Center
            )
            MeasurementButton(
                label = stringResource(R.string.heart_rate_try_again),
                icon = Icons.Default.Refresh,
                container = HeartRed,
                onClick = onRetry
            )
            TextButton(onClick = onDone) {
                Text(stringResource(R.string.common_back), color = Color.Gray)
            }
        }
    }
}

// ── Thành phần dùng chung ─────────────────────────────────────

@Composable
private fun MeasurementButton(
    label: String,
    icon: ImageVector,
    container: Color,
    onClick: () -> Unit,
    content: Color = Color.White
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
private fun MedicalDisclaimer() {
    Text(
        stringResource(R.string.heart_rate_disclaimer),
        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, textAlign = TextAlign.Center),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Hình minh hoạ vùng camera — dùng khi không có camera thật (preview trong IDE). */
@Composable
private fun CameraPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Color(0xFF1B1B1B)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CameraAlt, null, tint = Color.White.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Camera sau",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

// ── Camera thật (CameraX) ─────────────────────────────────────

/**
 * Mở camera sau, bật đèn flash và đưa **từng khung hình** cho [onSample] dưới dạng giá trị đỏ trung
 * bình (xem [CameraPpgAnalyzer]).
 *
 * Camera chỉ được mở khi [active]; khi màn hình rời đi hoặc [active] chuyển `false`, đèn flash **luôn**
 * được tắt và camera được gỡ khỏi vòng đời (`finally` chạy cả khi coroutine bị huỷ) — không để đèn
 * sáng mãi sau khi rời màn.
 *
 * Khi app ra nền, CameraX tự đóng camera (và tắt đèn); lúc quay lại, đèn được **bật lại** trong
 * `ON_RESUME` để phép đo tiếp tục bình thường (một số thiết bị không tự khôi phục trạng thái đèn).
 */
@Composable
private fun HeartRateCamera(
    active: Boolean,
    onSample: (red: Double, timestampMs: Long) -> Unit,
    onFlashUnitAvailable: (Boolean) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnSample by rememberUpdatedState(onSample)
    val currentOnFlashUnitAvailable by rememberUpdatedState(onFlashUnitAvailable)
    val currentOnError by rememberUpdatedState(onError)

    val analyzer = remember { CameraPpgAnalyzer { red, timestampMs -> currentOnSample(red, timestampMs) } }
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    var openCamera by remember { mutableStateOf<Camera?>(null) }
    var flashAvailable by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (active && flashAvailable) {
            openCamera?.cameraControl?.enableTorch(true)
        }
    }

    LaunchedEffect(active, lifecycleOwner) {
        if (!active) return@LaunchedEffect

        try {
            val provider = context.cameraProvider()

            val preview = CameraPreview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                // Chỉ giữ khung hình mới nhất: phân tích chậm hơn tốc độ khung hình cũng không bị dồn.
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(executor, analyzer) }

            val camera = provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )

            // Chỉ biết chắc máy có đèn flash SAU khi camera được mở.
            val hasFlash = camera.cameraInfo.hasFlashUnit()
            openCamera = camera
            flashAvailable = hasFlash
            currentOnFlashUnitAvailable(hasFlash)
            if (hasFlash) {
                camera.cameraControl.enableTorch(true)
            }

            try {
                awaitCancellation()
            } finally {
                // Tắt đèn trước khi gỡ camera: đèn flash là thứ người dùng nhìn thấy ngay.
                openCamera = null
                if (hasFlash) camera.cameraControl.enableTorch(false)
                provider.unbindAll()
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            currentOnError(e.message ?: "không xác định")
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/**
 * Lấy [ProcessCameraProvider] theo kiểu `suspend` để dùng được trong `LaunchedEffect`.
 *
 * CameraX mở camera **bất đồng bộ** (`ListenableFuture`); chờ sẵn ở đây giúp phần thân của
 * `LaunchedEffect` chỉ còn việc gắn camera và bật đèn.
 */
private suspend fun Context.cameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            {
                try {
                    continuation.resume(future.get())
                } catch (throwable: Throwable) {
                    continuation.resumeWithException(throwable)
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }

// ── Preview trong IDE ─────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun HeartRateIdlePreview() {
    HealthyAITheme {
        HeartRateScreenContent(
            uiState = HeartRateUiState(),
            hasCameraPermission = true,
            onRequestPermission = {},
            onStart = {},
            onStop = {},
            onRetry = {},
            onBack = {},
            onDismissError = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HeartRateMeasuringPreview() {
    HealthyAITheme {
        HeartRateScreenContent(
            uiState = HeartRateUiState(
                phase = HeartRatePhase.MEASURING,
                elapsedMs = 9_400,
                beats = 11,
                liveBpm = 74,
                quality = 0.72f,
                signalStrength = 0.83f,
                fingerDetected = true,
                waveform = List(140) { index ->
                    val z = ((index % 26) / 26f - 0.2f) * 9f
                    (kotlin.math.exp(-(z * z).toDouble()) * 1.6 - 0.6).toFloat()
                }
            ),
            hasCameraPermission = true,
            onRequestPermission = {},
            onStart = {},
            onStop = {},
            onRetry = {},
            onBack = {},
            onDismissError = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HeartRateResultPreview() {
    HealthyAITheme {
        HeartRateScreenContent(
            uiState = HeartRateUiState(
                phase = HeartRatePhase.FINISHED,
                elapsedMs = 15_200,
                beats = 19,
                liveBpm = 76,
                quality = 0.86f,
                signalStrength = 0.91f,
                fingerDetected = true,
                result = HeartRateResult(bpm = 76, quality = 0.86f, zone = HeartRateZone.NORMAL),
                saved = true
            ),
            hasCameraPermission = true,
            onRequestPermission = {},
            onStart = {},
            onStop = {},
            onRetry = {},
            onBack = {},
            onDismissError = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HeartRatePlaceFingerPreview() {
    HealthyAITheme {
        HeartRateScreenContent(
            uiState = HeartRateUiState(
                phase = HeartRatePhase.MEASURING,
                elapsedMs = 3_100,
                liveBpm = null,
                signalStrength = 0.12f,
                fingerDetected = false
            ),
            hasCameraPermission = true,
            onRequestPermission = {},
            onStart = {},
            onStop = {},
            onRetry = {},
            onBack = {},
            onDismissError = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HeartRateCameraPermissionPreview() {
    HealthyAITheme {
        HeartRateScreenContent(
            uiState = HeartRateUiState(),
            hasCameraPermission = false,
            onRequestPermission = {},
            onStart = {},
            onStop = {},
            onRetry = {},
            onBack = {},
            onDismissError = {}
        )
    }
}
