package com.healthyai.app.data.local.steps

import android.content.Context
import android.os.SystemClock
import com.healthyai.app.domain.usecase.steps.StepCounterMath
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lưu trạng thái đếm bước giữa các lần mở app: baseline của ngày, phần bù sau khi máy khởi động lại,
 * mẫu gần nhất (để nội suy mốc 00:00) và số bước hôm nay tính lần cuối.
 *
 * Dùng `SharedPreferences` **thường** (không bọc Keystore) vì đây chỉ là số đếm bước, không phải dữ liệu
 * nhạy cảm — khác với passphrase mã hoá database (`AndroidPassphraseStorage`).
 */
@Singleton
class StepCounterStore @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Trạng thái đã lưu; `null` nếu lần đầu chạy app. */
    fun readState(): StepCounterMath.State? {
        val dateIso = prefs.getString(KEY_DATE, null) ?: return null
        return StepCounterMath.State(
            dateIso = dateIso,
            baseline = prefs.getLong(KEY_BASELINE, 0L),
            offset = prefs.getLong(KEY_OFFSET, 0L),
            lastCumulative = prefs.getLong(KEY_LAST_CUMULATIVE, 0L),
            lastSampleMs = prefs.getLong(KEY_LAST_SAMPLE_MS, 0L),
            todaySteps = prefs.getLong(KEY_TODAY_STEPS, 0L)
        )
    }

    /** Số bước của **hôm nay** theo dữ liệu đã lưu (0 nếu đã sang ngày mới). */
    fun readStoredTodaySteps(todayIso: String = LocalDate.now().toString()): Long =
        StepCounterMath.storedTodaySteps(readState(), todayIso)

    /**
     * Xử lý một mẫu cảm biến: tính số bước hôm nay rồi lưu lại trạng thái mới.
     *
     * @return số bước hôm nay sau mẫu này.
     */
    fun onSample(
        cumulative: Long,
        nowMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
        bootTimeMs: Long = deviceBootTimeMs()
    ): Long {
        val previous = readState()
        val result = StepCounterMath.sample(previous, cumulative, nowMs, bootTimeMs, zone)
        writeState(previous, result.state)
        return result.todaySteps
    }

    /**
     * Ghi trạng thái, có tiết chế tần suất ghi:
     * - **đổi ngày / đổi baseline / đổi offset** → ghi ngay và **đồng bộ** (`commit()`) để không mất
     *   mốc ngày khi tiến trình bị hệ thống thu hồi;
     * - **chỉ mẫu mới hơn** → ghi không đồng bộ (`apply()`), và chỉ khi mẫu cũ đã quá
     *   [SAMPLE_WRITE_INTERVAL_MS] (cảm biến có thể phát rất dày, không cần ghi từng mẫu).
     */
    private fun writeState(previous: StepCounterMath.State?, state: StepCounterMath.State) {
        val milestoneChanged = previous == null ||
            previous.dateIso != state.dateIso ||
            previous.baseline != state.baseline ||
            previous.offset != state.offset
        val sampleIsFreshEnough = previous == null ||
            state.lastSampleMs - previous.lastSampleMs >= SAMPLE_WRITE_INTERVAL_MS
        if (!milestoneChanged && !sampleIsFreshEnough) return

        val editor = prefs.edit()
            .putString(KEY_DATE, state.dateIso)
            .putLong(KEY_BASELINE, state.baseline)
            .putLong(KEY_OFFSET, state.offset)
            .putLong(KEY_LAST_CUMULATIVE, state.lastCumulative)
            .putLong(KEY_LAST_SAMPLE_MS, state.lastSampleMs)
            .putLong(KEY_TODAY_STEPS, state.todaySteps)

        if (milestoneChanged) editor.commit() else editor.apply()
    }

    companion object {
        const val PREFS_NAME = "healthy_ai_step_counter"

        private const val KEY_DATE = "date"
        private const val KEY_BASELINE = "baseline"
        private const val KEY_OFFSET = "offset"
        private const val KEY_LAST_CUMULATIVE = "last_cumulative"
        private const val KEY_LAST_SAMPLE_MS = "last_sample_ms"
        private const val KEY_TODAY_STEPS = "today_steps"

        /** Khoảng cách tối thiểu giữa hai lần ghi prefs chỉ vì có mẫu mới (15 giây). */
        private const val SAMPLE_WRITE_INTERVAL_MS = 15_000L

        /**
         * Thời điểm máy khởi động theo đồng hồ tường — dùng để biết các bước từ lúc boot có thuộc hôm nay.
         */
        fun deviceBootTimeMs(): Long = System.currentTimeMillis() - SystemClock.elapsedRealtime()
    }
}
