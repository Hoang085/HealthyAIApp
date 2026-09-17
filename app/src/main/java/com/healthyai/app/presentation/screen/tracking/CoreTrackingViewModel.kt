package com.healthyai.app.presentation.screen.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.usecase.health.GetHealthDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State cho màn hình Core Tracking.
 */
data class TrackingUiState(
    val healthData: HealthData = HealthData(),
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * ViewModel quản lý trạng thái dữ liệu sức khỏe cốt lõi (Core Tracking).
 * Sử dụng StateFlow để tích hợp mượt mà với Jetpack Compose.
 */
@HiltViewModel
class CoreTrackingViewModel @Inject constructor(
    private val getHealthDataUseCase: GetHealthDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    init {
        refreshData()
    }

    /**
     * Làm mới dữ liệu từ Health Connect.
     */
    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            
            getHealthDataUseCase()
                .onSuccess { data ->
                    _uiState.update { 
                        it.copy(
                            healthData = data,
                            isRefreshing = false,
                            lastUpdated = System.currentTimeMillis(),
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { 
                        it.copy(
                            isRefreshing = false,
                            error = e.message ?: "Lỗi không xác định khi lấy dữ liệu"
                        )
                    }
                }
        }
    }

    /**
     * Xóa thông báo lỗi.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
