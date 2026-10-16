package com.healthyai.app.presentation.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.healthyai.app.domain.widget.HealthWidgetRefresher
import com.healthyai.app.util.runCatchingCancellable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "HealthWidgetRefresher"

/** Triển khai [HealthWidgetRefresher] bằng Glance: `updateAll()` vẽ lại mọi widget đang có. */
@Singleton
class HealthWidgetRefresherImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : HealthWidgetRefresher {

    override suspend fun refresh() {
        runCatchingCancellable { HealthWidget().updateAll(context) }
            .onFailure { Log.w(TAG, "Không vẽ lại được widget", it) }
    }
}
