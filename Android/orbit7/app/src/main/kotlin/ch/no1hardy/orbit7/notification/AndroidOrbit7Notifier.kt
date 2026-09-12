package ch.no1hardy.orbit7.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ch.no1hardy.orbit7.MainActivity
import ch.no1hardy.orbit7.R
import ch.no1hardy.orbit7.core.data.notification.Orbit7Notifier
import ch.no1hardy.orbit7.core.designsystem.format.MoneyFormatter
import ch.no1hardy.orbit7.core.domain.model.Contract
import ch.no1hardy.orbit7.core.domain.money.Money
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts the app's notifications.
 *
 * The daily reminder carries both actions from `docs/03-screens.md`: **Quick add**, which opens the
 * screen, and **Nothing spent today**, which writes the zero-spend mark *without launching the app*
 * — the cheapest possible way to produce a signal day.
 */
@Singleton
class AndroidOrbit7Notifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val formatter: MoneyFormatter,
    ) : Orbit7Notifier {
        private val manager = NotificationManagerCompat.from(context)

        override fun postDailyReminder(
            remainingThisWeek: Money,
            spentToday: Money,
        ) {
            val notification =
                NotificationCompat
                    .Builder(context, Orbit7NotificationChannels.DAILY_REMINDER)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(
                        context.getString(R.string.notification_daily_title, formatter.format(remainingThisWeek)),
                    ).setContentText(
                        context.getString(R.string.notification_daily_body, formatter.format(spentToday)),
                    ).setContentIntent(openApp(null, REQUEST_OPEN))
                    .addAction(
                        R.drawable.ic_notification,
                        context.getString(R.string.notification_action_quick_add),
                        openApp(MainActivity.DESTINATION_QUICK_ADD, REQUEST_QUICK_ADD),
                    ).addAction(
                        R.drawable.ic_notification,
                        context.getString(R.string.notification_action_zero_spend),
                        zeroSpendIntent(),
                    ).setAutoCancel(true)
                    .build()

            notifyIfPermitted(ID_DAILY, notification)
        }

        override fun postContractDeadline(
            contract: Contract,
            daysRemaining: Int,
        ) {
            val notification =
                NotificationCompat
                    .Builder(context, Orbit7NotificationChannels.CONTRACT_DEADLINE)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.notification_deadline_title, contract.name))
                    .setContentText(context.getString(R.string.notification_deadline_body, daysRemaining))
                    .setContentIntent(openApp(MainActivity.DESTINATION_DRAINS, contract.id.toInt()))
                    .setAutoCancel(true)
                    .build()

            notifyIfPermitted(ID_DEADLINE_BASE + contract.id.toInt(), notification)
        }

        override fun postSettlementReady(awardedEp: Int) {
            val notification =
                NotificationCompat
                    .Builder(context, Orbit7NotificationChannels.SETTLEMENT)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.notification_settlement_title))
                    .setContentText(context.getString(R.string.notification_settlement_body, awardedEp))
                    .setContentIntent(openApp(null, REQUEST_SETTLEMENT))
                    .setAutoCancel(true)
                    .build()

            notifyIfPermitted(ID_SETTLEMENT, notification)
        }

        override fun cancelDailyReminder() = manager.cancel(ID_DAILY)

        private fun notifyIfPermitted(
            id: Int,
            notification: android.app.Notification,
        ) {
            if (!manager.areNotificationsEnabled()) return
            try {
                manager.notify(id, notification)
            } catch (denied: SecurityException) {
                // POST_NOTIFICATIONS was revoked between the check and the post. Nothing to recover:
                // the reminder is optional by design and the app stays silent until it is granted.
                manager.cancel(id)
            }
        }

        private fun openApp(
            destination: String?,
            requestCode: Int,
        ): PendingIntent {
            val intent =
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    destination?.let { putExtra(MainActivity.EXTRA_DESTINATION, it) }
                }
            return PendingIntent.getActivity(context, requestCode, intent, FLAGS)
        }

        private fun zeroSpendIntent(): PendingIntent {
            val intent =
                Intent(context, ZeroSpendReceiver::class.java)
                    .setAction(ZeroSpendReceiver.ACTION_MARK_ZERO_SPEND)
            return PendingIntent.getBroadcast(context, REQUEST_ZERO_SPEND, intent, FLAGS)
        }

        private companion object {
            const val ID_DAILY = 1
            const val ID_SETTLEMENT = 2
            const val ID_DEADLINE_BASE = 1_000
            const val REQUEST_OPEN = 10
            const val REQUEST_QUICK_ADD = 11
            const val REQUEST_ZERO_SPEND = 12
            const val REQUEST_SETTLEMENT = 13
            const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        }
    }
