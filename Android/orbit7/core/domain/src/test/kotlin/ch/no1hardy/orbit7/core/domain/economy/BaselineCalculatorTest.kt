package ch.no1hardy.orbit7.core.domain.economy

import ch.no1hardy.orbit7.core.domain.model.Expense
import ch.no1hardy.orbit7.core.domain.test.Fixtures
import ch.no1hardy.orbit7.core.domain.test.anExpense
import ch.no1hardy.orbit7.core.domain.test.chf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BaselineCalculatorTest {
    private val calculator = BaselineCalculator()
    private val today = LocalDate.of(2025, 4, 14) // a Monday

    @Test
    fun `fewer than 28 days of history produces no baseline`() {
        val expenses = weeklySpend(weeksBack = 3, amounts = listOf(chf(100), chf(100), chf(100)))

        calculator.baseline(Fixtures.GROCERIES, expenses, today).shouldBeNull()
    }

    @Test
    @DisplayName("the baseline is the median, so one holiday grocery run does not move it")
    fun `median not mean`() {
        // Five complete weeks: four ordinary ones and one outlier.
        val expenses =
            weeklySpend(
                weeksBack = 5,
                amounts = listOf(chf(180), chf(175), chf(185), chf(180), chf(900)),
            )

        val baseline = calculator.baseline(Fixtures.GROCERIES, expenses, today)

        // Median of [180, 175, 185, 180, 900] is 180. The mean would be 324.
        baseline shouldBe chf(180)
    }

    @Test
    fun `an even number of weeks averages the two middle values`() {
        val expenses = weeklySpend(weeksBack = 4, amounts = listOf(chf(100), chf(120), chf(140), chf(160)))

        calculator.baseline(Fixtures.GROCERIES, expenses, today) shouldBe chf(130)
    }

    @Test
    fun `only the last twelve complete weeks are considered`() {
        val amounts = List(20) { index -> if (index < 8) chf(1000) else chf(100) }
        val expenses = weeklySpend(weeksBack = 20, amounts = amounts)

        // The eight ancient CHF 1000 weeks fall outside the window; the median is the recent level.
        calculator.baseline(Fixtures.GROCERIES, expenses, today) shouldBe chf(100)
    }

    @Test
    fun `a suggestion proposes a ten percent cut on the weekly baseline`() {
        val suggestion =
            calculator.suggestion(
                categoryId = Fixtures.GROCERIES,
                baseline = chf(180),
                currentWeeklyBudget = chf(200),
                today = today,
            )

        suggestion.shouldNotBeNull()
        suggestion.weeklyBaseline shouldBe chf(180)
        suggestion.weeklyTarget shouldBe chf(162)
        // April has 30 days: 162 × 30 / 7 = 694.29 per month.
        suggestion.suggestedMonthlyBudget shouldBe chf(694, 29)
    }

    @Test
    @DisplayName("no suggestion when the budget is already below the baseline — the user is winning")
    fun `suggestion suppressed when budget already under baseline`() {
        calculator
            .suggestion(
                categoryId = Fixtures.GROCERIES,
                baseline = chf(180),
                currentWeeklyBudget = chf(150),
                today = today,
            ).shouldBeNull()
    }

    @Test
    fun `a dismissed suggestion stays dismissed for 28 days and returns on the 28th`() {
        val dismissedYesterday = today.minusDays(1)
        calculator.suggestion(Fixtures.GROCERIES, chf(180), chf(200), today, dismissedYesterday).shouldBeNull()

        val dismissed27DaysAgo = today.minusDays(27)
        calculator.suggestion(Fixtures.GROCERIES, chf(180), chf(200), today, dismissed27DaysAgo).shouldBeNull()

        val dismissed28DaysAgo = today.minusDays(28)
        calculator.suggestion(Fixtures.GROCERIES, chf(180), chf(200), today, dismissed28DaysAgo).shouldNotBeNull()
    }

    @Test
    fun `no baseline means no suggestion`() {
        calculator
            .suggestion(Fixtures.GROCERIES, baseline = null, currentWeeklyBudget = chf(200), today = today)
            .shouldBeNull()
    }

    @Test
    fun `the sparkline reports the window oldest first`() {
        val expenses = weeklySpend(weeksBack = 5, amounts = listOf(chf(10), chf(20), chf(30), chf(40), chf(50)))

        val history = calculator.weeklySpendHistory(Fixtures.GROCERIES, expenses, today)

        history shouldBe listOf(chf(10), chf(20), chf(30), chf(40), chf(50))
    }

    /**
     * Builds one expense per complete week before [today], oldest first in [amounts] — index 0 is
     * the week furthest back.
     */
    private fun weeklySpend(
        weeksBack: Int,
        amounts: List<ch.no1hardy.orbit7.core.domain.money.Money>,
    ): List<Expense> =
        amounts.mapIndexed { index, amount ->
            anExpense(
                amount = amount,
                on = today.minusWeeks((weeksBack - index).toLong()),
                categoryId = Fixtures.GROCERIES,
                id = index.toLong() + 1,
            )
        }
}
