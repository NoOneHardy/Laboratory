package ch.no1hardy.orbit7.core.domain.economy

import ch.no1hardy.orbit7.core.domain.model.Budget
import ch.no1hardy.orbit7.core.domain.money.Fraction
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.time.Week
import java.time.LocalDate

/**
 * Turns monthly budgets into the week's figure (`docs/02-game-design.md` §3.2).
 *
 * Budgets are set monthly because that is how people think about money, but settlement is weekly and
 * a week can straddle two months. The weekly figure is therefore an exact day-proration — *not* a
 * division by four:
 *
 * ```
 * weeklyBudget = Σ over the 7 days d:  monthlyBudget(d) / daysInMonth(d)
 * ```
 *
 * using the budget version valid on each day, with the rounding to minor units applied **once**, on
 * the sum. The intermediate sum is kept as an exact fraction so no Rappen is lost on the way.
 */
object ProrationCalculator {
    /** The prorated budget for one category over [week], honouring versioned budgets. */
    fun weeklyBudget(
        categoryId: Long,
        week: Week,
        budgets: List<Budget>,
    ): Money {
        val versions = budgets.filter { it.categoryId == categoryId }
        var accumulated = Fraction.ZERO
        for (day in week.days) {
            val monthly = versions.firstOrNull { it.coversDate(day) }?.amountPerMonth ?: Money.ZERO
            if (monthly.isZero) continue
            accumulated += Fraction(monthly.minor, day.lengthOfMonth().toLong())
        }
        return Money(accumulated.roundHalfUp())
    }

    /** The prorated budget of every category that has a budget version touching [week]. */
    fun weeklyBudgets(
        week: Week,
        budgets: List<Budget>,
    ): Map<Long, Money> =
        budgets
            .map { it.categoryId }
            .distinct()
            .associateWith { weeklyBudget(it, week, budgets) }
            .filterValues { !it.isZero }

    /** The monthly budget in force for [categoryId] on [date], or zero if none is. */
    fun monthlyBudget(
        categoryId: Long,
        date: LocalDate,
        budgets: List<Budget>,
    ): Money =
        budgets.firstOrNull { it.categoryId == categoryId && it.coversDate(date) }?.amountPerMonth
            ?: Money.ZERO

    /** The month-to-date figure a monthly budget prorates to over [days] days of [month]. */
    fun proratedOverDays(
        monthly: Money,
        days: Int,
        daysInMonth: Int,
    ): Money = Money(Fraction(monthly.minor * days, daysInMonth.toLong()).roundHalfUp())
}
