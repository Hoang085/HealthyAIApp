package com.healthyai.app.presentation.screen.heartrate

import com.healthyai.app.domain.model.HeartRateZone
import com.healthyai.app.domain.usecase.health.SaveMeasuredHeartRateUseCase
import com.healthyai.app.domain.usecase.heartrate.HeartRateMath
import com.healthyai.app.domain.usecase.heartrate.PpgTestSignal
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Random

/** Unit test cho [HeartRateViewModel]: máy trạng thái của phép đo nhịp tim bằng camera + đèn flash. */
@OptIn(ExperimentalCoroutinesApi::class)
class HeartRateViewModelTest {

    private val saveMeasuredHeartRate = mockk<SaveMeasuredHeartRateUseCase>()

    @Before
    fun givenSavingWorks() {
        coEvery { saveMeasuredHeartRate(any()) } returns Result.success(Unit)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun createViewModel() = HeartRateViewModel(saveMeasuredHeartRate)

    /** Đẩy các khung hình giả lập vào ViewModel như màn hình vẫn làm với từng khung hình camera. */
    private fun HeartRateViewModel.feed(
        durationMs: Long,
        bpm: Double?,
        dc: Double = PpgTestSignal.FINGER_DC,
        amplitude: Double = PpgTestSignal.SYSTOLIC_AMPLITUDE,
        noise: Double = 0.0,
        startMs: Long = PpgTestSignal.BASE_TIMESTAMP_MS,
        random: Random = Random(42)
    ) {
        val frames = PpgTestSignal.frames(
            durationMs = durationMs,
            bpm = bpm,
            dc = dc,
            amplitude = amplitude,
            noise = noise,
            startMs = startMs,
            random = random
        )
        for (frame in frames) {
            onPpgSample(frame.red, frame.timestampMs)
        }
    }

    // Bắt đầu đo

    @Test
    fun `the screen starts idle with the instruction card`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(HeartRatePhase.IDLE, state.phase)
        assertEquals(HeartRateGuidance.READY_TO_START, state.guidance)
        assertNull(state.result)
    }

    @Test
    fun `starting a measurement clears the previous numbers`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()

        viewModel.startMeasurement()

        val state = viewModel.uiState.value
        assertEquals(HeartRatePhase.MEASURING, state.phase)
        assertEquals(0L, state.elapsedMs)
        assertNull(state.liveBpm)
        assertTrue(state.waveform.isEmpty())
    }

    @Test
    fun `frames arriving while idle are ignored`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()

        // Màn hình vẫn bật camera ở chế độ chờ để người dùng nhìn thấy vùng đỏ
        viewModel.feed(durationMs = 5_000, bpm = 72.0)

        val state = viewModel.uiState.value
        assertEquals(HeartRatePhase.IDLE, state.phase)
        assertEquals(0L, state.elapsedMs)
        assertNull(state.liveBpm)
        coVerify(exactly = 0) { saveMeasuredHeartRate(any()) }
    }

    // Đo thành công

    @Test
    fun `a clean pulse finishes the measurement with a result`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()

        viewModel.startMeasurement()
        viewModel.feed(durationMs = 20_000, bpm = 72.0)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(HeartRatePhase.FINISHED, state.phase)
        val result = state.result
        assertNotNull("Tín hiệu sạch phải cho ra kết quả", result)
        assertEquals("Thực tế: ${result!!.bpm}", 72.0, result.bpm.toDouble(), 3.0)
        assertEquals(HeartRateZone.NORMAL, result.zone)
        assertTrue("Chất lượng phải đạt ngưỡng", result.quality >= 0.5f)
        assertNull(state.error)
    }

    @Test
    fun `the result is saved to the health history automatically`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()
        val savedBpm = slot<Int>()
        coEvery { saveMeasuredHeartRate(capture(savedBpm)) } returns Result.success(Unit)

        viewModel.startMeasurement()
        viewModel.feed(durationMs = 20_000, bpm = 72.0)
        advanceUntilIdle()

        coVerify(exactly = 1) { saveMeasuredHeartRate(any()) }
        assertTrue("Nhịp lưu phải quanh 72, thực tế: ${savedBpm.captured}", savedBpm.captured in 69..75)
        val state = viewModel.uiState.value
        assertTrue("Ghi xong thì báo đã lưu", state.saved)
        assertFalse(state.isSaving)
    }

    @Test
    fun `a measurement stops as soon as the result is reliable`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()

        viewModel.startMeasurement()
        // Tín hiệu dài hơn nhiều so với thời gian tối thiểu: phải dừng ngay khi đủ điều kiện
        viewModel.feed(durationMs = 60_000, bpm = 72.0)
        advanceUntilIdle()

        val elapsed = viewModel.uiState.value.elapsedMs
        assertTrue("Phải dừng sớm, không đo hết 60 giây (thực tế ${elapsed}ms)", elapsed < 30_000L)
    }

    @Test
    fun `a fast pulse is classified as high`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()

        viewModel.startMeasurement()
        viewModel.feed(durationMs = 20_000, bpm = 130.0)
        advanceUntilIdle()

        val result = viewModel.uiState.value.result
        assertNotNull(result)
        assertEquals("Thực tế: ${result!!.bpm}", 130.0, result.bpm.toDouble(), 4.0)
        assertEquals(HeartRateZone.HIGH, result.zone)
    }

    // Không đọc được nhịp

    @Test
    fun `noise is refused instead of saved as a wrong number`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()

        viewModel.startMeasurement()
        viewModel.feed(durationMs = 40_000, bpm = null, noise = 1.0)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(HeartRatePhase.FINISHED, state.phase)
        assertNull("Nhiễu thì không được hiện một con số nhịp tim", state.result)
        assertNotNull("Phải nói rõ vì sao không có kết quả", state.error)
        coVerify(exactly = 0) { saveMeasuredHeartRate(any()) }
    }

    @Test
    fun `the measurement gives up after the maximum duration`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()

        viewModel.startMeasurement()
        // Mạch đập yếu hơn hẳn nhiễu: không thể kết luận
        viewModel.feed(durationMs = 40_000, bpm = 72.0, noise = 1.2, random = Random(11))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(HeartRatePhase.FINISHED, state.phase)
        assertTrue(
            "Không được đo quá thời gian tối đa, thực tế: ${state.elapsedMs}ms",
            state.elapsedMs <= HeartRateMath.MAX_MEASURE_MS
        )
    }

    // Dừng giữa chừng

    @Test
    fun `stopping mid measurement saves nothing`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()

        viewModel.startMeasurement()
        viewModel.feed(durationMs = 12_000, bpm = 72.0)
        viewModel.stopMeasurement()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(HeartRatePhase.IDLE, state.phase)
        assertNull(state.result)
        assertEquals(0L, state.elapsedMs)
        coVerify(exactly = 0) { saveMeasuredHeartRate(any()) }
    }

    @Test
    fun `frames after stopping do not restart the measurement`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()

        viewModel.startMeasurement()
        viewModel.stopMeasurement()
        viewModel.feed(durationMs = 20_000, bpm = 72.0)

        assertEquals(HeartRatePhase.IDLE, viewModel.uiState.value.phase)
        assertNull(viewModel.uiState.value.result)
    }

    @Test
    fun `measuring again after a result starts from zero`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()

        viewModel.startMeasurement()
        viewModel.feed(durationMs = 20_000, bpm = 72.0)
        advanceUntilIdle()
        assertEquals(HeartRatePhase.FINISHED, viewModel.uiState.value.phase)

        viewModel.restart()

        val state = viewModel.uiState.value
        assertEquals(HeartRatePhase.MEASURING, state.phase)
        assertNull("Kết quả cũ phải biến mất khi đo lại", state.result)
        assertEquals(0L, state.elapsedMs)
        assertFalse(state.saved)
    }

    // Hướng dẫn cho người dùng

    @Test
    fun `a dark frame asks the user to place the finger`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()

        viewModel.startMeasurement()
        viewModel.feed(durationMs = 3_000, bpm = 72.0, dc = PpgTestSignal.DARK_DC)

        val state = viewModel.uiState.value
        assertFalse(state.fingerDetected)
        assertEquals(HeartRateGuidance.PLACE_FINGER, state.guidance)
    }

    @Test
    fun `a barely moving light asks the user to press gently`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()

        viewModel.startMeasurement()
        // Ngón tay che camera (nền đỏ lớn) nhưng ánh sáng gần như không đổi theo nhịp
        viewModel.feed(durationMs = 3_000, bpm = 72.0, amplitude = 0.01)

        val state = viewModel.uiState.value
        assertTrue("Có ngón tay che camera", state.fingerDetected)
        assertTrue("Tín hiệu yếu, thực tế: ${state.signalStrength}", state.signalStrength < 0.35f)
        assertEquals(HeartRateGuidance.WEAK_SIGNAL, state.guidance)
    }

    @Test
    fun `a clean signal in progress tells the user to keep still`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()

        viewModel.startMeasurement()
        viewModel.feed(durationMs = 5_000, bpm = 72.0)

        val state = viewModel.uiState.value
        assertEquals(HeartRatePhase.MEASURING, state.phase)
        assertEquals(HeartRateGuidance.MEASURING, state.guidance)
        assertNotNull("Đã có nhịp ước lượng để hiện trong lúc đo", state.liveBpm)
        assertTrue("Phải có dạng sóng để vẽ", state.waveform.isNotEmpty())
        assertTrue("Tiến độ vòng tròn phải chạy", state.progress > 0f)
        assertTrue("Còn phải đếm ngược thời gian tối thiểu", state.secondsToMinimum > 0)
    }

    @Test
    fun `the guidance asks to keep holding when the minimum time is passed`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()

        viewModel.startMeasurement()
        // Nhiễu không có chu kỳ: đã quá thời gian tối thiểu mà vẫn chưa chốt được kết quả
        viewModel.feed(durationMs = 16_000, bpm = null, noise = 1.0)

        val state = viewModel.uiState.value
        assertEquals(HeartRatePhase.MEASURING, state.phase)
        assertTrue(
            "Đã quá thời gian tối thiểu, thực tế: ${state.elapsedMs}ms",
            state.elapsedMs >= HeartRateMath.MIN_MEASURE_MS
        )
        assertEquals("Đã đủ thời gian tối thiểu", 0, state.secondsToMinimum)
        assertEquals(HeartRateGuidance.ALMOST_DONE, state.guidance)
    }

    // Lỗi camera & lỗi lưu

    @Test
    fun `a camera failure returns to idle with an explanation`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()

        viewModel.startMeasurement()
        viewModel.onCameraFailed("camera đang bị ứng dụng khác dùng")

        val state = viewModel.uiState.value
        assertEquals(HeartRatePhase.IDLE, state.phase)
        assertNotNull(state.error)
        assertTrue("Thông báo phải nói rõ là lỗi camera", state.error!!.contains("camera"))
    }

    @Test
    fun `dismissing the error keeps the measuring state`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()

        viewModel.onCameraFailed("lỗi thử nghiệm")
        assertNotNull(viewModel.uiState.value.error)

        viewModel.dismissError()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `a device without a flash unit warns that the result may be less accurate`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()

        viewModel.onFlashUnavailable()

        val state = viewModel.uiState.value
        assertEquals("Vẫn cho đo, chỉ cảnh báo", HeartRatePhase.IDLE, state.phase)
        assertNotNull("Phải nói rõ là máy không có đèn flash", state.error)
        assertTrue("Thông báo phải nhắc tới đèn flash", state.error!!.contains("đèn flash"))
    }

    @Test
    fun `the flash warning does not overwrite a real error`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()

        viewModel.onCameraFailed("camera đang bị ứng dụng khác dùng")
        val cameraError = viewModel.uiState.value.error

        viewModel.onFlashUnavailable()

        assertEquals(cameraError, viewModel.uiState.value.error)
    }

    @Test
    fun `a saving failure keeps the result but says it was not stored`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel()
        coEvery { saveMeasuredHeartRate(any()) } returns Result.failure(IllegalStateException("DB hỏng"))

        viewModel.startMeasurement()
        viewModel.feed(durationMs = 20_000, bpm = 72.0)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull("Kết quả đo vẫn phải hiện cho người dùng", state.result)
        assertFalse("Nhưng phải nói rõ là chưa lưu được", state.saved)
        assertNotNull(state.error)
        assertFalse(state.isSaving)
    }
}
