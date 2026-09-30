package com.healthyai.app.data.local.sleep

import android.content.Context
import com.healthyai.app.domain.model.SleepSchedule
import com.healthyai.app.domain.usecase.sleep.SleepTrackerMath
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lưu trạng thái **tự tính giấc ngủ** giữa các lần chạy worker: đêm đang xét, thời gian ngủ đã cộng
 * dồn, mẫu gần nhất (để biết có cộng thêm khoảng vừa qua hay không), và **đêm đã chốt gần nhất**.
 *
 * Đêm đã chốt được giữ riêng để ô "Ngủ" ở màn Trang chủ vẫn hiển thị số của đêm vừa qua sau khi đêm
 * mới bắt đầu (giờ đi ngủ) nhưng chưa đủ dài — xem `SleepTrackerMath.displaySleepMinutes`.
 *
 * Khung giờ ngủ **không** lưu ở đây: đó là thiết lập của người dùng trong bảng Room `app_settings`, do
 * `SettingsRepository` cung cấp và được truyền vào mỗi lần đọc/ghi (xem [SleepSchedule]).
 *
 * Dùng `SharedPreferences` **thường** (không bọc Keystore) — đây chỉ là số phút ngủ ước lượng, không
 * phải dữ liệu nhạy cảm, giống `StepCounterStore` của phần đếm bước.
 */
@Singleton
class SleepTrackerStore @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Trạng thái của **đêm đang xét**; `null` nếu chưa từng lấy mẫu. */
    fun readState(): SleepTrackerMath.State? {
        val nightIso = prefs.getString(KEY_NIGHT, null) ?: return null
        return SleepTrackerMath.State(
            nightIso = nightIso,
            sleepMillis = prefs.getLong(KEY_SLEEP_MILLIS, 0L),
            lastSampleMs = prefs.getLong(KEY_LAST_SAMPLE_MS, 0L),
            lastWasAsleep = prefs.getBoolean(KEY_LAST_WAS_ASLEEP, false)
        )
    }

    /** Đêm **đã chốt** gần nhất (được lưu lại khi bước sang đêm mới); `null` nếu chưa có. */
    fun readArchivedNight(): SleepTrackerMath.NightTotal? {
        val nightIso = prefs.getString(KEY_PREVIOUS_NIGHT, null) ?: return null
        return SleepTrackerMath.NightTotal(
            nightIso = nightIso,
            sleepMillis = prefs.getLong(KEY_PREVIOUS_SLEEP_MILLIS, 0L)
        )
    }

    /**
     * Số phút ngủ để **hiển thị/lưu lịch sử** — đêm đang xét nếu đã đủ ngưỡng, không thì đêm vừa kết
     * thúc (`SleepTrackerMath.displaySleepMinutes`).
     */
    fun readDisplaySleepMinutes(
        schedule: SleepSchedule,
        nowMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Long = SleepTrackerMath.displaySleepMinutes(
        state = readState(),
        archived = readArchivedNight(),
        nowMs = nowMs,
        zone = zone,
        schedule = schedule
    )

    /**
     * Xử lý một mẫu mới: cộng dồn nếu mẫu này **và** mẫu trước đều là "đang ngủ", rồi lưu lại (kèm
     * việc chốt đêm cũ nếu mẫu này làm chuyển đêm).
     *
     * @return số phút ngủ của **đêm đang xét** sau mẫu này.
     */
    fun onSample(
        asleepNow: Boolean,
        schedule: SleepSchedule,
        nowMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Long {
        val result = SleepTrackerMath.sample(readState(), nowMs, asleepNow, zone, schedule)
        writeState(result.state, result.archived)
        return result.sleepMinutes
    }

    /**
     * Ghi **đồng bộ** (`commit()`): mỗi lần lấy mẫu cách nhau cả chục phút (worker chạy 15 phút/lần)
     * nên chi phí không đáng kể, nhưng nếu tiến trình bị hệ thống thu hồi thì mốc đêm / thời gian đã
     * cộng dồn / đêm đã chốt không bị mất.
     */
    private fun writeState(
        state: SleepTrackerMath.State,
        archived: SleepTrackerMath.NightTotal?
    ) {
        val editor = prefs.edit()
            .putString(KEY_NIGHT, state.nightIso)
            .putLong(KEY_SLEEP_MILLIS, state.sleepMillis)
            .putLong(KEY_LAST_SAMPLE_MS, state.lastSampleMs)
            .putBoolean(KEY_LAST_WAS_ASLEEP, state.lastWasAsleep)

        // Chỉ ghi đè đêm đã chốt khi mẫu này thực sự làm chuyển đêm, để không xoá mất giá trị cũ.
        if (archived != null) {
            editor
                .putString(KEY_PREVIOUS_NIGHT, archived.nightIso)
                .putLong(KEY_PREVIOUS_SLEEP_MILLIS, archived.sleepMillis)
        }

        editor.commit()
    }

    companion object {
        const val PREFS_NAME = "healthy_ai_sleep_tracker"

        private const val KEY_NIGHT = "night"
        private const val KEY_SLEEP_MILLIS = "sleep_millis"
        private const val KEY_LAST_SAMPLE_MS = "last_sample_ms"
        private const val KEY_LAST_WAS_ASLEEP = "last_was_asleep"
        private const val KEY_PREVIOUS_NIGHT = "previous_night"
        private const val KEY_PREVIOUS_SLEEP_MILLIS = "previous_sleep_millis"
    }
}
