package com.healthyai.app.domain.usecase.steps

import java.time.Instant
import java.time.ZoneId

/**
 * Toán quy đổi cảm biến đếm bước thành **"số bước đã đếm được trong hôm nay"**.
 *
 * Cảm biến `TYPE_STEP_COUNTER` của Android cho ta một con số **luỹ kế từ lúc máy khởi động**
 * (ví dụ "12 345 bước kể từ khi bật máy"), chứ không cho biết "hôm nay đi được bao nhiêu".
 *
 * ### Luật đếm: chỉ tính phần app đang chạy
 * Con số luỹ kế **có** bao gồm cả những bước đi lúc app không mở. Nếu lấy thẳng hiệu số thì số bước sẽ
 * "tự nhảy" thêm mỗi lần mở app — kể cả những bước đi khi người dùng đã đóng hẳn ứng dụng. Vì vậy hàm
 * này chỉ tính phần **quan sát được trong lúc app đang chạy**, nhận diện bằng [sessionId]:
 *
 * - **Một phiên** = một lần tiến trình app được tạo: app đang mở, hoặc app còn nằm trong tác vụ và đang
 *   chạy nền (đếm tiếp bình thường).
 * - **Phiên đổi** (app bị đóng/xoá khỏi tác vụ rồi mở lại): [State.baseline] được đặt lại bằng giá trị
 *   luỹ kế **hiện tại** ⇒ khoảng thời gian app không chạy **không** bị cộng vào, nhưng phần **đã đếm
 *   trong ngày** vẫn được giữ ở [State.offset] (mở lại app trong cùng ngày thì số bước không bị mất).
 *
 * Ba ca khó còn lại cũng được xử lý ở đây (test kỹ ở `StepCounterMathTest`):
 *
 * 1. **Sang ngày mới trong lúc app còn chạy** — cần mốc luỹ kế tại 00:00 hôm nay, mà app có thể đang ở
 *    nền và bị hệ thống "ngủ" qua nửa đêm. Cách xử lý: **nội suy tuyến tính** giữa mẫu cuối của ngày
 *    hôm trước và mẫu đầu tiên của hôm nay (hai mẫu thuộc cùng một phiên đang chạy).
 * 2. **Máy khởi động lại** — bộ đếm **reset về 0**, nên nếu không xử lý thì số bước trong ngày sẽ tụt
 *    xuống. Cách xử lý: phần đã đếm trước khi khởi động lại được giữ trong [State.offset] và baseline
 *    đặt lại theo mẫu mới. (Khởi động lại máy luôn kết thúc phiên cũ, nên đây cũng là ca "phiên mới".)
 * 3. **Máy khởi động trong hôm nay** — không còn suy diễn "mọi bước từ lúc boot đều thuộc hôm nay";
 *    lần chạy đầu tiên của app chỉ bắt đầu đếm **từ mẫu đó** (trước đó app không chạy).
 *
 * Hàm không phụ thuộc Android (chỉ cần `java.time`) nên chạy được trong unit test JVM.
 */
object StepCounterMath {

    /**
     * Trạng thái lưu giữa các lần đọc (một bản ghi trong `SharedPreferences`).
     *
     * @property dateIso Ngày (ISO `yyyy-MM-dd`) mà [baseline]/[offset] đang thuộc về.
     * @property sessionId Phiên app đã ghi ra trạng thái này — xem KDoc của object. Chuỗi rỗng (dữ
     *   liệu của bản cũ, chưa từng lưu phiên) được coi là **phiên khác**, tức hướng an toàn: không
     *   cộng thêm bước nào cho khoảng thời gian không rõ app có chạy hay không.
     * @property baseline Giá trị luỹ kế của cảm biến được coi là **"0 bước của phiên này"**.
     * @property offset Số bước **đã đếm được trong ngày** trước [baseline] hiện tại (do phiên trước,
     *   hoặc do bộ đếm bị reset khi máy khởi động lại).
     * @property lastCumulative Mẫu luỹ kế gần nhất (để phát hiện bộ đếm bị reset và để nội suy).
     * @property lastSampleMs Thời điểm (epoch ms) của mẫu gần nhất.
     * @property todaySteps Số bước hôm nay tính ở lần đọc gần nhất — dùng để hiện ngay khi mở app.
     */
    data class State(
        val dateIso: String,
        val sessionId: String,
        val baseline: Long,
        val offset: Long,
        val lastCumulative: Long,
        val lastSampleMs: Long,
        val todaySteps: Long
    )

    data class Result(val state: State, val todaySteps: Long)

    /**
     * Xử lý một mẫu cảm biến và trả về số bước đã đếm được trong hôm nay.
     *
     * @param state Trạng thái đã lưu trước đó, `null` nếu lần đầu chạy.
     * @param cumulative Giá trị luỹ kế vừa đọc từ cảm biến.
     * @param nowMs Thời điểm đọc mẫu (epoch ms) — truyền vào để test được.
     * @param sessionId Định danh **phiên chạy của tiến trình app** hiện tại. Khác với
     *   [State.sessionId] nghĩa là app đã bị đóng rồi mở lại giữa hai mẫu ⇒ bỏ qua khoảng thời gian đó.
     * @param zone Múi giờ dùng để xác định ranh giới ngày.
     */
    fun sample(
        state: State?,
        cumulative: Long,
        nowMs: Long,
        sessionId: String,
        zone: ZoneId
    ): Result {
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val todayIso = today.toString()

        if (state == null) {
            // Chưa có gì để so sánh: bắt đầu đếm từ chính mẫu này (không suy diễn ngược về trước).
            return result(todayIso, sessionId, baseline = cumulative, offset = 0L, cumulative, nowMs)
        }

        val newDay = state.dateIso != todayIso
        // Bộ đếm luỹ kế nhỏ hơn mẫu trước ⇒ máy (hoặc cảm biến) đã khởi động lại.
        val counterReset = cumulative < state.lastCumulative

        val baseline: Long
        val offset: Long
        when {
            // App đã bị đóng rồi mở lại: đếm lại từ mẫu hiện tại, giữ phần đã đếm trong ngày.
            state.sessionId != sessionId -> {
                baseline = cumulative
                offset = if (newDay) 0L else carriedOverSteps(state)
            }

            // Cùng một phiên đang chạy mà sang ngày mới: nội suy mốc 00:00 giữa hai mẫu của phiên này.
            newDay -> {
                offset = 0L
                baseline = if (counterReset) {
                    cumulative
                } else {
                    interpolateAt(
                        t0 = state.lastSampleMs,
                        c0 = state.lastCumulative,
                        t1 = nowMs,
                        c1 = cumulative,
                        targetMs = today.atStartOfDay(zone).toInstant().toEpochMilli()
                    )
                }
            }

            // Cùng phiên, cùng ngày, nhưng bộ đếm vừa bị reset: giữ lại phần đã đi trước đó.
            counterReset -> {
                baseline = cumulative
                offset = carriedOverSteps(state)
            }

            else -> {
                baseline = state.baseline
                offset = state.offset
            }
        }

        return result(todayIso, sessionId, baseline, offset, cumulative, nowMs)
    }

    /**
     * Số bước nên hiển thị **ngay khi mở app** (trước khi cảm biến kịp phát mẫu mới).
     *
     * Chỉ dùng giá trị đã lưu nếu nó thuộc **đúng hôm nay** — nếu đã sang ngày mới thì trả `0` thay vì
     * hiện số của hôm qua.
     */
    fun storedTodaySteps(state: State?, todayIso: String): Long =
        if (state != null && state.dateIso == todayIso) state.todaySteps else 0L

    /** Số bước đã đếm được trong ngày tính tới mẫu cuối của [state] (dùng khi baseline đổi). */
    private fun carriedOverSteps(state: State): Long =
        state.offset + (state.lastCumulative - state.baseline).coerceAtLeast(0L)

    private fun result(
        todayIso: String,
        sessionId: String,
        baseline: Long,
        offset: Long,
        cumulative: Long,
        nowMs: Long
    ): Result {
        val todaySteps = (offset + (cumulative - baseline)).coerceAtLeast(0L)
        return Result(
            state = State(
                dateIso = todayIso,
                sessionId = sessionId,
                baseline = baseline,
                offset = offset,
                lastCumulative = cumulative,
                lastSampleMs = nowMs,
                todaySteps = todaySteps
            ),
            todaySteps = todaySteps
        )
    }

    /**
     * Ước lượng giá trị luỹ kế tại [targetMs] bằng nội suy tuyến tính giữa hai mẫu
     * `(t0, c0)` → `(t1, c1)`, luôn nằm trong khoảng `[min(c0,c1), max(c0,c1)]` để không sinh ra
     * giá trị vô lý khi dữ liệu mẫu bất thường.
     */
    private fun interpolateAt(t0: Long, c0: Long, t1: Long, c1: Long, targetMs: Long): Long {
        if (t1 <= t0 || c0 == c1) return c1
        val clamped = targetMs.coerceIn(t0, t1)
        val estimate = c0 + ((c1 - c0).toDouble() * (clamped - t0).toDouble() / (t1 - t0).toDouble()).toLong()
        return estimate.coerceIn(minOf(c0, c1), maxOf(c0, c1))
    }
}
