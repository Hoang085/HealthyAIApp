package com.healthyai.app.domain.usecase.health

import com.healthyai.app.domain.model.HealthConnectStatus
import com.healthyai.app.domain.repository.HealthDataRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Test cho use case đọc trạng thái quyền Health Connect. */
class GetHealthConnectPermissionStatusUseCaseTest {

    private val repository = mockk<HealthDataRepository>()
    private val useCase = GetHealthConnectPermissionStatusUseCase(repository)

    @Test
    fun `available health connect reports missing permissions`() = runTest {
        coEvery { repository.getStatus() } returns HealthConnectStatus.AVAILABLE
        coEvery { repository.getRequiredPermissions() } returns REQUIRED
        coEvery { repository.hasAllPermissions() } returns false

        val status = useCase()

        assertEquals(HealthConnectStatus.AVAILABLE, status.status)
        assertEquals(REQUIRED, status.requiredPermissions)
        assertFalse(status.hasAllPermissions)
        assertTrue(status.needsPermission)
    }

    @Test
    fun `granted permissions do not ask for anything`() = runTest {
        coEvery { repository.getStatus() } returns HealthConnectStatus.AVAILABLE
        coEvery { repository.getRequiredPermissions() } returns REQUIRED
        coEvery { repository.hasAllPermissions() } returns true

        val status = useCase()

        assertTrue(status.hasAllPermissions)
        assertFalse(status.needsPermission)
    }

    @Test
    fun `missing health connect skips permission queries`() = runTest {
        coEvery { repository.getStatus() } returns HealthConnectStatus.NOT_INSTALLED

        val status = useCase()

        assertEquals(HealthConnectStatus.NOT_INSTALLED, status.status)
        assertTrue(status.requiredPermissions.isEmpty())
        assertFalse(status.hasAllPermissions)
        assertFalse(status.needsPermission)
        coVerify(exactly = 0) { repository.hasAllPermissions() }
        coVerify(exactly = 0) { repository.getRequiredPermissions() }
    }

    @Test
    fun `read failure is treated as not installed`() = runTest {
        coEvery { repository.getStatus() } throws IllegalStateException("Health Connect bị lỗi")

        val status = useCase()

        assertEquals(HealthConnectStatus.NOT_INSTALLED, status.status)
    }

    private companion object {
        val REQUIRED = setOf(
            "android.permission.health.READ_STEPS",
            "android.permission.health.READ_SLEEP"
        )
    }
}
