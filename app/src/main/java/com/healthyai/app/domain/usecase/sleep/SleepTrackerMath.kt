package com.healthyai.app.domain.usecase.sleep

import com.healthyai.app.domain.model.SleepEnvironment
import com.healthyai.app.domain.model.SleepSchedule
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Toán **tự tính số giờ ngủ** từ cảm biến của chính điện thoại (nguồn "tự lực" của app, song song với
 * `StepCounterMath` của phần đếm bước chân).
 *
 * Khác với `TYPE_STEP_COUNTER` (một con số luỹ kế do phần cứng đếm sẵn), Android **không** có cảm biến
 * "đang ngủ". Vì vậy app suy ra giấc ngủ từ ba tín hiệu đọc được **không cần quyền** — màn hình có đang
 * được dùng không, phòng có tối không, và điện thoại có bất động không — kết hợp với **khung giờ ngủ do
 * người dùng đặt** ([SleepSchedule]): app **không** tự quyết định giờ ngủ, người dùng đặt *giờ đi ngủ* và
 * *giờ thức*, app chỉ đo trong khung đó. Đủ cả ba điều kiện **và** đang trong khung ⇒ **đang ngủ**
 * ([isAsleep]).
 *
 * Thời gian ngủ được **cộng dồn** giữa hai lần lấy mẫu khi **cả hai mẫu đều là "đang ngủ"** ([sample]) —
 * nhờ vậy khoảng thời gian app/worker không chạy vẫn được tính (WorkManager có thể bị hoãn trong chế độ
 * Doze), còn khoảng thức giữa đêm thì **không** bị tính.
 *
 * ### Mốc "đêm"
 * Một "đêm" `D` là khoảng `[giờ đi ngủ hôm D-1, giờ thức hôm D)` và được gọi tên theo **ngày thức dậy**
 * `D`. Khoá đêm ([nightIsoOf]) chỉ **nhảy khi bước vào khung giờ ngủ (giờ đi ngủ)** — nhờ vậy suốt ngày
 * (sau giờ thức tới trước giờ đi ngủ) khoá vẫn là đêm vừa kết thúc, số giờ ngủ của đêm đó **không bị
 * xoá**. Khi sang đêm mới, số của đêm cũ được [sample] trả về qua [Result.archived] để tầng lưu trữ giữ
 * lại, và [displaySleepMinutes] dùng nó làm giá trị hiển thị cho tới khi đêm mới đủ dài để thay thế.
 *
 * Hàm chỉ dùng `java.time` nên chạy được trong unit test JVM (`SleepTrackerMathTest`).
 *
 * ⚠️ **Giới hạn đã biết:** đây là **ước lượng** từ điện thoại, không phải thiết bị y tế. Nếu điện thoại
 * bị bỏ quên ở nơi tối và bất động **trong khung giờ ngủ** (ví dụ trong túi khi đang ở ngoài), thời gian
 * đó bị tính là ngủ. Muốn số liệu chính xác theo pha (Sâu/Nông/REM) thì cần đồng hồ đeo tay — app vẫn
 * ưu tiên số của **Health Connect** khi có.
 */
object SleepTrackerMath {

    /** Dưới ngưỡng này (lux) coi là phòng tối. Để thấp nhằm tránh nhận nhầm phòng có đèn/TV là đang ngủ. */
    const val DARK_LUX = 5f

    /** Độ lệch gia tốc lớn nhất (đơn vị `g`) còn coi là "điện thoại bất động". */
    const val STILL_MOTION_G = 0.05f

    /** Dưới số phút này thì **không** coi là một giấc ngủ (lọc nhiễu: ngồi yên một lúc không phải ngủ). */
    const val MIN_SLEEP_MINUTES = 45L

    /** Trần hợp lý cho một đêm ngủ (chặn dữ liệu vô lý khi cảm biến bị kẹt/điện thoại bị bỏ quên). */
    const val MAX_SLEEP_MINUTES = 12L * 60L

    private const val MILLIS_PER_MINUTE = 60_000L

    /**
     * Trạng thái cộng dồn của **đêm đang xét** (một bản ghi trong `SharedPreferences`).
     *
     * @property nightIso Đêm (`yyyy-MM-dd` của **ngày thức dậy**) mà [sleepMillis] đang thuộc về.
     * @property sleepMillis Tổng thời gian ngủ đã cộng dồn của đêm đó.
     * @property lastSampleMs Thời điểm (epoch ms) của mẫu gần nhất — mốc để tính phần cộng thêm.
     * @property lastWasAsleep Mẫu gần nhất có phải "đang ngủ" hay không. Chỉ cộng dồn khi **cả** mẫu
     *   trước **và** mẫu hiện tại đều là "đang ngủ", nên khoảng thức giữa đêm không bị tính.
     */
    data class State(
        val nightIso: String,
        val sleepMillis: Long,
        val lastSampleMs: Long,
        val lastWasAsleep: Boolean
    ) {
        val sleepMinutes: Long get() = sleepMillis / MILLIS_PER_MINUTE
    }

    /**
     * Một đêm **đã chốt** (được giữ lại khi [sample] bước sang đêm mới) — để ô "Ngủ" ở màn Trang chủ
     * không nhảy về `0` ngay khi đêm mới vừa bắt đầu.
     */
    data class NightTotal(val nightIso: String, val sleepMillis: Long) {
        val sleepMinutes: Long get() = sleepMillis / MILLIS_PER_MINUTE
    }

    data class Result(
        val state: State,
        val sleepMillis: Long,
        /** Đêm vừa khép lại ở mẫu này; `null` nếu mẫu này không làm chuyển đêm. */
        val archived: NightTotal? = null
    ) {
        val sleepMinutes: Long get() = sleepMillis / MILLIS_PER_MINUTE
    }

    /**
     * Đêm mà thời điểm [nowMs] thuộc về, theo khung giờ [schedule] của người dùng.
     *
     * Khung **vắt qua nửa đêm** (ví dụ 23:00 → 07:00): trước giờ đi ngủ → còn là đêm vừa kết thúc sáng
     * nay (`today`); từ giờ đi ngủ trở đi → đêm mới bắt đầu, thuộc về `tomorrow` (ngày sẽ thức dậy).
     * Khung **không vắt** (ví dụ 01:00 → 08:00) nằm gọn trong một ngày nên khoá đêm chính là `today`.
     */
    fun nightIsoOf(nowMs: Long, zone: ZoneId, schedule: SleepSchedule): String {
        val local = Instant.ofEpochMilli(nowMs).atZone(zone)
        val date = if (schedule.wrapsMidnight && local.toLocalTime() >= schedule.bedtime) {
            local.toLocalDate().plusDays(1)
        } else {
            local.toLocalDate()
        }
        return date.toString()
    }

    /** [nowMs] có nằm trong khung giờ ngủ của người dùng hay không. */
    fun isInSleepWindow(nowMs: Long, zone: ZoneId, schedule: SleepSchedule): Boolean =
        schedule.contains(Instant.ofEpochMilli(nowMs).atZone(zone).toLocalTime())

    /** Ba tín hiệu của [environment] có đủ để kết luận "đang ngủ" tại thời điểm [nowMs] hay không. */
    fun isAsleep(
        environment: SleepEnvironment,
        nowMs: Long,
        zone: ZoneId,
        schedule: SleepSchedule
    ): Boolean =
        isInSleepWindow(nowMs, zone, schedule) &&
            !environment.screenInteractive &&
            environment.lightLux < DARK_LUX &&
            environment.maxMotionG < STILL_MOTION_G

    /**
     * Cập nhật trạng thái với một mẫu mới.
     *
     * @param state Trạng thái đã lưu, `null` nếu lần đầu chạy hoặc đã sang đêm mới. Khi sang đêm mới,
     *   tổng của đêm cũ **không** mang sang (mỗi đêm là một con số riêng) mà được trả về ở
     *   [Result.archived] để tầng lưu trữ giữ lại cho màn Trang chủ.
     * @param nowMs Thời điểm lấy mẫu (epoch ms) — truyền vào để test được.
     * @param asleepNow Kết quả phân loại của mẫu này (thường là [isAsleep]).
     */
    fun sample(
        state: State?,
        nowMs: Long,
        asleepNow: Boolean,
        zone: ZoneId,
        schedule: SleepSchedule
    ): Result {
        val night = nightIsoOf(nowMs, zone, schedule)
        val previous = state?.takeIf { it.nightIso == night }

        val archived = if (state != null && previous == null && state.sleepMillis > 0L) {
            NightTotal(nightIso = state.nightIso, sleepMillis = state.sleepMillis)
        } else {
            null
        }

        // Chỉ cộng phần thời gian nằm giữa hai mẫu "đang ngủ" liên tiếp.
        val carriedMillis = if (previous != null && previous.lastWasAsleep && asleepNow) {
            (nowMs - previous.lastSampleMs).coerceAtLeast(0L)
        } else {
            0L
        }

        val totalMillis = ((previous?.sleepMillis ?: 0L) + carriedMillis)
            .coerceIn(0L, MAX_SLEEP_MINUTES * MILLIS_PER_MINUTE)

        return Result(
            state = State(
                nightIso = night,
                sleepMillis = totalMillis,
                lastSampleMs = nowMs,
                lastWasAsleep = asleepNow
            ),
            sleepMillis = totalMillis,
            archived = archived
        )
    }

    /**
     * Số phút ngủ của **đúng** đêm [nightIso] theo trạng thái đã lưu; `0` nếu trạng thái thuộc đêm khác.
     */
    fun storedSleepMinutes(state: State?, nightIso: String): Long =
        if (state != null && state.nightIso == nightIso) state.sleepMinutes else 0L

    /**
     * Số phút ngủ nên **hiển thị** trên thẻ "Tóm tắt sức khỏe" (và ghi vào `health_data_history`).
     *
     * - **Ngoài khung giờ ngủ** (sau giờ thức, trước giờ đi ngủ): đêm đang xét chính là **đêm vừa kết
     *   thúc sáng nay**, nên hiển thị thẳng số của nó — kể cả khi ít hơn ngưỡng (người dùng thức trắng
     *   thì phải hiện `0` chứ **không** lấy số của đêm cũ hơn ra thay).
     * - **Trong khung giờ ngủ** (đang ngủ): đủ ngưỡng [MIN_SLEEP_MINUTES] thì hiện số đang cộng dồn;
     *   chưa đủ (đêm mới vừa bắt đầu) thì vẫn hiện **đêm vừa kết thúc** — lấy từ [archived] hoặc từ
     *   chính [state] khi chưa có mẫu nào trong khung mới. Nhờ vậy ô "Ngủ" không nhảy về `0` vào buổi
     *   tối, mà cũng không bao giờ hiện số của một đêm cũ hơn đêm vừa qua.
     */
    fun displaySleepMinutes(
        state: State?,
        archived: NightTotal?,
        nowMs: Long,
        zone: ZoneId,
        schedule: SleepSchedule
    ): Long {
        val currentNight = nightIsoOf(nowMs, zone, schedule)
        val ongoing = storedSleepMinutes(state, currentNight)

        if (!isInSleepWindow(nowMs, zone, schedule)) return ongoing
        if (ongoing >= MIN_SLEEP_MINUTES) return ongoing

        val finishedNight = LocalDate.parse(currentNight).minusDays(1).toString()
        val finished = storedSleepMinutes(state, finishedNight).takeIf { it > 0L }
            ?: archived?.takeIf { it.nightIso == finishedNight }?.sleepMinutes

        return if (finished != null && finished >= MIN_SLEEP_MINUTES) finished else ongoing
    }

    /**
     * Đổi số phút ngủ thành **số giờ** để hiển thị/lưu vào `health_data_history`.
     *
     * - Dưới [MIN_SLEEP_MINUTES] → `0.0` (chưa đủ để coi là một giấc ngủ).
     * - Làm tròn **1 chữ số thập phân** để tránh sai lệch làm tròn khi UI đổi giờ → phút.
     */
    fun toSleepHours(minutes: Long): Double {
        if (minutes < MIN_SLEEP_MINUTES) return 0.0
        return Math.round(minutes / 60.0 * 10.0) / 10.0
    }
}
