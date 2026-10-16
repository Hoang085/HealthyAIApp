package com.healthyai.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Test cho [SleepStageBreakdown] — hai mức chi tiết dữ liệu giấc ngủ từ Health Connect: máy chia pha (Sâu/Nông/REM) và máy chỉ báo tổng thời lượng. */
class SleepStageBreakdownTest {

    @Test
    fun `a device that reports phases exposes both hasStages and hasData`() {
        val breakdown = SleepStageBreakdown(
            deepMinutes = 90,
            lightMinutes = 240,
            remMinutes = 60
        )

        assertEquals(390, breakdown.totalMinutes)
        assertTrue(breakdown.hasStages)
        assertTrue(breakdown.hasData)
    }

    @Test
    fun `a device that only reports total sleep still has data`() {
        // Máy chỉ báo STAGE_TYPE_SLEEPING: không có pha nào nhưng KHÔNG được coi là "không có dữ liệu"
        val breakdown = SleepStageBreakdown(unstagedMinutes = 420)

        assertEquals(0, breakdown.totalMinutes)
        assertFalse("Không có pha Sâu/Nông/REM", breakdown.hasStages)
        assertTrue("Vẫn có dữ liệu ngủ để hiển thị", breakdown.hasData)
    }

    @Test
    fun `an empty breakdown has no data at all`() {
        val breakdown = SleepStageBreakdown()

        assertFalse(breakdown.hasStages)
        assertFalse(breakdown.hasData)
    }
}
