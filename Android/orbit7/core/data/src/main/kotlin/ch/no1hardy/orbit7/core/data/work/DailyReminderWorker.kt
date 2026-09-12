package ch.no1hardy.orbit7.core.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ch.no1hardy.orbit7.core.data.notification.Orbit7Notifier
import ch.no1hardy.orbit7.core.domain.economy.ProrationCalculator
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.money.sum
import ch.no1hardy.orbit7.core.domain.repository.BudgetRepository
import ch.no1hardy.orbit7.core.domain.repository.ExpenseRepository
import ch.no1hardy.orbit7.core.domain.repository.SettingsRepository
import ch.no1hardy.orbit7.core.domain.time.Week
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import java.time.LocalDate

/**
 * The one scheduled notification of the day, and only if the app was not already used today.
 *
 * Its second action — "Nothing spent today" — writes a zero-spend mark without launching the app,
 * which is the cheapest possible way to produce a signal day and what keeps the pure-trust model
 * honest (`docs/03-screens.md`, Android surfaces).
 */
@HiltWorker
class DailyReminderWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted parameters: WorkerParameters,
        private val expenses: ExpenseRepository,
        private val budgets: BudgetRepository,
        private val settings: SettingsRepository,
        private val notifier: Orbit7Notifier,
        private val clock: Clock,
    ) : CoroutineWorker(context, parameters) {
        override suspend fun doWork(): Result {
            val current = settings.settings()
            if (!current.dailyReminderEnabled) {
                notifier.cancelDailyReminder()
                return Result.success()
            }

            val today = LocalDate.now(clock)
            if (current.lastAppUsageOn == today) return Result.success() // already used today: stay quiet

            val week = Week.containing(today, current.firstDayOfWeek)
            val budgetVersions = budgets.versionsCovering(week.start, week.endInclusive)
            val weeklyBudget =
                ProrationCalculator
                    .weeklyBudgets(week, budgetVersions)
                    .values
                    .fold(Money.ZERO) { acc, value -> acc + value }
            val spentThisWeek = expenses.between(week.start, week.endInclusive).map { it.amount }.sum()
            val spentToday = expenses.between(today, today).map { it.amount }.sum()

            notifier.postDailyReminder(
                remainingThisWeek = weeklyBudget - spentThisWeek,
                spentToday = spentToday,
            )
            return Result.success()
        }

        companion object {
            const val UNIQUE_NAME = "orbit7-daily-reminder"
        }
    }
