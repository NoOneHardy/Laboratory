package ch.no1hardy.orbit7.core.domain.economy

import ch.no1hardy.orbit7.core.domain.GameBalance
import ch.no1hardy.orbit7.core.domain.model.Expense
import ch.no1hardy.orbit7.core.domain.money.IntegerMath
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.money.sum
import ch.no1hardy.orbit7.core.domain.time.Week
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** A baseline and the cut it suggests, for one category. */
data class BaselineSuggestion(
    val categoryId: Long,
    val weeklyBaseline: Money,
    val weeklyTarget: Money,
    val suggestedMonthlyBudget: Money,
)

/**
 * The rolling baseline (`docs/02-game-design.md` §4.3).
 *
 * After 28 days of data in a category, the app computes the **rolling median** of the last 12
 * complete weeks of weekly spend — median, not mean, so one holiday grocery run does not move it.
 * The baseline is a ghost marker on every chart and drives a suggestion the user can accept, adjust
 * or dismiss. Nothing is ever applied automatically.
 */
class BaselineCalculator(
    private val balance: GameBalance = GameBalance.DEFAULT,
) {
    /**
     * The weekly baseline for [categoryId], or `null` while the category has fewer than
     * [GameBalance.baselineMinimumHistoryDays] days of history.
     */
    fun baseline(
        categoryId: Long,
        expenses: List<Expense>,
        today: LocalDate,
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ): Money? {
        val categoryExpenses = expenses.filter { it.categoryId == categoryId }
        val firstDay = categoryExpenses.minOfOrNull { it.occurredOn } ?: return null
        val historyDays = ChronoUnit.DAYS.between(firstDay, today)
        if (historyDays < balance.baselineMinimumHistoryDays) return null

        val weeklyTotals = weeklyTotals(categoryExpenses, today, firstDayOfWeek, firstDay)
        if (weeklyTotals.isEmpty()) return null
        return median(weeklyTotals)
    }

    /** The weekly spend of each of the complete weeks inside the rolling window, oldest first. */
    fun weeklySpendHistory(
        categoryId: Long,
        expenses: List<Expense>,
        today: LocalDate,
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ): List<Money> {
        val categoryExpenses = expenses.filter { it.categoryId == categoryId }
        val firstDay = categoryExpenses.minOfOrNull { it.occurredOn } ?: return emptyList()
        return weeklyTotals(categoryExpenses, today, firstDayOfWeek, firstDay).reversed()
    }

    /**
     * The suggestion for a category, or `null` when one must not be made:
     * - no baseline yet;
     * - the budget is already below the baseline (the user is winning there already);
     * - a suggestion for this category was dismissed less than 28 days ago.
     */
    fun suggestion(
        categoryId: Long,
        baseline: Money?,
        currentWeeklyBudget: Money,
        today: LocalDate,
        lastDismissedOn: LocalDate? = null,
    ): BaselineSuggestion? {
        if (baseline == null || !baseline.isPositive) return null
        if (currentWeeklyBudget.isPositive && currentWeeklyBudget <= baseline) return null
        if (lastDismissedOn != null &&
            ChronoUnit.DAYS.between(lastDismissedOn, today) < balance.suggestionCooldownDays
        ) {
            return null
        }

        val target =
            Money(
                IntegerMath.applyBasisPoints(
                    baseline.minor,
                    IntegerMath.ONE_BP - balance.suggestedCutBasisPoints,
                ),
            )
        val monthly =
            Money(
                IntegerMath.roundHalfUpDiv(
                    target.minor * today.lengthOfMonth(),
                    Week.DAYS.toLong(),
                ),
            )
        return BaselineSuggestion(
            categoryId = categoryId,
            weeklyBaseline = baseline,
            weeklyTarget = target,
            suggestedMonthlyBudget = monthly,
        )
    }

    private fun weeklyTotals(
        categoryExpenses: List<Expense>,
        today: LocalDate,
        firstDayOfWeek: DayOfWeek,
        firstDay: LocalDate,
    ): List<Money> {
        val currentWeek = Week.containing(today, firstDayOfWeek)
        val firstWeek = Week.containing(firstDay, firstDayOfWeek)
        return (1..balance.baselineWindowWeeks)
            .map { currentWeek.minusWeeks(it) }
            .filter { it >= firstWeek }
            .map { week -> categoryExpenses.filter { it.occurredOn in week }.map { it.amount }.sum() }
    }

    /** Median of a list of amounts; an even-sized list averages the two middle values, half-up. */
    private fun median(values: List<Money>): Money {
        val sorted = values.sortedBy { it.minor }
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            Money(IntegerMath.roundHalfUpDiv(sorted[middle - 1].minor + sorted[middle].minor, 2))
        }
    }
}
