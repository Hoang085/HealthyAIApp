package com.healthyai.app.data.healthconnect

import com.healthyai.app.domain.model.HealthConnectStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/** Test dịch mã trạng thái Health Connect sang enum domain; dùng số literal để chạy trên JVM (3=available, 2=update required, 1=unavailable). */
class HealthConnectStatusMappingTest {

    @Test
    fun `sdk available maps to AVAILABLE`() {
        assertEquals(HealthConnectStatus.AVAILABLE, sdkStatusToHealthConnectStatus(3))
    }

    @Test
    fun `sdk provider update required maps to UPDATE_REQUIRED`() {
        assertEquals(
            HealthConnectStatus.UPDATE_REQUIRED,
            sdkStatusToHealthConnectStatus(2)
        )
    }

    @Test
    fun `sdk unavailable maps to NOT_INSTALLED`() {
        assertEquals(HealthConnectStatus.NOT_INSTALLED, sdkStatusToHealthConnectStatus(1))
    }

    @Test
    fun `unknown sdk status is treated as not installed`() {
        assertEquals(HealthConnectStatus.NOT_INSTALLED, sdkStatusToHealthConnectStatus(99))
    }
}
