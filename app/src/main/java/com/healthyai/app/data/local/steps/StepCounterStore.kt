package com.healthyai.app.data.local.steps

import android.content.Context
import com.healthyai.app.domain.usecase.steps.StepCounterMath
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lưu trạng thái đếm bước giữa các lần mở app: **phiên chạy hiện tại**, baseline của phiên, phần đã
 * đếm trước đó trong ngày, mẫu gần nhất (để nội suy mốc 00:00) và số bước hôm nay tính lần cuối.
 *
 * **Phiên chạy** ([sessionId]) được sinh **một lần cho mỗi tiến trình app**, nên nó là dấu hiệu đáng tin
 * để biết app có bị đóng rồi mở lại hay không:
 * - app **đang mở**, hoặc **còn nằm trong tác vụ** (chạy nền) → phiên **không đổi** → bước chân vẫn
 *   được đếm tiếp;
 * - app bị **đóng hẳn** (vuốt khỏi tác vụ, tiến trình bị thu hồi) → lần mở sau là **phiên mới** → khoảng
 *   thời gian app không chạy **không** được cộng vào (phần đã đếm trong ngày vẫn giữ).
 *
 * Luật đếm chi tiết nằm ở `StepCounterMath`.
 *
 * Dùng `SharedPreferences` **thường** (không bọc Keystore) vì đây chỉ là số đếm bước, không phải dữ liệu
 * nhạy cảm — khác với passphrase mã hoá database (`AndroidPassphraseStorage`).
 */
@Singleton
class StepCounterStore @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Phiên của **tiến trình đang chạy**: sinh một lần khi `StepCounterStore` (singleton) được tạo —
     * tức là mỗi lần tiến trình app được tạo lại (sau khi app bị đóng hoặc bị hệ thống thu hồi).
     */
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
        zone: ZoneId = ZoneId.systemDefault()
    ): Long {
        val previous = readState()
        val result = StepCounterMath.sample(previous, cumulative, nowMs, sessionId, zone)
        writeState(previous, result.state)
        return result.todaySteps
    }

    /**
     * Ghi trạng thái, có tiết chế tần suất ghi:
     * - **đổi ngày / đổi phiên / đổi baseline / đổi offset** → ghi ngay và **đồng bộ** (`commit()`) để
     *   không mất mốc ngày khi tiến trình bị hệ thống thu hồi;
     * - **chỉ mẫu mới hơn** → ghi không đồng bộ (`apply()`), và chỉ khi mẫu cũ đã quá
     *   [SAMPLE_WRITE_INTERVAL_MS] (cảm biến có thể phát rất dày, không cần ghi từng mẫu).
     */
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
