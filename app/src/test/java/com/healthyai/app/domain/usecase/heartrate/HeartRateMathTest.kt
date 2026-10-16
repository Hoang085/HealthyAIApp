package com.healthyai.app.domain.usecase.heartrate

import com.healthyai.app.domain.model.HeartRateZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit test cho [HeartRateMath] — phép đo nhịp tim bằng camera + đèn flash; mọi giá trị tính tay. */
class HeartRateMathTest {

    // Ngưỡng phải khớp với nhau

    @Test
    fun `the interval limits match the bpm limits they describe`() {
        assertEquals(
            "Khoảng cách ngắn nhất tương ứng nhịp nhanh nhất",
            60_000.0 / HeartRateMath.MAX_BPM,
            HeartRateMath.MIN_INTERVAL_MS.toDouble(),
            0.001
        )
        assertEquals(
            "Khoảng cách dài nhất tương ứng nhịp chậm nhất",
            60_000.0 / HeartRateMath.MIN_BPM,
            HeartRateMath.MAX_INTERVAL_MS.toDouble(),
            0.001
        )
    }

    @Test
    fun `only intervals inside the plausible range are valid`() {
        assertFalse(HeartRateMath.isValidInterval(250.0))   // 240 bpm → quá nhanh
        assertTrue(HeartRateMath.isValidInterval(300.0))    // đúng 200 bpm
        assertTrue(HeartRateMath.isValidInterval(1_000.0))  // 60 bpm
        assertTrue(HeartRateMath.isValidInterval(1_500.0))  // đúng 40 bpm
        assertFalse(HeartRateMath.isValidInterval(1_600.0)) // 37 bpm → quá chậm
    }

    // Trung vị và độ tản

    @Test
    fun `the bpm of an interval is 60 000 divided by its length`() {
        assertEquals(60.0, HeartRateMath.bpmOf(1_000.0), 0.001)
        assertEquals(75.0, HeartRateMath.bpmOf(800.0), 0.001)
    }

    @Test
    fun `median handles odd and even counts`() {
        // 800, 900, 1000 → phần tử giữa là 900
        assertEquals(900.0, HeartRateMath.median(listOf(900.0, 800.0, 1_000.0)), 0.001)
        assertEquals(850.0, HeartRateMath.median(listOf(900.0, 800.0)), 0.001)
        assertEquals(0.0, HeartRateMath.median(emptyList()), 0.001)
    }

    @Test
    fun `median absolute deviation ignores one wild interval better than a mean would`() {
        val intervals = listOf(800.0, 810.0, 790.0, 805.0, 1_500.0)

        // Trung vị vẫn là 805 (ngoại lệ 1 500 không kéo được trung vị)
        assertEquals(805.0, HeartRateMath.median(intervals), 0.001)
        // Còn độ lệch tuyệt đối trung vị chỉ ra rằng 4/5 khoảng rất đều nhau (lệch nhiều nhất 15 ms)
        assertEquals(5.0, HeartRateMath.medianAbsoluteDeviation(intervals), 0.001)
    }

    // Độ đều của nhịp

    @Test
    fun `perfectly even heartbeats have maximum regularity`() {
        val intervals = List(HeartRateMath.MIN_INTERVALS) { 800.0 }
        assertEquals(1f, HeartRateMath.intervalRegularity(intervals), 0.001f)
    }

    @Test
    fun `regularity needs enough intervals before it says anything`() {
        // Chỉ 4 khoảng: chưa đủ cơ sở để nói về độ đều (trả 0 chứ không phải "rất đều")
        assertEquals(0f, HeartRateMath.intervalRegularity(List(4) { 800.0 }), 0.001f)
    }

    @Test
    fun `heartbeats jittering more than the allowed dispersion score zero regularity`() {
        // Lệch 20% so với trung vị: quá ngưỡng MAX_DISPERSION (15%)
        val intervals = listOf(640.0, 960.0, 640.0, 960.0, 800.0)
        assertEquals(0f, HeartRateMath.intervalRegularity(intervals), 0.001f)
    }

    @Test
    fun `mild natural jitter still scores as an even rhythm`() {
        // Nhịp nghỉ ngơi bình thường vẫn có dao động vài phần trăm
        val intervals = listOf(800.0, 815.0, 790.0, 805.0, 800.0)
        assertTrue(HeartRateMath.intervalRegularity(intervals) > 0.7f)
    }

    // Độ mạnh tín hiệu

    @Test
    fun `signal strength is a ratio so a brighter image does not change it`() {
        // AC/DC = 1% ở hai mức sáng khác nhau → cùng điểm
        val dim = HeartRateMath.signalStrength(acPeakToPeak = 0.5, dc = 50.0)
        val bright = HeartRateMath.signalStrength(acPeakToPeak = 2.0, dc = 200.0)
        assertEquals(dim, bright, 0.001f)
        assertEquals(1f, bright, 0.001f)
    }

    @Test
    fun `signal strength is zero when there is no light at all`() {
        assertEquals(0f, HeartRateMath.signalStrength(1.0, 0.0), 0.001f)
        assertEquals(0f, HeartRateMath.signalStrength(0.0, 200.0), 0.001f)
    }

    @Test
    fun `a very weak but non zero signal scores low`() {
        // AC/DC = 0,03% → thấp hơn mức "tín hiệu yếu"
        val weak = HeartRateMath.signalStrength(acPeakToPeak = 0.07, dc = 200.0)
        assertTrue("Phải nằm dưới mức tín hiệu mạnh", weak < 0.5f)
    }

    // Nội suy đỉnh

    @Test
    fun `a symmetric peak needs no correction`() {
        assertEquals(0.0, HeartRateMath.parabolicPeakOffset(5.0, 10.0, 5.0), 0.0001)
    }

    @Test
    fun `a peak leaning to the right is pushed to the right`() {
        // Đỉnh thật nằm giữa mẫu đỉnh và mẫu sau (mẫu sau lớn hơn mẫu trước)
        val offset = HeartRateMath.parabolicPeakOffset(4.0, 10.0, 8.0)
        assertTrue("Đỉnh thật lệch sang phải → offset dương", offset > 0.0)
    }

    @Test
    fun `a peak leaning to the left is pushed to the left`() {
        val offset = HeartRateMath.parabolicPeakOffset(8.0, 10.0, 4.0)
        assertTrue("Đỉnh thật lệch sang trái → offset âm", offset < 0.0)
    }

    @Test
    fun `the correction stays inside half a sample on each side`() {
        // Đỉnh thật luôn nằm trong khoảng giữa hai mẫu kề đỉnh → offset trong (-1, 1)
        assertEquals(0.5, HeartRateMath.parabolicPeakOffset(0.0, 10.0, 10.0), 0.0001)
        assertEquals(-0.5, HeartRateMath.parabolicPeakOffset(10.0, 10.0, 0.0), 0.0001)
        // Ba mẫu bằng nhau (không có đỉnh) → không hiệu chỉnh gì
        assertEquals(0.0, HeartRateMath.parabolicPeakOffset(5.0, 5.0, 5.0), 0.0001)
        // Mẫu giữa không phải đỉnh (A<V) → vẫn bị chặn trong ±1, không khuếch đại bậy
        val offset = HeartRateMath.parabolicPeakOffset(100.0, 1.0, 0.0)
        assertTrue(offset >= -1.0 && offset <= 1.0)
    }

    // Đổi YUV → kênh đỏ

    @Test
    fun `a neutral chroma means the red channel equals the brightness`() {
        assertEquals(150.0, HeartRateMath.redFromYuvMeans(luma = 150.0, chromaV = 128.0), 0.001)
    }

    @Test
    fun `a red tint raises the red channel above the brightness`() {
        // V = 180 (ngả đỏ) → R = 150 + 1,402·(180 − 128) = 222,9
        assertEquals(222.9, HeartRateMath.redFromYuvMeans(luma = 150.0, chromaV = 180.0), 0.01)
    }

    @Test
    fun `a blue tint lowers the red channel below the brightness`() {
        assertTrue(HeartRateMath.redFromYuvMeans(luma = 150.0, chromaV = 100.0) < 150.0)
    }

    // Phân loại nhịp nghỉ

    @Test
    fun `resting zones follow the usual 60 to 100 bpm range`() {
        assertEquals(HeartRateZone.LOW, HeartRateMath.zoneOf(59))
        assertEquals(HeartRateZone.NORMAL, HeartRateMath.zoneOf(60))
        assertEquals(HeartRateZone.NORMAL, HeartRateMath.zoneOf(72))
        assertEquals(HeartRateZone.NORMAL, HeartRateMath.zoneOf(100))
        assertEquals(HeartRateZone.HIGH, HeartRateMath.zoneOf(101))
    }
}
