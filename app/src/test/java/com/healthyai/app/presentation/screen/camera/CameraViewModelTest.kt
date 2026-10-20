package com.healthyai.app.presentation.screen.camera

import com.healthyai.app.data.vision.FoodAnalysisError
import com.healthyai.app.data.vision.FoodAnalysisResult
import com.healthyai.app.data.vision.FoodInsightSource
import com.healthyai.app.domain.model.FoodInsight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Unit test cho [CameraViewModel]: máy trạng thái của lượt "Phân tích món ăn". */
@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    /** Giả lập nguồn AI: trả đúng [result] đã cài, và ghi lại ảnh nhận được. */
    private class FakeSource(var result: FoodAnalysisResult) : FoodInsightSource {
        var calls = 0
            private set
        var lastJpeg: ByteArray? = null
            private set

        override suspend fun analyze(jpegBytes: ByteArray): FoodAnalysisResult {
            calls++
            lastJpeg = jpegBytes
            return result
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Tạo ViewModel (không cần chờ gì: không còn thiết lập opt-in phải đọc từ "DB"). */
    private fun TestScope.createViewModel(source: FoodInsightSource): CameraViewModel =
        CameraViewModel(source).also { advanceUntilIdle() }

    private fun insight(
        dishName: String = "Cơm tấm sườn",
        benefits: List<String> = listOf("Nhiều năng lượng từ cơm và đạm"),
        cautions: List<String> = listOf("Sườn nướng nhiều dầu mỡ")
    ) = FoodInsight(
        isFood = true,
        dishName = dishName,
        emoji = "🍚",
        benefits = benefits,
        cautions = cautions
    )

    private fun jpeg() = byteArrayOf(1, 2, 3)

    // Đường thành công

    @Test
    fun `phan tich anh mon an hien ten mon tieng Viet kem loi ich va dieu nen luu y`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeSource(FoodAnalysisResult.Success(insight())))

        viewModel.runAnalysis { jpeg() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Cơm tấm sườn", state.insight?.dishName)
        assertEquals(listOf("Nhiều năng lượng từ cơm và đạm"), state.insight?.benefits)
        assertEquals(listOf("Sườn nướng nhiều dầu mỡ"), state.insight?.cautions)
        assertFalse(state.noFoodDetected)
        assertFalse(state.isAnalyzing)
        assertNull(state.error)
    }

    @Test
    fun `dang phan tich thi nut bi khoa ngay lap tuc`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeSource(FoodAnalysisResult.Success(insight())))

        viewModel.runAnalysis { jpeg() }

        // Trạng thái được đặt trước khi coroutine chạy để nút đổi sang "Đang phân tích…" tức thì.
        assertTrue(viewModel.uiState.value.isAnalyzing)
        assertNull(viewModel.uiState.value.insight)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAnalyzing)
        assertEquals("Cơm tấm sườn", viewModel.uiState.value.insight?.dishName)
    }

    // Không còn cổng đồng ý

    @Test
    fun `bam phan tich la goi AI luon khong con cong tac nao chan`() = runTest(dispatcher) {
        val source = FakeSource(FoodAnalysisResult.Success(insight()))
        val viewModel = createViewModel(source)

        viewModel.runAnalysis { jpeg() }
        advanceUntilIdle()

        assertEquals(
            "Màn Ghi chép đã bỏ công tắc 'Phân tích món ăn bằng AI' nên không còn gì chặn lượt gọi",
            1,
            source.calls
        )
        assertEquals("Cơm tấm sườn", viewModel.uiState.value.insight?.dishName)
    }

    // Ảnh không có đồ ăn

    @Test
    fun `anh khong phai do an thi bao chua nhan dien duoc chu khong hien ket qua gia`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeSource(FoodAnalysisResult.Success(FoodInsight())))

        viewModel.runAnalysis { jpeg() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.insight)
        assertTrue("Ảnh không có món ăn phải báo cho người dùng biết", state.noFoodDetected)
        assertNull(state.error)
    }

    @Test
    fun `AI noi co do an nhung khong neu ten mon thi coi nhu chua nhan dien duoc`() = runTest(dispatcher) {
        val namedButBlank = FoodInsight(isFood = true, dishName = "   ", benefits = listOf("a"))
        val viewModel = createViewModel(FakeSource(FoodAnalysisResult.Success(namedButBlank)))

        viewModel.runAnalysis { jpeg() }
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.insight)
        assertTrue(viewModel.uiState.value.noFoodDetected)
    }

    // Lỗi: mỗi loại một câu, loại thử lại được thì có nút

    @Test
    fun `mat mang thi bao ro va cho thu lai`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeSource(FoodAnalysisResult.Failure(FoodAnalysisError.NETWORK)))

        viewModel.runAnalysis { jpeg() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Không kết nối được tới AI. Kiểm tra mạng rồi thử lại.", state.error)
        assertTrue("Đã có ảnh của lượt này ⇒ thử lại được ngay", state.canRetry)
        assertFalse(state.isAnalyzing)
    }

    @Test
    fun `AI qua tai thi bao ro la qua tai`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeSource(FoodAnalysisResult.Failure(FoodAnalysisError.BUSY)))

        viewModel.runAnalysis { jpeg() }
        advanceUntilIdle()

        assertEquals("AI đang quá tải. Vui lòng thử lại sau vài giây.", viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.canRetry)
    }

    @Test
    fun `chua cau hinh khoa AI thi bao ro`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeSource(FoodAnalysisResult.Failure(FoodAnalysisError.NOT_CONFIGURED)))

        viewModel.runAnalysis { jpeg() }
        advanceUntilIdle()

        assertTrue(
            "Máy chưa cấu hình khoá phải được nói rõ, không phải lỗi chung chung",
            viewModel.uiState.value.error!!.contains("khoá AI")
        )
    }

    @Test
    fun `khoa AI sai thi bao ro`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeSource(FoodAnalysisResult.Failure(FoodAnalysisError.AUTH)))

        viewModel.runAnalysis { jpeg() }
        advanceUntilIdle()

        assertEquals("Khoá AI không hợp lệ hoặc đã bị thu hồi.", viewModel.uiState.value.error)
    }

    @Test
    fun `thu lai chay lai dung anh cua luot truoc`() = runTest(dispatcher) {
        val source = FakeSource(FoodAnalysisResult.Failure(FoodAnalysisError.BUSY))
        val viewModel = createViewModel(source)

        viewModel.runAnalysis { jpeg() }
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canRetry)

        // Mạng/AI đã ổn trở lại.
        source.result = FoodAnalysisResult.Success(insight())
        viewModel.retryLastAnalysis()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, source.calls)
        assertArrayEquals("Gửi lại đúng ảnh cũ, không cần chụp lại", jpeg(), source.lastJpeg)
        assertEquals("Cơm tấm sườn", state.insight?.dishName)
        assertNull(state.error)
        assertFalse(state.canRetry)
    }

    @Test
    fun `luot phan tich moi xoa ket qua va loi cua luot truoc`() = runTest(dispatcher) {
        val source = FakeSource(FoodAnalysisResult.Success(insight(dishName = "Phở bò")))
        val viewModel = createViewModel(source)

        viewModel.runAnalysis { jpeg() }
        advanceUntilIdle()
        assertEquals("Phở bò", viewModel.uiState.value.insight?.dishName)

        // Lượt 2 lỗi: kết quả cũ phải biến mất, chỉ còn thẻ lỗi.
        source.result = FoodAnalysisResult.Failure(FoodAnalysisError.BAD_RESPONSE)
        viewModel.runAnalysis { jpeg() }
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.insight)
        assertEquals("AI trả về kết quả không đọc được. Vui lòng thử lại.", viewModel.uiState.value.error)

        // Lượt 3 thành công: thẻ lỗi tự xoá.
        source.result = FoodAnalysisResult.Success(insight(dishName = "Bún chả"))
        viewModel.runAnalysis { jpeg() }
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.error)
        assertEquals("Bún chả", viewModel.uiState.value.insight?.dishName)
    }

    @Test
    fun `loi phan tich khong lam mat man hinh`() = runTest(dispatcher) {
        val viewModel = createViewModel(FailingSource())

        viewModel.runAnalysis { jpeg() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Lỗi phân tích món ăn: Gemini hỏng", state.error)
        assertFalse(state.isAnalyzing)
        assertFalse(state.noFoodDetected)
    }

    @Test
    fun `loi chup anh duoc hien thanh the loi`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeSource(FoodAnalysisResult.Success(insight())))

        viewModel.onCaptureError("Camera bận")

        val state = viewModel.uiState.value
        assertEquals("Không chụp được ảnh: Camera bận", state.error)
        assertFalse(state.isAnalyzing)
    }

    @Test
    fun `loi doc anh tu thu vien duoc hien thanh the loi`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeSource(FoodAnalysisResult.Success(insight())))

        viewModel.onImageLoadError("tệp không phải ảnh hoặc đã bị xoá")

        val state = viewModel.uiState.value
        assertEquals("Không đọc được ảnh đã chọn: tệp không phải ảnh hoặc đã bị xoá", state.error)
        assertFalse(state.isAnalyzing)
    }

    @Test
    fun `clearResult xoa het trang thai cua luot truoc`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeSource(FoodAnalysisResult.Success(FoodInsight())))

        viewModel.runAnalysis { jpeg() }
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.noFoodDetected)

        viewModel.clearResult()

        assertEquals(
            "Xoá kết quả phải đưa màn hình về trạng thái đầu, không giữ lại gì của lượt trước",
            CameraUiState(),
            viewModel.uiState.value
        )
    }

    /** Nguồn AI ném lỗi ngoài dự kiến (ví dụ lỗi lập trình) để kiểm tra nhánh `catch` chung. */
    private class FailingSource : FoodInsightSource {
        override suspend fun analyze(jpegBytes: ByteArray): FoodAnalysisResult =
            throw IllegalStateException("Gemini hỏng")
    }
}
