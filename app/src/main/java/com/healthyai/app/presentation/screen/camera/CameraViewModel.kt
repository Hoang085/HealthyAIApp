package com.healthyai.app.presentation.screen.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class FoodLabel(val text: String, val confidence: Float)

data class CameraUiState(
    val detectedLabels: List<FoodLabel> = emptyList(),
    val isAnalyzing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    /** ML Kit Image Labeler — chạy hoàn toàn on-device */
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.65f) // Chỉ lấy kết quả tin cậy > 65%
            .build()
    )

    fun analyzeImage(image: InputImage) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, detectedLabels = emptyList(), error = null) }
            runCatching {
                labeler.process(image).await()
            }.onSuccess { labels ->
                val foodLabels = labels.map { FoodLabel(it.text, it.confidence) }
                _uiState.update { it.copy(detectedLabels = foodLabels, isAnalyzing = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = "Lỗi phân tích: ${e.message}", isAnalyzing = false) }
            }
        }
    }

    fun clearResult() = _uiState.update { CameraUiState() }

    override fun onCleared() {
        super.onCleared()
        labeler.close() // Giải phóng ML Kit resources
    }
}
