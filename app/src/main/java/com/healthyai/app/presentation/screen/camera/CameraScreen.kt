package com.healthyai.app.presentation.screen.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthyai.app.R
import com.healthyai.app.data.vision.GalleryImageLoader
import com.healthyai.app.domain.model.FoodInsight
import com.healthyai.app.presentation.ui.theme.HealthyAITheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    onAddToJournal: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    // Photo picker của hệ thống: **không** cần quyền đọc thư viện (Android 13+ dùng picker riêng, máy cũ
    // rơi về `ACTION_OPEN_DOCUMENT`) nên người dùng từ chối quyền Camera vẫn phân tích ảnh được.
    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bitmap = GalleryImageLoader.load(context, uri)
                if (bitmap != null) {
                    viewModel.analyzeBitmap(bitmap)
                } else {
                    viewModel.onImageLoadError("tệp không phải ảnh hoặc đã bị xoá")
                }
            }
        }
        // `uri == null` = người dùng bấm Huỷ → không làm gì, không báo lỗi.
    }

    CameraScreenContent(
        uiState = uiState,
        hasCameraPermission = hasCameraPermission,
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        onAnalyzeBitmap = viewModel::analyzeBitmap,
        onCaptureError = viewModel::onCaptureError,
        onRetry = viewModel::retryLastAnalysis,
        onPickFromGallery = {
            galleryPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onAddToJournal = onAddToJournal
    )
}

@Composable
fun CameraScreenContent(
    uiState: CameraUiState,
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    onAnalyzeBitmap: (Bitmap) -> Unit,
    onCaptureError: (String) -> Unit,
    /** Chạy lại lượt phân tích gần nhất khi lỗi (mạng/AI quá tải). */
    onRetry: () -> Unit,
    /** Bấm "Chọn ảnh từ thư viện" — mở photo picker của hệ thống để phân tích ảnh có sẵn. */
    onPickFromGallery: () -> Unit,
    /** Bấm "Thêm vào nhật ký" ở thẻ kết quả (phụ lục C #22) — mở hộp thoại ghi nhận thói quen dinh dưỡng. */
    onAddToJournal: () -> Unit
) {
    // Executor riêng cho CameraX (chụp ảnh không được chạy trên main thread)
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var captureController by remember { mutableStateOf<LifecycleCameraController?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            captureController = null
            cameraExecutor.shutdown()
        }
    }

    // Hai nút phân tích chỉ bị khoá trong lúc AI đang xử lý (mục 3 #43: không còn công tắc bật/tắt AI).
    val canAnalyze = !uiState.isAnalyzing

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.camera_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp
                        )
                    )
                    Text(
                        stringResource(R.string.camera_subtitle),
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.Gray)
                    )
                }
            }

            if (!hasCameraPermission) {
                item {
                    PermissionRequestCard(onRequestPermission)
                }
            } else {
                item {
                    val previewError = cameraError
                    if (previewError != null) {
                        CameraErrorCard(previewError)
                    } else {
                        CameraPreviewSection(
                            onCameraReady = { captureController = it },
                            onCameraError = { cameraError = it }
                        )
                    }
                }

                item {
                    val controller = captureController
                    Button(
                        onClick = {
                            controller?.takePicture(
                                cameraExecutor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        // ImageProxy chỉ hợp lệ trong callback → chuyển sang Bitmap ngay
                                        val bitmap = try {
                                            image.toBitmap()
                                        } finally {
                                            image.close()
                                        }
                                        onAnalyzeBitmap(bitmap)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        onCaptureError(exception.message ?: "Lỗi không xác định")
                                    }
                                }
                            )
                        },
                        enabled = controller != null && canAnalyze,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (uiState.isAnalyzing) stringResource(R.string.camera_analyzing) else stringResource(R.string.camera_analyze),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            // Ảnh có sẵn trong thư viện: **không** cần quyền Camera nên nút này nằm ngoài nhánh quyền —
            // ai không muốn cấp quyền camera vẫn phân tích được ảnh đã chụp trước đó.
            item {
                OutlinedButton(
                    onClick = onPickFromGallery,
                    enabled = canAnalyze,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, Color(0xFF2E7D32))
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.camera_pick_image),
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            uiState.error?.let { message ->
                item {
                    CameraErrorCard(
                        message = message,
                        // Có ảnh của lượt trước thì cho thử lại ngay, không phải chụp/chọn lại.
                        onRetry = onRetry.takeIf { uiState.canRetry }
                    )
                }
            }

            uiState.insight?.let { insight ->
                item {
                    FoodInsightCard(
                        insight = insight,
                        onAddToJournal = onAddToJournal
                    )
                }
            }

            if (uiState.noFoodDetected) {
                item { NoFoodDetectedCard() }
            }
        }
    }

    // Hộp thoại nằm NGOÀI `Scaffold` (dialog là một cửa sổ riêng): đặt ở đây thì không ảnh hưởng bố cục
    // của danh sách phía trên và không phải lồng thêm layout.
    if (uiState.isAnalyzing) {
        AnalyzingDialog()
    }
}

/**
 * Hộp thoại **"Đang phân tích món ăn"** (mục 3 #43).
 *
 * Trước đây lượt phân tích chỉ đổi nhãn nút sang "Đang phân tích…" — mà nút nằm giữa một màn dài, người
 * dùng bấm xong không biết app có đang làm gì hay bị "treo". Hộp thoại này cho phản hồi rõ ràng: AI đọc
 * ảnh mất vài giây nên khoá luôn cả màn hình để tránh bấm chồng nhiều lượt.
 *
 * **Không** cho tắt bằng nút Back hay chạm ra ngoài: lượt gọi AI vẫn đang chạy và kết quả sẽ hiện ngay
 * khi xong, nên "đóng" hộp thoại chỉ gây hiểu nhầm là đã huỷ.
 */
@Composable
private fun AnalyzingDialog() {
    Dialog(
        onDismissRequest = { /* không tắt được — xem KDoc */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = Color(0xFF2E7D32))
                Text(
                    stringResource(R.string.camera_analyzing_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(R.string.camera_analyzing_message),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Preview camera thật bằng CameraX (`LifecycleCameraController` tự quản lý vòng đời camera),
 * kèm khung quét món ăn. Controller được trả ra ngoài để nút "Phân tích món ăn" chụp ảnh.
 */
@Composable
private fun CameraPreviewSection(
    onCameraReady: (LifecycleCameraController) -> Unit,
    onCameraError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        }
    }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            controller = cameraController
        }
    }

    LaunchedEffect(cameraController, lifecycleOwner) {
        try {
            cameraController.bindToLifecycle(lifecycleOwner)
            onCameraReady(cameraController)
        } catch (e: CancellationException) {
            // Rời màn/huỷ composition: không phải lỗi camera → ném tiếp để LaunchedEffect kết thúc đúng
            // thay vì hiện thẻ lỗi "Không mở được camera" (phụ lục C #39).
            throw e
        } catch (e: Exception) {
            onCameraError("Không mở được camera: ${e.message}")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .clip(RoundedCornerShape(48.dp))
            .background(Color.LightGray)
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Scan Overlay Frame
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.Center)
                .border(4.dp, Color.White, RoundedCornerShape(32.dp))
        )

        // Scan Food Button at bottom of preview
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FilterCenterFocus, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.camera_scan_frame), color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Thẻ lỗi; khi có ảnh của lượt trước thì kèm nút **"Thử lại"**. */
@Composable
private fun CameraErrorCard(message: String, onRetry: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFC62828), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    message,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFC62828)
                )
            }

            if (onRetry != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828))
                ) {
                    Text(stringResource(R.string.camera_error_retry), color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Thẻ **kết quả phân tích món ăn**: tên món (do AI đọc từ ảnh) + "Có lợi cho cơ thể" + "Nên lưu ý"
 * + dòng miễn trừ trách nhiệm.
 *
 * Dòng miễn trừ là **bắt buộc**: nội dung do model sinh ra là claim về sức khỏe, phải nói rõ đây là
 * thông tin tham khảo chứ không phải tư vấn y tế.
 */
@Composable
private fun FoodInsightCard(
    insight: FoodInsight,
    onAddToJournal: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LunchDining, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.camera_result_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Surface(color = Color(0xFFF1F8E9), shape = RoundedCornerShape(12.dp)) {
                    Text(
                        stringResource(R.string.camera_result_ai_badge),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(insight.emoji, fontSize = 32.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    insight.dishName,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (insight.benefits.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                InsightSection(
                    title = stringResource(R.string.camera_result_benefits),
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF2E7D32),
                    bullets = insight.benefits
                )
            }

            if (insight.cautions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                InsightSection(
                    title = stringResource(R.string.camera_result_cautions),
                    icon = Icons.Default.WarningAmber,
                    color = Color(0xFFE65100),
                    bullets = insight.cautions
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(R.string.camera_result_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                stringResource(R.string.camera_result_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onAddToJournal,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Text(stringResource(R.string.camera_add_to_journal), color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Một mục "Có lợi cho cơ thể" / "Nên lưu ý": tiêu đề có icon + các gạch đầu dòng. */
@Composable
private fun InsightSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    bullets: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(color = color, fontWeight = FontWeight.Bold)
            )
        }
        bullets.forEach { bullet ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.6f))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    bullet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Thẻ báo **AI không nhận ra món ăn nào** trong ảnh (hoặc không gọi được tên món).
 *
 * Cần thiết vì trước đây màn này luôn hiển thị "Kết quả nhận diện" kể cả khi ảnh không có đồ ăn
 * (phụ lục C #21) — cùng tinh thần: không có gì đáng nói thì nói thẳng, kèm gợi ý chụp lại.
 */
@Composable
private fun NoFoodDetectedCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.NoFood,
                null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.camera_no_food_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.camera_no_food_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun PermissionRequestCard(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(64.dp), tint = Color(0xFF2E7D32))
            Text(stringResource(R.string.common_camera_permission_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.camera_permission_description),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.camera_permission_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(stringResource(R.string.common_grant_camera_permission))
            }
        }
    }
}

/** Ảnh mẫu dùng chung cho các `@Preview` bên dưới. */
private val PREVIEW_INSIGHT = FoodInsight(
    isFood = true,
    dishName = "Cơm tấm sườn",
    emoji = "🍚",
    benefits = listOf(
        "Cung cấp năng lượng từ gạo và đạm từ thịt, giúp no lâu.",
        "Kèm đồ chua/rau giúp tăng chất xơ và dễ tiêu hơn."
    ),
    cautions = listOf(
        "Sườn nướng nhiều dầu mỡ và muối — không nên ăn thường xuyên.",
        "Khẩu phần cơm khá lớn: người tiểu đường nên chia nhỏ."
    )
)

@Preview(showBackground = true)
@Composable
fun CameraScreenPreview() {
    HealthyAITheme {
        CameraScreenContent(
            uiState = CameraUiState(),
            // Preview của IDE không chạy được CameraX thật → hiển thị luồng xin quyền
            hasCameraPermission = false,
            onRequestPermission = {},
            onAnalyzeBitmap = {},
            onCaptureError = {},
            onRetry = {},
            onPickFromGallery = {},
            onAddToJournal = {}
        )
    }
}

/** Trạng thái đang chờ AI: hộp thoại "Đang phân tích món ăn" (mục 3 #43). */
@Preview(showBackground = true)
@Composable
fun CameraAnalyzingPreview() {
    HealthyAITheme {
        CameraScreenContent(
            uiState = CameraUiState(isAnalyzing = true),
            hasCameraPermission = true,
            onRequestPermission = {},
            onAnalyzeBitmap = {},
            onCaptureError = {},
            onRetry = {},
            onPickFromGallery = {},
            onAddToJournal = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CameraDetectionResultPreview() {
    HealthyAITheme {
        CameraScreenContent(
            uiState = CameraUiState(insight = PREVIEW_INSIGHT),
            hasCameraPermission = true,
            onRequestPermission = {},
            onAnalyzeBitmap = {},
            onCaptureError = {},
            onRetry = {},
            onPickFromGallery = {},
            onAddToJournal = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CameraNoFoodDetectedPreview() {
    HealthyAITheme {
        CameraScreenContent(
            uiState = CameraUiState(noFoodDetected = true),
            hasCameraPermission = true,
            onRequestPermission = {},
            onAnalyzeBitmap = {},
            onCaptureError = {},
            onRetry = {},
            onPickFromGallery = {},
            onAddToJournal = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CameraErrorPreview() {
    HealthyAITheme {
        CameraScreenContent(
            uiState = CameraUiState(
                error = "AI đang quá tải. Vui lòng thử lại sau vài giây.",
                canRetry = true
            ),
            hasCameraPermission = true,
            onRequestPermission = {},
            onAnalyzeBitmap = {},
            onCaptureError = {},
            onRetry = {},
            onPickFromGallery = {},
            onAddToJournal = {}
        )
    }
}
