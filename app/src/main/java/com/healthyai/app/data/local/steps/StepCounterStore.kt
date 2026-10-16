package com.healthyai.app.data.local.steps

import android.content.Context
import com.healthyai.app.domain.usecase.steps.StepCounterMath
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Lưu trạng thái đếm bước qua SharedPreferences thường; sessionId theo tiến trình để không cộng khoảng lúc app đóng. */
@Singleton
class StepCounterStore @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Phiên của tiến trình đang chạy, sinh một lần mỗi khi tiến trình app được tạo lại. */
    private val sessionId: String = UUID.randomUUID().toString()

    /** Trạng thái đã lưu; `null` nếu lần đầu chạy app. */
    fun readState(): StepCounterMath.State? {
        val dateIso = prefs.getString(KEY_DATE, null) ?: return null
        return StepCounterMath.State(
            dateIso = dateIso,
            // Bản cũ chưa lưu phiên ⇒ chuỗi rỗng ⇒ được coi là phiên khác (không cộng thêm bước nào).
            sessionId = prefs.getString(KEY_SESSION_ID, "").orEmpty(),
            baseline = prefs.getLong(KEY_BASELINE, 0L),
            offset = prefs.getLong(KEY_OFFSET, 0L),
            lastCumulative = prefs.getLong(KEY_LAST_CUMULATIVE, 0L),
            lastSampleMs = prefs.getLong(KEY_LAST_SAMPLE_MS, 0L),
            todaySteps = prefs.getLong(KEY_TODAY_STEPS, 0L)
        )
    }

    /** Số bước hôm nay theo dữ liệu đã lưu (0 nếu đã sang ngày mới). */
    fun readStoredTodaySteps(todayIso: String = LocalDate.now().toString()): Long =
        StepCounterMath.storedTodaySteps(readState(), todayIso)

    /** Xử lý một mẫu cảm biến, lưu trạng thái mới; trả về số bước hôm nay sau mẫu này. */
    fun onSample(
        cumulative: Long,
        nowMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Long {
        val previous = readState()
        val result = StepCounterMath.sample(previous, cumulative, nowMs, sessionId, zone)
        writeState(previous, result.state)
        return result.todaySteps
    }

    /** Ghi trạng thái có tiết chế: đổi ngày/phiên/baseline/offset thì commit() ngay; chỉ mẫu mới thì apply() theo nhịp. */
    private fun writeState(previous: StepCounterMath.State?, state: StepCounterMath.State) {
        val milestoneChanged = previous == null ||
            previous.dateIso != state.dateIso ||
            previous.sessionId != state.sessionId ||
            previous.baseline != state.baseline ||
            previous.offset != state.offset
        val sampleIsFreshEnough = previous == null ||
            state.lastSampleMs - previous.lastSampleMs >= SAMPLE_WRITE_INTERVAL_MS
        if (!milestoneChanged && !sampleIsFreshEnough) return

        val editor = prefs.edit()
            .putString(KEY_DATE, state.dateIso)
            .putString(KEY_SESSION_ID, state.sessionId)
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

        /** Phiên chạy của app lúc ghi trạng thái (khoá thêm ở bản sửa "chỉ đếm khi app đang chạy"). */
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_BASELINE = "baseline"
        private const val KEY_OFFSET = "offset"
        private const val KEY_LAST_CUMULATIVE = "last_cumulative"
        private const val KEY_LAST_SAMPLE_MS = "last_sample_ms"
        private const val KEY_TODAY_STEPS = "today_steps"

        /** Khoảng cách tối thiểu giữa hai lần ghi prefs chỉ vì có mẫu mới (15 giây). */
        private const val SAMPLE_WRITE_INTERVAL_MS = 15_000L
    }
}
