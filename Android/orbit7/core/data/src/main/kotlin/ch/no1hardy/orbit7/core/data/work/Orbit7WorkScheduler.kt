package ch.no1hardy.orbit7.core.data.work

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ch.no1hardy.orbit7.core.domain.repository.SettingsRepository
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers the three workers, and re-registers them on boot and on app start.
 *
 * All scheduling maths goes through the injected [Clock], like everything else that touches time.
 */
@Singleton
class Orbit7WorkScheduler
    @Inject
    constructor(
        private val workManager: WorkManager,
        private val settings: SettingsRepository,
        private val clock: Clock,
    ) {
        /** Called from `Application.onCreate` and from the boot receiver. */
        suspend fun scheduleAll() {
            scheduleWeeklySettlement()
            scheduleDailyReminder()
            scheduleContractDeadlines()
            runSettlementCatchUpNow()
        }

        /** Monday, shortly after the week closes. */
        fun scheduleWeeklySettlement() {
            val request =
                PeriodicWorkRequestBuilder<WeeklySettlementWorker>(Duration.ofDays(DAYS_PER_WEEK))
                    .setInitialDelay(delayUntilNextMonday())
                    .build()
            workManager.enqueueUniquePeriodicWork(
                WeeklySettlementWorker.UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        /**
         * The catch-up path: a one-shot settlement on app start, so a user returning after five weeks
         * sees their power the moment they open the app rather than next Monday.
         */
        fun runSettlementCatchUpNow() {
            workManager.enqueueUniqueWork(
                WeeklySettlementWorker.CATCH_UP_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<WeeklySettlementWorker>().build(),
            )
        }

        suspend fun scheduleDailyReminder() {
            val current = settings.settings()
            if (!current.dailyReminderEnabled) {
                workManager.cancelUniqueWork(DailyReminderWorker.UNIQUE_NAME)
                return
            }
            val request =
                PeriodicWorkRequestBuilder<DailyReminderWorker>(Duration.ofDays(1))
                    .setInitialDelay(delayUntil(current.dailyReminderTime))
                    .build()
            workManager.enqueueUniquePeriodicWork(
                DailyReminderWorker.UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun scheduleContractDeadlines() {
            val request =
                PeriodicWorkRequestBuilder<ContractDeadlineWorker>(Duration.ofDays(1))
                    .setInitialDelay(delayUntil(DEADLINE_CHECK_TIME))
                    .build()
            workManager.enqueueUniquePeriodicWork(
                ContractDeadlineWorker.UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        private fun delayUntil(time: LocalTime): Duration {
            val now = ZonedDateTime.now(clock)
            val target = now.with(time).let { if (it.isAfter(now)) it else it.plusDays(1) }
            return Duration.between(now, target)
        }

        private fun delayUntilNextMonday(): Duration {
            val now = ZonedDateTime.now(clock)
            val nextMonday = now.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).with(SETTLEMENT_TIME)
            return Duration.between(now, nextMonday)
        }

        private companion object {
            const val DAYS_PER_WEEK = 7L

            /** Early Monday: the week is closed, the phone is idle, and the payoff is waiting at breakfast. */
            val SETTLEMENT_TIME: LocalTime = LocalTime.of(3, 30)
            val DEADLINE_CHECK_TIME: LocalTime = LocalTime.of(9, 0)
        }
    }
