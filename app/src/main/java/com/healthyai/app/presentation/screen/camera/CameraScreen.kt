package com.healthyai.app.presentation.screen.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthyai.app.presentation.ui.theme.HealthyAITheme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    onAddToJournal: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    CameraScreenContent(
        uiState = uiState,
        hasCameraPermission = hasCameraPermission,
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        onAnalyzeBitmap = viewModel::analyzeBitmap,
        onCaptureError = viewModel::onCaptureError,
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
                        "Ghi chép bữa ăn",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp
                        )
                    )
                    Text(
                        "Hướng camera vào món ăn để phân tích.",
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
                        enabled = controller != null && !uiState.isAnalyzing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (uiState.isAnalyzing) "Đang phân tích..." else "Phân tích món ăn",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                uiState.error?.let { message ->
                    item { CameraErrorCard(message) }
                }

                if (uiState.detectedLabels.isNotEmpty()) {
                    item {
                        DetectionResultCard(
                            labels = uiState.detectedLabels,
                            onAddToJournal = onAddToJournal
                        )
                    }
                }
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
                Text("Quét món ăn", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun CameraErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFC62828), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFC62828)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetectionResultCard(
    labels: List<FoodLabel>,
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
                        "Kết quả nhận diện",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Surface(
                    color = Color(0xFFF1F8E9),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Tự động",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                labels.forEach { label ->
                    FoodDetectionChip(label)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedButton(
                onClick = onAddToJournal,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Text("Thêm vào nhật ký", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FoodDetectionChip(label: FoodLabel) {
    Surface(
        color = Color(0xFFC8E6C9),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji placeholder based on label
            val emoji = when (label.text.lowercase()) {
                "salad" -> "🥗"
                "bơ" -> "🥑"
                "trứng luộc" -> "🥚"
                else -> "🍲"
            }
            Text(emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(label.text, fontWeight = FontWeight.Medium, color = Color(0xFF1B5E20))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "${(label.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF1B5E20).copy(alpha = 0.7f)
            )
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
            Text("Cần quyền Camera", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Để nhận diện món ăn, ứng dụng cần truy cập camera của bạn.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Cấp quyền Camera")
            }
        }
    }
}

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
            onAddToJournal = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CameraDetectionResultPreview() {
    HealthyAITheme {
        CameraScreenContent(
            uiState = CameraUiState(
                detectedLabels = listOf(
                    FoodLabel(text = "Salad", confidence = 0.92f),
                    FoodLabel(text = "Rau xanh", confidence = 0.78f)
                )
            ),
            hasCameraPermission = true,
            onRequestPermission = {},
            onAnalyzeBitmap = {},
            onCaptureError = {},
            onAddToJournal = {}
        )
    }
}
