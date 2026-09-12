package ch.no1hardy.orbit7.core.domain.usecase

import ch.no1hardy.orbit7.core.domain.economy.SettlementCalculator
import ch.no1hardy.orbit7.core.domain.model.EnergyReason
import ch.no1hardy.orbit7.core.domain.model.ZeroSpendSource
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.test.FakeBudgetRepository
import ch.no1hardy.orbit7.core.domain.test.FakeClock
import ch.no1hardy.orbit7.core.domain.test.FakeEnergyRepository
import ch.no1hardy.orbit7.core.domain.test.FakeExpenseRepository
import ch.no1hardy.orbit7.core.domain.test.FakeSettingsRepository
import ch.no1hardy.orbit7.core.domain.test.FakeSettlementRepository
import ch.no1hardy.orbit7.core.domain.test.FakeZeroSpendRepository
import ch.no1hardy.orbit7.core.domain.test.aBudget
import ch.no1hardy.orbit7.core.domain.test.anExpense
import ch.no1hardy.orbit7.core.domain.test.chf
import ch.no1hardy.orbit7.core.domain.time.Week
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SettleDueWeeksUseCaseTest {
    private val onboarding = LocalDate.of(2025, 4, 7) // a Monday: budgeting starts here
    private val clock = FakeClock()
    private val expenses = FakeExpenseRepository()
    private val zeroSpend = FakeZeroSpendRepository()
    private val settlements = FakeSettlementRepository()
    private val energy = FakeEnergyRepository()

    private val settle = useCase(expenses, zeroSpend, settlements, energy, clock, onboarding)

    @Test
    fun `nothing to settle before anything at all has happened`() =
        runTest {
            val noBudgets =
                SettleDueWeeksUseCase(
                    expenses,
                    FakeBudgetRepository(),
                    zeroSpend,
                    settlements,
                    energy,
                    FakeSettingsRepository(),
                    SettlementCalculator(),
                    clock,
                )

            noBudgets() shouldHaveSize 0
        }

    @Test
    fun `the current, incomplete week is never settled`() =
        runTest {
            clock.setDate(LocalDate.of(2025, 4, 16)) // a Wednesday
            expenses.add(anExpense(chf(20), LocalDate.of(2025, 4, 14)))

            val settled = settle()

            settled.map { it.week } shouldContain Week(onboarding)
            settled.map { it.week } shouldNotContain Week(LocalDate.of(2025, 4, 14))
        }

    @Test
    @DisplayName("settling twice produces one settlement row and one ledger entry")
    fun `settlement is idempotent`() =
        runTest {
            clock.setDate(LocalDate.of(2025, 4, 14))
            expenses.add(anExpense(chf(20), LocalDate.of(2025, 4, 8)))

            val first = settle()
            val balanceAfterFirst = energy.balance()
            val second = settle()

            first shouldHaveSize 1
            second shouldHaveSize 0
            settlements.settlements() shouldHaveSize 1
            energy.entries shouldHaveSize 1
            energy.balance() shouldBe balanceAfterFirst
        }

    @Test
    @DisplayName("the economy starts at onboarding: a week with no spend at all still settles, at ×0.4")
    fun `silent weeks settle at the confidence floor`() =
        runTest {
            clock.setDate(LocalDate.of(2025, 4, 14))

            val settled = settle().single()

            settled.signalDays shouldBe 0
            settled.confidenceBasisPoints shouldBe 4_000
            settled.awardedEp shouldBeGreaterThan 0
        }

    @Test
    @DisplayName("the user who was away for five weeks: catch-up settles every missed week, oldest first")
    fun `catch up settles all missed weeks`() =
        runTest {
            val start = LocalDate.of(2025, 3, 10) // a Monday
            val awayExpenses = FakeExpenseRepository()
            repeat(5) { weekIndex -> awayExpenses.add(anExpense(chf(30), start.plusWeeks(weekIndex.toLong()))) }
            val awaySettlements = FakeSettlementRepository()
            val awayEnergy = FakeEnergyRepository()
            val awayClock = FakeClock().apply { setDate(start.plusWeeks(5)) }

            val settled =
                useCase(awayExpenses, FakeZeroSpendRepository(), awaySettlements, awayEnergy, awayClock, start)()

            settled shouldHaveSize 5
            settled.map { it.week } shouldBe (0 until 5).map { Week(start.plusWeeks(it.toLong())) }
            awayEnergy.entries shouldHaveSize 5
            awayEnergy.balance() shouldBeGreaterThan 0
        }

    @Test
    @DisplayName("settling one week at a time equals settling the same weeks in one catch-up pass")
    fun `piecemeal equals catch-up`() =
        runTest {
            val start = LocalDate.of(2025, 3, 10)
            val amounts = listOf(chf(30), chf(120), chf(10), chf(95))

            val weekly = scenario(start, amounts)
            repeat(4) { week ->
                weekly.clock.setDate(start.plusWeeks(week.toLong() + 1))
                weekly.settle()
            }

            val away = scenario(start, amounts)
            away.clock.setDate(start.plusWeeks(4))
            away.settle()

            away.energy.balance() shouldBe weekly.energy.balance()
            away.settlements
                .settlements()
                .sortedBy { it.week }
                .map { it.awardedEp } shouldBe
                weekly.settlements
                    .settlements()
                    .sortedBy { it.week }
                    .map { it.awardedEp }
        }

    @Test
    fun `a zero-spend mark is a signal day even when nothing was spent`() =
        runTest {
            zeroSpend.mark(LocalDate.of(2025, 4, 8), clock.instant(), ZeroSpendSource.APP)
            clock.setDate(LocalDate.of(2025, 4, 14))

            settle().single().signalDays shouldBe 1
        }

    @Test
    @DisplayName("a bad week can pay nothing, but never a debt — the balance floors at zero")
    fun `the balance is floored at zero`() =
        runTest {
            expenses.add(anExpense(chf(900), LocalDate.of(2025, 4, 8)))
            clock.setDate(LocalDate.of(2025, 4, 14))

            // Weekly budget 93.33, spend 900.00 → overdraw 806.67, vented at half rate = 403 EP.
            settle().single().awardedEp shouldBe -403
            energy.balance() shouldBe 0
        }

    @Test
    fun `a bad week vents against the banked balance but never past zero`() =
        runTest {
            zeroSpend.mark(LocalDate.of(2025, 4, 8), clock.instant(), ZeroSpendSource.APP)
            clock.setDate(LocalDate.of(2025, 4, 14))
            settle()
            val banked = energy.balance()
            banked shouldBeGreaterThan 0

            expenses.add(anExpense(chf(2_000), LocalDate.of(2025, 4, 15)))
            clock.setDate(LocalDate.of(2025, 4, 21))
            settle()

            energy.balance() shouldBe 0
            energy.entries.last().reason shouldBe EnergyReason.OVERDRAW
        }

    @Test
    @DisplayName("the anti-exploit case: the same week logged pays more than the same week in silence")
    fun `logging a week beats staying silent`() =
        runTest {
            val week = LocalDate.of(2025, 4, 7)
            expenses.add(anExpense(chf(20), week))
            clock.setDate(LocalDate.of(2025, 4, 14))
            settle()
            val quiet = settlements.settlements().single().awardedEp

            val loud = scenario(week, listOf(chf(20)))
            (1..6).forEach { offset ->
                loud.zeroSpend.mark(week.plusDays(offset.toLong()), loud.clock.instant(), ZeroSpendSource.APP)
            }
            loud.clock.setDate(LocalDate.of(2025, 4, 14))
            loud.settle()

            loud.settlements
                .settlements()
                .single()
                .awardedEp shouldBeGreaterThan quiet
        }

    private fun useCase(
        expenses: FakeExpenseRepository,
        zeroSpend: FakeZeroSpendRepository,
        settlements: FakeSettlementRepository,
        energy: FakeEnergyRepository,
        clock: FakeClock,
        budgetFrom: LocalDate,
    ) = SettleDueWeeksUseCase(
        expenses = expenses,
        budgets = FakeBudgetRepository(listOf(aBudget(monthly = chf(400), validFrom = budgetFrom))),
        zeroSpend = zeroSpend,
        settlements = settlements,
        energy = energy,
        settings = FakeSettingsRepository(),
        calculator = SettlementCalculator(),
        clock = clock,
    )

    /** An independent world: its own repositories, its own clock, the same budget. */
    private suspend fun scenario(
        start: LocalDate,
        weeklyAmounts: List<Money>,
    ): Scenario {
        val expenses = FakeExpenseRepository()
        weeklyAmounts.forEachIndexed { index, amount ->
            expenses.add(anExpense(amount, start.plusWeeks(index.toLong())))
        }
        val zeroSpend = FakeZeroSpendRepository()
        val settlements = FakeSettlementRepository()
        val energy = FakeEnergyRepository()
        val clock = FakeClock()
        return Scenario(
            expenses,
            zeroSpend,
            settlements,
            energy,
            clock,
            useCase(expenses, zeroSpend, settlements, energy, clock, start),
        )
    }

    private class Scenario(
        val expenses: FakeExpenseRepository,
        val zeroSpend: FakeZeroSpendRepository,
        val settlements: FakeSettlementRepository,
        val energy: FakeEnergyRepository,
        val clock: FakeClock,
        val settle: SettleDueWeeksUseCase,
    )
}
