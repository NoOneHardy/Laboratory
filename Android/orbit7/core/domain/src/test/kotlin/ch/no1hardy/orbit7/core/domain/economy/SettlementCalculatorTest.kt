package ch.no1hardy.orbit7.core.domain.economy

import ch.no1hardy.orbit7.core.domain.GameBalance
import ch.no1hardy.orbit7.core.domain.test.Fixtures
import ch.no1hardy.orbit7.core.domain.test.aBudget
import ch.no1hardy.orbit7.core.domain.test.aWeek
import ch.no1hardy.orbit7.core.domain.test.anExpense
import ch.no1hardy.orbit7.core.domain.test.chf
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant
import java.time.LocalDate

class SettlementCalculatorTest {
    private val calculator = SettlementCalculator(GameBalance.DEFAULT)
    private val week = aWeek(LocalDate.of(2025, 4, 14)) // Mon 14 – Sun 20 April, a 30-day month
    private val settledAt = Instant.parse("2025-04-21T03:00:00Z")

    private val workedExampleBudgets =
        listOf(
            aBudget(categoryId = Fixtures.GROCERIES, monthly = chf(400)),
            aBudget(categoryId = Fixtures.EATING_OUT, monthly = chf(200), id = 2),
            aBudget(categoryId = Fixtures.TRANSPORT, monthly = chf(100), id = 3),
        )

    @Test
    @DisplayName("the worked example from docs 02 §3.6, digit for digit")
    fun `worked example`() {
        val expenses =
            listOf(
                anExpense(chf(78, 20), week.start, Fixtures.GROCERIES),
                anExpense(chf(61), week.start.plusDays(1), Fixtures.EATING_OUT),
                anExpense(chf(12, 40), week.start.plusDays(2), Fixtures.TRANSPORT),
            )
        // Six signal days: three days carry an expense, three more are marked zero-spend.
        val marks = setOf(week.start.plusDays(3), week.start.plusDays(4), week.start.plusDays(5))

        val result =
            calculator.settle(
                SettlementInput(
                    week = week,
                    budgets = workedExampleBudgets,
                    expenses = expenses,
                    zeroSpendDates = marks,
                    consecutiveGoodWeeksBefore = 3,
                    settledAt = settledAt,
                ),
            )
        val settlement = result.settlement

        settlement.signalDays shouldBe 6
        settlement.confidenceBasisPoints shouldBe 9_143 // 0.4 + 0.6 × 6/7 = 0.914
        settlement.streakBasisPoints shouldBe 11_500 // three good weeks → ×1.15
        settlement.baseEp shouldBe 26 // surplus 15.13 + 10.93 = 26.06 CHF
        settlement.ventedEp shouldBe 7 // overdraw 14.33 × 0.5 = 7.165 EP
        settlement.awardedEp shouldBe 20 // round(26 × 0.914 × 1.15) − 7 = 27 − 7
        settlement.budgeted shouldBe chf(163, 33)
        settlement.spent shouldBe chf(151, 60)
        // One category was overdrawn, yet the week as a whole came in under budget: the streak advances.
        settlement.goodWeek.shouldBeTrue()
        settlement.bestCategoryId shouldBe Fixtures.GROCERIES
        settlement.worstCategoryId shouldBe Fixtures.EATING_OUT
    }

    @Test
    fun `an all-surplus week vents nothing`() {
        val result = calculator.settle(input(expenses = listOf(anExpense(chf(10), week.start))))

        result.settlement.ventedEp shouldBe 0
        result.settlement.baseEp shouldBe 153 // 163.33 − 10.00 = 153.33 CHF of surplus
        result.settlement.goodWeek.shouldBeTrue()
    }

    @Test
    fun `an all-overdraw week generates no base power and vents at half rate`() {
        val result =
            calculator.settle(
                input(
                    expenses = listOf(anExpense(chf(263, 33), week.start, Fixtures.GROCERIES)),
                    consecutiveGoodWeeksBefore = 4,
                ),
            )

        result.settlement.baseEp shouldBe 70 // the two untouched categories still generate surplus
        result.settlement.ventedEp shouldBe 85 // (263.33 − 93.33) × 0.5
        result.settlement.awardedEp shouldBeLessThan 0
        result.settlement.goodWeek shouldBe false
    }

    @Test
    @DisplayName("a silent week and a week of seven zero-spend marks must not settle the same")
    fun `silence is not the same as a marked quiet week`() {
        val silent = calculator.settle(input(expenses = emptyList(), marks = emptySet()))
        val marked = calculator.settle(input(expenses = emptyList(), marks = week.days.toSet()))

        silent.settlement.signalDays shouldBe 0
        marked.settlement.signalDays shouldBe 7
        silent.settlement.confidenceBasisPoints shouldBe 4_000
        marked.settlement.confidenceBasisPoints shouldBe 10_000
        // Both weeks spent nothing, but the logged one pays the full amount and silence does not.
        marked.settlement.awardedEp shouldBeGreaterThan silent.settlement.awardedEp
        silent.settlement.awardedEp shouldBe 65 // 163 EP × 0.4 — never zero, never full
        marked.settlement.awardedEp shouldBe 163
    }

    @ParameterizedTest(name = "{0} signal days → {1} bp")
    @CsvSource("0,4000", "1,4857", "2,5714", "3,6571", "4,7429", "5,8286", "6,9143", "7,10000")
    fun `the confidence factor spans 0,4 to 1,0`(
        signalDays: Int,
        expectedBasisPoints: Long,
    ) {
        calculator.confidenceBasisPoints(signalDays) shouldBe expectedBasisPoints
    }

    @Test
    fun `a day with both an expense and a mark counts as one signal day`() {
        val day = week.start.plusDays(2)
        val result =
            calculator.settle(
                input(expenses = listOf(anExpense(chf(5), day)), marks = setOf(day)),
            )

        result.settlement.signalDays shouldBe 1
    }

    @Test
    fun `marks outside the week are ignored`() {
        val result =
            calculator.settle(
                input(expenses = emptyList(), marks = setOf(week.start.minusDays(1), week.endInclusive.plusDays(1))),
            )

        result.settlement.signalDays shouldBe 0
    }

    @ParameterizedTest(name = "{0} consecutive good weeks → {1} bp")
    @CsvSource("0,10000", "1,10500", "3,11500", "9,14500", "10,15000", "11,15000", "40,15000")
    fun `the streak multiplier builds up and caps at one and a half`(
        goodWeeks: Int,
        expected: Long,
    ) {
        calculator.streakBasisPoints(goodWeeks) shouldBe expected
    }

    @Test
    fun `expenses outside the week never count`() {
        val result =
            calculator.settle(
                input(
                    expenses =
                        listOf(
                            anExpense(chf(500), week.start.minusDays(1)),
                            anExpense(chf(500), week.endInclusive.plusDays(1)),
                        ),
                ),
            )

        result.settlement.spent.isZero
            .shouldBeTrue()
    }

    @Test
    fun `spending in a category with no budget is pure overdraw`() {
        val result =
            calculator.settle(
                input(expenses = listOf(anExpense(chf(20), week.start, categoryId = 42))),
            )

        result.categories.first { it.categoryId == 42L }.overdraw shouldBe chf(20)
        result.settlement.ventedEp shouldBe 10
    }

    private fun input(
        expenses: List<ch.no1hardy.orbit7.core.domain.model.Expense> = emptyList(),
        marks: Set<LocalDate> = emptySet(),
        consecutiveGoodWeeksBefore: Int = 0,
    ) = SettlementInput(
        week = week,
        budgets = workedExampleBudgets,
        expenses = expenses,
        zeroSpendDates = marks,
        consecutiveGoodWeeksBefore = consecutiveGoodWeeksBefore,
        settledAt = settledAt,
    )
}
