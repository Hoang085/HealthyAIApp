package com.healthyai.app.data.healthconnect

import com.healthyai.app.domain.model.HealthConnectStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test cho việc dịch mã trạng thái của Health Connect SDK sang enum của domain —
 * đây là dữ liệu quyết định màn Home hiện thẻ "Cấp quyền" hay "Cài / Cập nhật Health Connect".
 *
 * Dùng số literal thay vì hằng số của `HealthConnectClient` để test chạy được trên JVM
 * (giá trị theo SDK: `SDK_AVAILABLE = 3`, `SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED = 2`,
 * `SDK_UNAVAILABLE = 1`).
 */
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
