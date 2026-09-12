package ch.no1hardy.orbit7.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import ch.no1hardy.orbit7.R

/**
 * Two channels, because the two kinds of notification have genuinely different stakes: a daily
 * nudge that may always be ignored, and a real-world deadline with money attached.
 */
object Orbit7NotificationChannels {
    const val DAILY_REMINDER = "orbit7.daily_reminder"
    const val CONTRACT_DEADLINE = "orbit7.contract_deadline"
    const val SETTLEMENT = "orbit7.settlement"

    fun ensure(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                DAILY_REMINDER,
                context.getString(R.string.channel_daily_reminder),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.channel_daily_reminder_description) },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CONTRACT_DEADLINE,
                context.getString(R.string.channel_contract_deadline),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.channel_contract_deadline_description) },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                SETTLEMENT,
                context.getString(R.string.channel_settlement),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.channel_settlement_description) },
        )
    }
}
