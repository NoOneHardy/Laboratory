package ch.no1hardy.orbit7.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import ch.no1hardy.orbit7.core.domain.model.ZeroSpendSource
import ch.no1hardy.orbit7.core.domain.usecase.MarkZeroSpendDayUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * "Nothing spent today", straight from the notification shade.
 *
 * No activity is launched: one tap, a row in the database, and the notification disappears. This
 * single interaction is what keeps the confidence factor honest for a user who never opens the app
 * on a quiet day.
 */
@AndroidEntryPoint
class ZeroSpendReceiver : BroadcastReceiver() {
    @Inject lateinit var markZeroSpendDay: MarkZeroSpendDayUseCase

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_MARK_ZERO_SPEND) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                markZeroSpendDay(source = ZeroSpendSource.NOTIFICATION)
                NotificationManagerCompat.from(context).cancel(DAILY_NOTIFICATION_ID)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_ZERO_SPEND = "ch.no1hardy.orbit7.MARK_ZERO_SPEND"
        private const val DAILY_NOTIFICATION_ID = 1
    }
}
