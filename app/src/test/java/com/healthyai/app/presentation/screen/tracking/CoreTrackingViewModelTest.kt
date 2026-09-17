package com.healthyai.app.presentation.screen.tracking

import com.healthyai.app.domain.model.HealthData
import com.healthyai.app.domain.usecase.health.GetHealthDataUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoreTrackingViewModelTest {

    private val getHealthDataUseCase = mockk<GetHealthDataUseCase>()
    private lateinit var viewModel: CoreTrackingViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Mặc định trả về thành công để init không lỗi
        coEvery { getHealthDataUseCase() } returns Result.success(HealthData())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refreshData updates state with success when use case returns data`() = runTest {
        // Arrange
        val expectedData = HealthData(steps = 5000, activeCalories = 200.0)
        coEvery { getHealthDataUseCase() } returns Result.success(expectedData)
        
        viewModel = CoreTrackingViewModel(getHealthDataUseCase)

        // Act
        viewModel.refreshData()

        // Assert
        val state = viewModel.uiState.value
        assertEquals(expectedData, state.healthData)
        assertFalse(state.isRefreshing)
        assertEquals(null, state.error)
    }

    @Test
    fun `refreshData updates state with error when use case fails`() = runTest {
        // Arrange
        val errorMessage = "Lỗi kết nối"
        coEvery { getHealthDataUseCase() } returns Result.failure(Exception(errorMessage))
        
        viewModel = CoreTrackingViewModel(getHealthDataUseCase)

        // Act
        viewModel.refreshData()

        // Assert
        val state = viewModel.uiState.value
        assertEquals(errorMessage, state.error)
        assertFalse(state.isRefreshing)
    }
}
