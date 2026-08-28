package com.healthyai.app.presentation.screen.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(viewModel: CameraViewModel = hiltViewModel()) {
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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Nhận diện món ăn 🍽️", fontWeight = FontWeight.Bold) })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasCameraPermission) {
                item { PermissionRequestCard { permissionLauncher.launch(Manifest.permission.CAMERA) } }
            } else {
                item {
                    // TODO: Thay bằng CameraX PreviewView thực tế
                    // Tham khảo: https://developer.android.com/training/camerax
                    CameraPreviewPlaceholder()
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { /* TODO: Chụp ảnh từ CameraX và gọi viewModel.analyzeImage(image) */ },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isAnalyzing
                        ) {
                            Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.padding(end = 8.dp))
                            Text(if (uiState.isAnalyzing) "Đang phân tích..." else "Chụp & Phân tích")
                        }
                        OutlinedButton(onClick = viewModel::clearResult) {
                            Icon(Icons.Filled.Clear, null)
                        }
                    }
                }

                if (uiState.isAnalyzing) {
                    item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Text("ML Kit đang nhận diện...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                if (uiState.detectedLabels.isNotEmpty()) {
                    item { DetectionResultCard(uiState.detectedLabels) }
                }

                uiState.error?.let { error ->
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(error, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestCard(onRequestPermission: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Cần quyền Camera", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Để nhận diện món ăn và ước tính thành phần dinh dưỡng, app cần truy cập camera. Dữ liệu xử lý hoàn toàn trên thiết bị (on-device AI).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRequestPermission) { Text("Cấp quyền Camera") }
        }
    }
}

@Composable
private fun CameraPreviewPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth().height(280.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.outline)
                Text("CameraX Preview", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("TODO: Tích hợp AndroidView { PreviewView }", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun DetectionResultCard(labels: List<FoodLabel>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "🤖 Kết quả nhận diện (ML Kit On-device)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            labels.forEach { label ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    val confidence = (label.confidence * 100).toInt()
                    AssistChip(
                        onClick = {},
                        label = { Text("$confidence%", style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
        }
    }
}
