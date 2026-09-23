package com.healthyai.app.presentation.screen.habits.addedit

import androidx.lifecycle.SavedStateHandle
import com.healthyai.app.domain.model.Habit
import com.healthyai.app.domain.model.HabitCategory
import com.healthyai.app.domain.repository.HabitRepository
import com.healthyai.app.domain.usecase.habit.AddHabitUseCase
import com.healthyai.app.domain.usecase.habit.DeleteHabitUseCase
import com.healthyai.app.domain.usecase.habit.UpdateHabitUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
import org.junit.Test

/**
 * Unit test cho luồng xóa thói quen (**xóa mềm**) của màn Sửa thói quen.
 *
 * Trước đây bấm icon thùng rác là xóa ngay (không xác nhận) và ViewModel dùng chung cờ `isSaved`
 * với lưu thói quen. Test này chốt lại: chỉ xóa khi có `habitId`, xóa ở chế độ thêm mới là no-op,
 * xóa xong bật cờ `isDeleted` (để UI hiện phản hồi), và bấm xóa lần hai không gọi lại repository.
 *
 * Ngoài ra test còn chốt định dạng ô **"Mục tiêu hàng ngày"** khi mở màn Sửa: số nguyên hiển thị
 * không có đuôi `.0` (`2000`, không phải `2000.0`) nhưng vẫn giữ phần thập phân khi cần (`7.5`), và
 * **chống trùng tên** (mục 3 #13): tạo/sửa thành tên đã có (không phân biệt hoa/thường, bỏ khoảng
 * trắng thừa) → báo "Tên thói quen đã tồn tại" và **không** ghi vào DB; giữ nguyên tên cũ của chính
 * thói quen đang sửa vẫn hợp lệ. Cuối cùng là **chống double-submit** (phụ lục C #12): bấm lưu 2 lần
 * liên tiếp chỉ ghi DB 1 lần và cờ `isSaving` luôn được nhả ra.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEditHabitViewModelTest {

    private val addHabitUseCase = mockk<AddHabitUseCase>()
    private val updateHabitUseCase = mockk<UpdateHabitUseCase>()
    private val deleteHabitUseCase = mockk<DeleteHabitUseCase>()
    private val repository = mockk<HabitRepository>(relaxed = true)

    private val waterHabit = Habit(
        id = 5L,
        name = "Uống nước",
        category = HabitCategory.WATER,
        targetValue = 2000f,
        unit = "ml"
    )

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `deleteHabit soft deletes the habit and signals completion`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        coEvery { deleteHabitUseCase(5L) } returns Unit
        val viewModel = createViewModel(habitId = 5L)
        advanceUntilIdle()

        viewModel.deleteHabit()
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteHabitUseCase(5L) }
        assertTrue(viewModel.uiState.value.isDeleted)
        assertFalse(viewModel.uiState.value.isSaved) // Cờ lưu phải tách khỏi cờ xóa
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `deleteHabit does nothing in add mode`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel(habitId = null)
        advanceUntilIdle()

        viewModel.deleteHabit()
        advanceUntilIdle()

        coVerify(exactly = 0) { deleteHabitUseCase(any()) }
        assertFalse(viewModel.uiState.value.isDeleted)
        assertFalse(viewModel.uiState.value.isEditMode)
    }

    @Test
    fun `deleteHabit reports the error and does not leave the screen`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        coEvery { deleteHabitUseCase(5L) } throws IllegalStateException("db lỗi")
        val viewModel = createViewModel(habitId = 5L)
        advanceUntilIdle()

        viewModel.deleteHabit()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDeleted)
        val error = viewModel.uiState.value.error
        assertNotNull(error)
        assertTrue(error!!.startsWith("Lỗi khi xóa:"))
    }

    @Test
    fun `second delete request is ignored while the first one is running`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val gate = CompletableDeferred<Unit>()
        coEvery { deleteHabitUseCase(5L) } coAnswers {
            gate.await()
            Unit
        }
        val viewModel = createViewModel(habitId = 5L)
        advanceUntilIdle()

        viewModel.deleteHabit()
        gate.complete(Unit)
        advanceUntilIdle()
        viewModel.deleteHabit() // Đã xóa rồi → không gọi lại
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteHabitUseCase(5L) }
    }

    @Test
    fun `saving the habit still signals save, not delete`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        coEvery { addHabitUseCase(any()) } returns 1L
        val viewModel = createViewModel(habitId = null)
        advanceUntilIdle()

        viewModel.onNameChange("Thiền 15 phút")
        viewModel.onTargetValueChange("15")
        viewModel.saveHabit()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        assertFalse(viewModel.uiState.value.isDeleted)
        coVerify(exactly = 0) { deleteHabitUseCase(any()) }
    }

    // ── Ô "Mục tiêu hàng ngày" hiển thị số sạch (mục 3, #11) ────

    @Test
    fun `editing a habit with a whole target drops the decimal tail`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel(habitId = 5L)
        advanceUntilIdle()

        // 2000f trước đây hiển thị "2000.0" (float thô)
        assertEquals("2000", viewModel.uiState.value.targetValue)
    }

    @Test
    fun `editing a habit with a fractional target keeps the decimals`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val sleepHabit = waterHabit.copy(targetValue = 7.5f, unit = "giờ")

        val viewModel = createViewModel(habitId = 5L, habits = listOf(sleepHabit))
        advanceUntilIdle()

        assertEquals("7.5", viewModel.uiState.value.targetValue)
    }

    @Test
    fun `saving right after opening the edit screen keeps the target value`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        coEvery { updateHabitUseCase(any()) } returns 5L
        val updated = slot<Habit>()
        val viewModel = createViewModel(habitId = 5L)
        advanceUntilIdle()

        // Người dùng không sửa gì, bấm "Cập nhật" ngay
        viewModel.saveHabit()
        advanceUntilIdle()

        coVerify(exactly = 1) { updateHabitUseCase(capture(updated)) }
        assertEquals(2000f, updated.captured.targetValue)
        assertTrue(viewModel.uiState.value.isSaved)
    }

    // ── Chống trùng tên thói quen (mục 3, #13) ────

    @Test
    fun `creating a habit with a name that already exists is rejected`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel(habitId = null)
        advanceUntilIdle()

        viewModel.onNameChange("Uống nước")
        viewModel.onTargetValueChange("500")
        viewModel.saveHabit()
        advanceUntilIdle()

        assertEquals("Tên thói quen đã tồn tại", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaved)
        coVerify(exactly = 0) { addHabitUseCase(any()) }
    }

    @Test
    fun `the duplicate check ignores case and surrounding spaces`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel(habitId = null)
        advanceUntilIdle()

        viewModel.onNameChange("  uống NƯỚC  ")
        viewModel.onTargetValueChange("500")
        viewModel.saveHabit()
        advanceUntilIdle()

        assertEquals("Tên thói quen đã tồn tại", viewModel.uiState.value.error)
        coVerify(exactly = 0) { addHabitUseCase(any()) }
    }

    @Test
    fun `a brand new name is still saved`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        coEvery { addHabitUseCase(any()) } returns 1L
        val viewModel = createViewModel(habitId = null)
        advanceUntilIdle()

        viewModel.onNameChange("Đi bộ buổi tối")
        viewModel.onTargetValueChange("30")
        viewModel.saveHabit()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.isSaved)
        coVerify(exactly = 1) { addHabitUseCase(any()) }
    }

    @Test
    fun `editing a habit without renaming keeps its own name valid`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        coEvery { updateHabitUseCase(any()) } returns 5L
        val viewModel = createViewModel(habitId = 5L) // Tên điền sẵn chính là "Uống nước"
        advanceUntilIdle()

        viewModel.saveHabit()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.isSaved)
        coVerify(exactly = 1) { updateHabitUseCase(any()) }
    }

    @Test
    fun `renaming a habit to another habit name is rejected`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val runHabit = Habit(
            id = 6L,
            name = "Chạy bộ",
            category = HabitCategory.EXERCISE,
            targetValue = 30f,
            unit = "phút"
        )
        val viewModel = createViewModel(habitId = 5L, habits = listOf(waterHabit, runHabit))
        advanceUntilIdle()

        viewModel.onNameChange("Chạy bộ")
        viewModel.saveHabit()
        advanceUntilIdle()

        assertEquals("Tên thói quen đã tồn tại", viewModel.uiState.value.error)
        coVerify(exactly = 0) { updateHabitUseCase(any()) }
    }

    // ── Chống double-submit khi lưu (phụ lục C #12) ────

    @Test
    fun `double tapping save only creates one habit`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val gate = CompletableDeferred<Long>()
        coEvery { addHabitUseCase(any()) } coAnswers {
            gate.await() // Giữ cho lần lưu đầu tiên "đang chạy"
        }
        val viewModel = createViewModel(habitId = null)
        advanceUntilIdle()

        viewModel.onNameChange("Uống nước buổi sáng")
        viewModel.onTargetValueChange("500")

        viewModel.saveHabit() // Bấm lần 1 → bật isSaving
        assertTrue(viewModel.uiState.value.isSaving)
        viewModel.saveHabit() // Bấm lần 2 khi đang lưu → bị bỏ qua

        gate.complete(1L)
        advanceUntilIdle()

        coVerify(exactly = 1) { addHabitUseCase(any()) }
        assertFalse(viewModel.uiState.value.isSaving)
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `the saving flag is released when the name is duplicated`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = createViewModel(habitId = null)
        advanceUntilIdle()

        viewModel.onNameChange("Uống nước") // Trùng với waterHabit
        viewModel.onTargetValueChange("500")
        viewModel.saveHabit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Tên thói quen đã tồn tại", state.error)
        // Không được kẹt ở trạng thái "đang lưu" (finally luôn tắt cờ)
        assertFalse(state.isSaving)
        assertFalse(state.isSaved)
    }

    private fun createViewModel(
        habitId: Long?,
        habits: List<Habit> = listOf(waterHabit)
    ): AddEditHabitViewModel {
        // Màn Sửa đọc thói quen từ danh sách đang hoạt động (DAO đã lọc isActive = 1)
        every { repository.getAllActiveHabits() } returns flowOf(habits)

        val initialState: Map<String, Any?> =
            if (habitId == null) emptyMap() else mapOf("habitId" to habitId)
        return AddEditHabitViewModel(
            addHabitUseCase = addHabitUseCase,
            updateHabitUseCase = updateHabitUseCase,
            deleteHabitUseCase = deleteHabitUseCase,
            repository = repository,
            savedStateHandle = SavedStateHandle(initialState)
        )
    }
}
