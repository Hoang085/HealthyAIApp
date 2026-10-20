package com.healthyai.app.presentation.screen.home

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.healthyai.app.R
import com.healthyai.app.domain.model.SleepSchedule
import com.healthyai.app.presentation.ui.component.InfoActionCard
import com.healthyai.app.presentation.ui.format.formatClockTime
import com.healthyai.app.presentation.ui.format.formatSleepWindow
import com.healthyai.app.presentation.ui.format.formatSleepWindowDuration
import com.healthyai.app.presentation.ui.theme.HealthyAITheme
import java.time.LocalTime

/** Thẻ "Khung giờ ngủ" trên Trang chủ; app không tự quyết định giờ ngủ mà chỉ đo trong khung người dùng chọn. */
@Composable
fun SleepScheduleCard(
    schedule: SleepSchedule,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfoActionCard(
        icon = Icons.Default.Bedtime,
        title = stringResource(R.string.sleep_schedule_title),
        description = stringResource(
        R.string.sleep_schedule_current,
        formatSleepWindow(schedule),
        formatSleepWindowDuration(schedule.durationMinutes)
        ),
        actionLabel = stringResource(R.string.sleep_schedule_action),
        onAction = onEdit,
        modifier = modifier
    )
}

/** Hộp thoại đặt giờ đi ngủ và giờ thức; không tự lưu, mọi thay đổi báo lên qua onDraftChange để HomeViewModel kiểm tra và lưu. */
@Composable
fun SleepScheduleEditorDialog(
    draft: SleepSchedule,
    error: String?,
    onDraftChange: (SleepSchedule) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sleep_schedule_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.sleep_schedule_dialog_message),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )

                TimeRow(
                    label = stringResource(R.string.sleep_schedule_bedtime_label),
                    time = draft.bedtime,
                    onTimeChange = { onDraftChange(draft.copy(bedtime = it)) }
                )

                TimeRow(
                    label = stringResource(R.string.sleep_schedule_wake_label),
                    time = draft.wakeTime,
                    onTimeChange = { onDraftChange(draft.copy(wakeTime = it)) }
                )

                Text(
                    text = "Khung ${formatSleepWindow(draft)} · " +
                        formatSleepWindowDuration(draft.durationMinutes),
                    style = MaterialTheme.typography.labelMedium
                )

                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel_khong_dau)) }
        }
    )
}

@Composable
private fun TimeRow(label: String, time: LocalTime, onTimeChange: (LocalTime) -> Unit) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = { showTimePicker(context, time, onTimeChange) }) {
            Text(formatClockTime(time), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Mở hộp thoại chọn giờ hệ thống (24 giờ); dùng TimePickerDialog thay TimePicker của Material3 vì API ổn định, không cần @OptIn. */
private fun showTimePicker(context: Context, initial: LocalTime, onTimePicked: (LocalTime) -> Unit) {
    TimePickerDialog(
        context,
        { _, hourOfDay, minute -> onTimePicked(LocalTime.of(hourOfDay, minute)) },
        initial.hour,
        initial.minute,
        true
    ).show()
}

@Preview(showBackground = true, backgroundColor = 0xFFF6FFF5)
@Composable
private fun SleepScheduleEditorDialogPreview() {
    HealthyAITheme {
        SleepScheduleEditorDialog(
            draft = SleepSchedule.DEFAULT,
            error = "Giờ đi ngủ và giờ thức không được trùng nhau",
            onDraftChange = {},
            onDismiss = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6FFF5)
@Composable
private fun SleepScheduleCardPreview() {
    HealthyAITheme {
        SleepScheduleCard(schedule = SleepSchedule.DEFAULT, onEdit = {})
    }
}
