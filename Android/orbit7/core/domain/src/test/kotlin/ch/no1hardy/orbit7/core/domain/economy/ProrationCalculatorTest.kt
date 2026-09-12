package ch.no1hardy.orbit7.core.domain.economy

import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.test.Fixtures
import ch.no1hardy.orbit7.core.domain.test.aBudget
import ch.no1hardy.orbit7.core.domain.test.aWeek
import ch.no1hardy.orbit7.core.domain.test.chf
import ch.no1hardy.orbit7.core.domain.time.Week
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Proration is fiddly enough to get wrong, so it is one of the first things pinned down
 * (`docs/06-test-strategy.md` §3).
 */
class ProrationCalculatorTest {
    @Test
    @DisplayName("a week inside one 30-day month prorates by exact days, not by dividing by four")
    fun `week inside one month`() {
        val week = aWeek(LocalDate.of(2025, 4, 14)) // Mon 14 Apr – Sun 20 Apr, April has 30 days
        val budgets = listOf(aBudget(monthly = chf(400)))

        // 400.00 × 7 / 30 = 93.333… → CHF 93.33, not 400 / 4 = 100.00
        ProrationCalculator.weeklyBudget(Fixtures.GROCERIES, week, budgets) shouldBe chf(93, 33)
    }

    @Test
    @DisplayName("a week straddling a month boundary uses each day's own month length")
    fun `week straddling months`() {
        // Mon 28 Apr – Sun 4 May: 3 days in April (30), 4 days in May (31).
        val week = aWeek(LocalDate.of(2025, 4, 28))
        val budgets = listOf(aBudget(monthly = chf(400)))

        // 3 × 400/30 + 4 × 400/31 = 40.00 + 51.6129… = 91.6129… → CHF 91.61
        ProrationCalculator.weeklyBudget(Fixtures.GROCERIES, week, budgets) shouldBe chf(91, 61)
    }

    @Test
    @DisplayName("February in a leap year has 29 days and the proration knows it")
    fun `leap february`() {
        val leap = aWeek(LocalDate.of(2024, 2, 5)) // Mon 5 Feb 2024, 29 days in the month
        val nonLeap = aWeek(LocalDate.of(2025, 2, 3)) // Mon 3 Feb 2025, 28 days
        val budgets = listOf(aBudget(monthly = chf(280)))

        // 280 × 7 / 29 = 67.586… → 67.59      280 × 7 / 28 = 70.00 exactly
        ProrationCalculator.weeklyBudget(Fixtures.GROCERIES, leap, budgets) shouldBe chf(67, 59)
        ProrationCalculator.weeklyBudget(Fixtures.GROCERIES, nonLeap, budgets) shouldBe chf(70)
    }

    @Test
    @DisplayName("a budget changed mid-week is honoured per day — versioned budgets stay reproducible")
    fun `budget version changes mid week`() {
        val week = aWeek(LocalDate.of(2025, 4, 14)) // Mon 14 – Sun 20 April
        val budgets =
            listOf(
                aBudget(monthly = chf(300), validFrom = LocalDate.of(2025, 1, 1), validTo = LocalDate.of(2025, 4, 16)),
                aBudget(monthly = chf(600), validFrom = LocalDate.of(2025, 4, 17), id = 2),
            )

        // 3 days at 300/30 = 30.00, 4 days at 600/30 = 80.00 → CHF 110.00
        ProrationCalculator.weeklyBudget(Fixtures.GROCERIES, week, budgets) shouldBe chf(110)
    }

    @Test
    @DisplayName("a week containing the DST changeover still gets exactly seven days of budget")
    fun `dst weeks have seven days of budget`() {
        val budgets = listOf(aBudget(monthly = chf(310)))
        // Europe/Zurich: clocks go forward on Sun 30 March 2025 and back on Sun 26 October 2025.
        val marchWeek = aWeek(LocalDate.of(2025, 3, 24))
        val octoberWeek = aWeek(LocalDate.of(2025, 10, 20))

        marchWeek.days.size shouldBe Week.DAYS
        octoberWeek.days.size shouldBe Week.DAYS
        // 310 × 7 / 31 = 70.00 in both directions; an hour lost or gained changes nothing.
        ProrationCalculator.weeklyBudget(Fixtures.GROCERIES, marchWeek, budgets) shouldBe chf(70)
        ProrationCalculator.weeklyBudget(Fixtures.GROCERIES, octoberWeek, budgets) shouldBe chf(70)
    }

    @Test
    fun `a category with no budget version prorates to zero`() {
        ProrationCalculator.weeklyBudget(
            categoryId = 99,
            week = aWeek(),
            budgets = listOf(aBudget()),
        ) shouldBe Money.ZERO
    }

    @Test
    @DisplayName("the worked example's three category budgets (docs 02 §3.6)")
    fun `worked example budgets`() {
        // The worked example uses a 30-day month.
        val week = aWeek(LocalDate.of(2025, 4, 14))
        val budgets =
            listOf(
                aBudget(categoryId = Fixtures.GROCERIES, monthly = chf(400)),
                aBudget(categoryId = Fixtures.EATING_OUT, monthly = chf(200), id = 2),
                aBudget(categoryId = Fixtures.TRANSPORT, monthly = chf(100), id = 3),
            )

        val prorated = ProrationCalculator.weeklyBudgets(week, budgets)

        prorated[Fixtures.GROCERIES] shouldBe chf(93, 33)
        prorated[Fixtures.EATING_OUT] shouldBe chf(46, 67)
        prorated[Fixtures.TRANSPORT] shouldBe chf(23, 33)
        prorated.values.sumOf { it.minor } shouldBe chf(163, 33).minor
    }
}
