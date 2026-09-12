package ch.no1hardy.orbit7.widget

import ch.no1hardy.orbit7.core.domain.economy.ProrationCalculator
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.money.sum
import ch.no1hardy.orbit7.core.domain.repository.BudgetRepository
import ch.no1hardy.orbit7.core.domain.repository.EnergyRepository
import ch.no1hardy.orbit7.core.domain.repository.ExpenseRepository
import ch.no1hardy.orbit7.core.domain.repository.SettingsRepository
import ch.no1hardy.orbit7.core.domain.time.Week
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Assembles the widget's state. Pure reads: the widget never writes anything. */
@Singleton
class Orbit7WidgetRepository
    @Inject
    constructor(
        private val energy: EnergyRepository,
        private val expenses: ExpenseRepository,
        private val budgets: BudgetRepository,
        private val settings: SettingsRepository,
        private val clock: Clock,
    ) {
        suspend fun widgetState(): WidgetState {
            val today = LocalDate.now(clock)
            val week = Week.containing(today, settings.settings().firstDayOfWeek)

            val weeklyBudget =
                ProrationCalculator
                    .weeklyBudgets(week, budgets.versionsCovering(week.start, week.endInclusive))
                    .values
                    .fold(Money.ZERO) { acc, value -> acc + value }
            val spentThisWeek = expenses.between(week.start, week.endInclusive).map { it.amount }.sum()
            val spentToday = expenses.between(today, today).map { it.amount }.sum()
            val balance = energy.balance()

            return WidgetState(
                balanceEp = balance,
                remainingThisWeekMinor = (weeklyBudget - spentThisWeek).minor,
                todaySpentMinor = spentToday.minor,
                weekProgress =
                    if (weeklyBudget.minor <= 0) {
                        0f
                    } else {
                        ((weeklyBudget - spentThisWeek).minor.toFloat() / weeklyBudget.minor.toFloat()).coerceIn(0f, 1f)
                    },
                hasData = weeklyBudget.isPositive || balance > 0,
            )
        }
    }
