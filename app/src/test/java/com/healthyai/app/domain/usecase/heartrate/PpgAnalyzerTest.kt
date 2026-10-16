package com.healthyai.app.domain.usecase.heartrate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.abs

/** Unit test cho [PpgAnalyzer] — biến dòng khung hình camera thành nhịp tim; tín hiệu mạch đập giả lập (xem [PpgTestSignal]). */
class PpgAnalyzerTest {

    /** Phát khung hình trong [durationMs] vào [analyzer] và trả về tình trạng đo ở khung hình cuối; `bpm = null` = chỉ nhiễu. */
    private fun feed(
        analyzer: PpgAnalyzer = PpgAnalyzer(),
        durationMs: Long,
        bpm: Double?,
        dc: Double = PpgTestSignal.FINGER_DC,
        amplitude: Double = PpgTestSignal.SYSTOLIC_AMPLITUDE,
        noise: Double = 0.0,
        startMs: Long = PpgTestSignal.BASE_TIMESTAMP_MS,
        random: Random = Random(42)
    ): PpgProgress {
        var progress = emptyProgress()
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
            progress = analyzer.addSample(frame.red, frame.timestampMs)
        }
        return progress
    }

    private fun emptyProgress() = PpgProgress(
        elapsedMs = 0,
        beats = 0,
        bpm = null,
        quality = 0f,
        signalStrength = 0f,
        fingerDetected = false,
        waveform = emptyList(),
        ready = false
    )

    // Đo đúng nhịp

    @Test
    fun `a 72 bpm pulse is measured as 72 bpm`() {
        val progress = feed(durationMs = 20_000, bpm = 72.0)

        assertNotNull("Đủ 20 giây tín hiệu sạch thì phải có kết quả", progress.bpm)
        assertEquals(
            "Nhịp đo được phải khớp nhịp thật, thực tế: ${progress.bpm}",
            72.0,
            progress.bpm!!.toDouble(),
            3.0
        )
        assertTrue("Đủ điều kiện chốt kết quả", progress.ready)
        assertTrue("Chất lượng phải đạt ngưỡng", progress.quality >= HeartRateMath.MIN_QUALITY)
    }

    @Test
    fun `a fast pulse is measured as well as a slow one`() {
        val fast = feed(durationMs = 20_000, bpm = 130.0)
        val slow = feed(durationMs = 25_000, bpm = 48.0)

        assertNotNull("Nhịp nhanh phải đo được", fast.bpm)
        assertNotNull("Nhịp chậm phải đo được", slow.bpm)
        assertEquals("Thực tế: ${fast.bpm}", 130.0, fast.bpm!!.toDouble(), 4.0)
        assertEquals("Thực tế: ${slow.bpm}", 48.0, slow.bpm!!.toDouble(), 4.0)
    }

    @Test
    fun `beats are counted while measuring`() {
        val progress = feed(durationMs = 20_000, bpm = 72.0)

        // 20 giây ở 72 bpm ≈ 24 nhịp
        assertTrue("Phải đếm được số nhịp đã nhận, thực tế: ${progress.beats}", progress.beats >= 15)
    }

    @Test
    fun `the waveform is normalised and inside the drawable range`() {
        val progress = feed(durationMs = 20_000, bpm = 72.0)

        assertTrue("Phải có dạng sóng để vẽ", progress.waveform.isNotEmpty())
        assertTrue(
            "Dạng sóng phải nằm trong -1..1 để màn đo vẽ được",
            progress.waveform.all { it >= -1f && it <= 1f }
        )
    }

    @Test
    fun `a clean signal is reported as strong`() {
        val progress = feed(durationMs = 20_000, bpm = 72.0)

        assertTrue("Ngón tay che đèn flash → phải nhận ra", progress.fingerDetected)
        assertTrue("Biên độ đỉnh tâm thu ~0,6% của DC → tín hiệu mạnh", progress.signalStrength > 0.5f)
    }

    // Chưa đủ dữ liệu

    @Test
    fun `nothing is concluded before the minimum measuring time`() {
        // Tín hiệu sạch nhưng mới đo 8 giây (< 15 giây tối thiểu)
        val progress = feed(durationMs = 8_000, bpm = 72.0)

        assertFalse("Chưa đủ thời gian tối thiểu thì không được tự chốt", progress.ready)
        assertTrue("Mới đo 8 giây, thực tế: ${progress.elapsedMs}", progress.elapsedMs in 7_000L..9_000L)
    }

    @Test
    fun `the first frames of a measurement do not produce a rhythm`() {
        // Nửa giây đầu: chưa đủ một chu kỳ tim nào
        val progress = feed(durationMs = 500, bpm = 72.0)

        assertNull("Chưa đủ nhịp thì chưa được hiện số", progress.bpm)
        assertFalse(progress.ready)
    }

    // Từ chối tín hiệu không phải mạch đập

    @Test
    fun `pure noise never passes as a heartbeat`() {
        val progress = feed(durationMs = 30_000, bpm = null, noise = 1.0)

        assertFalse("Nhiễu không có chu kỳ thì không được chốt kết quả", progress.ready)
        assertTrue(
            "Chất lượng phải thấp (tính chu kỳ ~0), thực tế: ${progress.quality}",
            progress.quality < HeartRateMath.MIN_QUALITY
        )
    }

    @Test
    fun `a signal buried in noise is not reported as a wrong rhythm`() {
        // Nhiễu mạnh (rung tay): chấp nhận không kết luận, nhưng có kết quả thì không được lệch hẳn.
        val progress = feed(durationMs = 30_000, bpm = 72.0, noise = 0.3)

        if (progress.bpm != null) {
            assertTrue(
                "Nhịp đo được không được lệch hẳn khỏi nhịp thật, thực tế: ${progress.bpm}",
                abs(progress.bpm!! - 72) <= 8
            )
        }
    }

    @Test
    fun `a dark frame means the finger is not covering the camera`() {
        val progress = feed(durationMs = 20_000, bpm = 72.0, dc = PpgTestSignal.DARK_DC)

        assertFalse("Ánh sáng phòng không phải ngón tay che đèn flash", progress.fingerDetected)
        assertNull("Không có ngón tay thì không có nhịp", progress.bpm)
        assertFalse(progress.ready)
        assertEquals("Nhịp đã đếm phải được xoá khi mất ngón tay", 0, progress.beats)
    }

    @Test
    fun `lifting the finger in the middle forgets the beats measured before`() {
        val analyzer = PpgAnalyzer()
        feed(analyzer, durationMs = 12_000, bpm = 72.0)
        val beforeLifting = analyzer.addSample(
            PpgTestSignal.redAt(
                timestampMs = PpgTestSignal.BASE_TIMESTAMP_MS + 12_000,
                bpm = 72.0,
                dc = PpgTestSignal.FINGER_DC,
                amplitude = PpgTestSignal.SYSTOLIC_AMPLITUDE
            ),
            PpgTestSignal.BASE_TIMESTAMP_MS + 12_000
        )
        assertTrue("Trước khi nhấc tay phải đang nhận được nhịp", beforeLifting.beats > 0)

        // Nhấc ngón tay: khung hình tối (ánh sáng phòng)
        val lifted = feed(
            analyzer,
            durationMs = 2_000,
            bpm = null,
            dc = PpgTestSignal.DARK_DC,
            startMs = PpgTestSignal.BASE_TIMESTAMP_MS + 12_100
        )

        assertEquals("Mất ngón tay → đếm lại từ đầu", 0, lifted.beats)
        assertNull("Không còn tín hiệu mạch đập thì không hiện số cũ", lifted.bpm)
        assertFalse(lifted.fingerDetected)
    }

    @Test
    fun `measuring works again after the finger is put back`() {
        val analyzer = PpgAnalyzer()
        feed(analyzer, durationMs = 12_000, bpm = 72.0)
        feed(
            analyzer,
            durationMs = 2_000,
            bpm = null,
            dc = PpgTestSignal.DARK_DC,
            startMs = PpgTestSignal.BASE_TIMESTAMP_MS + 12_100
        )

        val recovered = feed(
            analyzer,
            durationMs = 20_000,
            bpm = 72.0,
            startMs = PpgTestSignal.BASE_TIMESTAMP_MS + 14_200
        )

        assertNotNull("Đặt ngón tay lại thì đo được tiếp", recovered.bpm)
        assertEquals("Thực tế: ${recovered.bpm}", 72.0, recovered.bpm!!.toDouble(), 4.0)
        assertTrue(recovered.ready)
    }

    @Test
    fun `an uneven rhythm is not reported as a confident result`() {
        // Mỗi nhịp một khoảng khác nhau (loạn nhịp/rung tay): tín hiệu "có chu kỳ" nhưng nhịp không đều
        val analyzer = PpgAnalyzer()
        val random = Random(7)
        var timestamp = PpgTestSignal.BASE_TIMESTAMP_MS
        val end = PpgTestSignal.BASE_TIMESTAMP_MS + 40_000
        var progress = emptyProgress()
        while (timestamp <= end) {
            val bpm = 60.0 + random.nextDouble() * 45.0
            val red = PpgTestSignal.redAt(
                timestampMs = timestamp,
                bpm = bpm,
                dc = PpgTestSignal.FINGER_DC,
                amplitude = PpgTestSignal.SYSTOLIC_AMPLITUDE
            )
            progress = analyzer.addSample(red, timestamp)
            timestamp += PpgTestSignal.FRAME_MS
        }

        assertFalse("Nhịp không đều thì không được tự chốt", progress.ready)
        assertTrue(
            "Độ đều của nhịp phải kéo chất lượng xuống, thực tế: ${progress.quality}",
            progress.quality < HeartRateMath.MIN_QUALITY
        )
    }

    // Trạng thái

    @Test
    fun `reset clears everything for the next measurement`() {
        val analyzer = PpgAnalyzer()
        feed(analyzer, durationMs = 20_000, bpm = 72.0)

        analyzer.reset()
        val timestamp = PpgTestSignal.BASE_TIMESTAMP_MS + 100_000
        val first = analyzer.addSample(
            PpgTestSignal.redAt(timestamp, 72.0, PpgTestSignal.FINGER_DC, PpgTestSignal.SYSTOLIC_AMPLITUDE),
            timestamp
        )

        assertEquals("Đồng hồ đo phải bắt đầu lại", 0, first.elapsedMs)
        assertEquals("Số nhịp phải về 0", 0, first.beats)
        assertNull(first.bpm)
        assertTrue(first.waveform.isEmpty())
    }

    @Test
    fun `a frame timestamp of zero is handled like any other timestamp`() {
        // Mốc 0 không được dùng làm sentinel "chưa có", nếu không đồng hồ đo đứng ở 0 mãi.
        val analyzer = PpgAnalyzer()
        var progress = analyzer.addSample(PpgTestSignal.FINGER_DC, 0L)
        var timestamp = PpgTestSignal.FRAME_MS
        while (timestamp <= 20_000) {
            val red = PpgTestSignal.redAt(
                timestampMs = timestamp,
                bpm = 72.0,
                dc = PpgTestSignal.FINGER_DC,
                amplitude = PpgTestSignal.SYSTOLIC_AMPLITUDE
            )
            progress = analyzer.addSample(red, timestamp)
            timestamp += PpgTestSignal.FRAME_MS
        }

        assertEquals("Thực tế: ${progress.elapsedMs}", 20_000.0, progress.elapsedMs.toDouble(), 40.0)
        assertTrue(progress.ready)
    }
}
