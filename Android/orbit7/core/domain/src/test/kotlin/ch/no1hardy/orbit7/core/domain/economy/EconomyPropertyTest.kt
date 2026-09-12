package ch.no1hardy.orbit7.core.domain.economy

import ch.no1hardy.orbit7.core.domain.model.Expense
import ch.no1hardy.orbit7.core.domain.money.IntegerMath
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.test.FakeBudgetRepository
import ch.no1hardy.orbit7.core.domain.test.FakeClock
import ch.no1hardy.orbit7.core.domain.test.FakeEnergyRepository
import ch.no1hardy.orbit7.core.domain.test.FakeExpenseRepository
import ch.no1hardy.orbit7.core.domain.test.FakeSettingsRepository
import ch.no1hardy.orbit7.core.domain.test.FakeSettlementRepository
import ch.no1hardy.orbit7.core.domain.test.FakeZeroSpendRepository
import ch.no1hardy.orbit7.core.domain.test.Fixtures
import ch.no1hardy.orbit7.core.domain.test.aBudget
import ch.no1hardy.orbit7.core.domain.test.anExpense
import ch.no1hardy.orbit7.core.domain.test.chf
import ch.no1hardy.orbit7.core.domain.time.Week
import ch.no1hardy.orbit7.core.domain.usecase.SettleDueWeeksUseCase
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import io.kotest.property.forAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

/**
 * The invariants from `docs/06-test-strategy.md` §3, checked over generated histories.
 *
 * These are the properties that must hold for *every* possible week, not just for the examples
 * somebody thought of — a wrong proration or a farmable settlement is invisible for months.
 */
class EconomyPropertyTest {
    private val calculator = SettlementCalculator()
    private val monday = LocalDate.of(2025, 4, 7)
    private val week = Week(monday)
    private val settledAt: Instant = Instant.parse("2025-04-14T03:00:00Z")

    private val budgets =
        listOf(
            aBudget(categoryId = Fixtures.GROCERIES, monthly = chf(400)),
            aBudget(categoryId = Fixtures.EATING_OUT, monthly = chf(200), id = 2),
            aBudget(categoryId = Fixtures.TRANSPORT, monthly = chf(100), id = 3),
        )

    @Test
    @DisplayName("property 4: confidence stays in [0.4, 1.0] and the streak in [1.0, 1.5], always")
    fun `factors stay inside their bounds`() =
        runTest {
            forAll(Arb.int(-50..500), Arb.int(-50..500)) { signalDays, goodWeeks ->
                val confidence = calculator.confidenceBasisPoints(signalDays)
                val streak = calculator.streakBasisPoints(goodWeeks)
                confidence in 4_000..10_000 && streak in 10_000..15_000
            }
        }

    @Test
    @DisplayName("property 7: money arithmetic never loses a Rappen")
    fun `per-category surpluses sum to the total surplus`() =
        runTest {
            checkAll(expenseAmounts()) { amounts ->
                val result = calculator.settle(inputFor(expensesOf(amounts)))

                val categorySurplus = result.categories.sumOf { it.surplus.minor }
                val categoryOverdraw = result.categories.sumOf { it.overdraw.minor }
                val budgeted = result.categories.sumOf { it.budgeted.minor }
                val spent = result.categories.sumOf { it.spent.minor }

                // Surplus and overdraw partition the difference exactly, with nothing rounded away.
                (categorySurplus - categoryOverdraw) shouldBe (budgeted - spent)
                result.settlement.budgeted.minor shouldBe budgeted
                result.settlement.spent.minor shouldBe spent
            }
        }

    @Test
    @DisplayName("property 5: a silent week never earns more than the same week fully logged")
    fun `the anti-exploit property`() =
        runTest {
            checkAll(expenseAmounts(), Arb.int(0..10)) { amounts, goodWeeks ->
                val expenses = expensesOf(amounts)

                val silent =
                    calculator
                        .settle(
                            inputFor(expenses, marks = emptySet(), goodWeeks = goodWeeks),
                        ).settlement
                val logged =
                    calculator
                        .settle(
                            inputFor(expenses, marks = week.days.toSet(), goodWeeks = goodWeeks),
                        ).settlement

                // Identical spending; the only difference is whether the user produced signal days.
                silent.spent shouldBe logged.spent
                (silent.awardedEp <= logged.awardedEp).shouldBeTrue()
            }
        }

    @Test
    @DisplayName("more signal days never pay less")
    fun `confidence is monotonic in signal days`() =
        runTest {
            forAll(Arb.int(0..6)) { days ->
                calculator.confidenceBasisPoints(days) <= calculator.confidenceBasisPoints(days + 1)
            }
        }

    @Test
    @DisplayName("properties 1 and 3: the balance equals the ledger sum and is never negative")
    fun `the ledger is the balance`() =
        runTest {
            checkAll(Arb.list(Arb.long(0L..40_000L), 0..12)) { weeklySpends ->
                val world = world()
                weeklySpends.forEachIndexed { index, minor ->
                    if (minor > 0) {
                        world.expenses.add(anExpense(Money(minor), monday.plusWeeks(index.toLong())))
                    }
                }
                world.clock.setDate(monday.plusWeeks(weeklySpends.size.toLong()))
                world.settle()

                world.energy.balance() shouldBe world.energy.entries.sumOf { it.delta }
                world.energy.balance() shouldBeGreaterThanOrEqual 0
            }
        }

    @Test
    @DisplayName("property 2: settling the same weeks again changes nothing")
    fun `settlement is idempotent for any history`() =
        runTest {
            checkAll(Arb.list(Arb.long(0L..40_000L), 1..8)) { weeklySpends ->
                val world = world()
                weeklySpends.forEachIndexed { index, minor ->
                    if (minor > 0) {
                        world.expenses.add(anExpense(Money(minor), monday.plusWeeks(index.toLong())))
                    }
                }
                world.clock.setDate(monday.plusWeeks(weeklySpends.size.toLong()))

                world.settle()
                val balance = world.energy.balance()
                val entries = world.energy.entries.size
                repeat(3) { world.settle() }

                world.energy.balance() shouldBe balance
                world.energy.entries.size shouldBe entries
            }
        }

    @Test
    @DisplayName("property 6: catch-up equals settling week by week")
    fun `catch-up equals piecemeal for any history`() =
        runTest {
            checkAll(Arb.list(Arb.long(0L..40_000L), 1..8)) { weeklySpends ->
                val piecemeal = world()
                val away = world()
                weeklySpends.forEachIndexed { index, minor ->
                    if (minor > 0) {
                        val expense = anExpense(Money(minor), monday.plusWeeks(index.toLong()))
                        piecemeal.expenses.add(expense)
                        away.expenses.add(expense)
                    }
                }

                weeklySpends.indices.forEach { index ->
                    piecemeal.clock.setDate(monday.plusWeeks(index.toLong() + 1))
                    piecemeal.settle()
                }
                away.clock.setDate(monday.plusWeeks(weeklySpends.size.toLong()))
                away.settle()

                away.energy.balance() shouldBe piecemeal.energy.balance()
                away.settlements
                    .settlements()
                    .sortedBy { it.week }
                    .map { it.awardedEp } shouldBe
                    piecemeal.settlements
                        .settlements()
                        .sortedBy { it.week }
                        .map { it.awardedEp }
            }
        }

    @Test
    @DisplayName("rounding half-up is exact: the result is never more than half a unit from the quotient")
    fun `half-up division is within half a unit`() =
        runTest {
            checkAll(Arb.long(-1_000_000L..1_000_000L), Arb.long(1L..10_000L)) { numerator, denominator ->
                val rounded = IntegerMath.roundHalfUpDiv(numerator, denominator)
                val error = numerator - rounded * denominator

                (error * 2).shouldBeGreaterThanOrEqual(-denominator)
                (error * 2).shouldBeLessThanOrEqual(denominator)
            }
        }

    private fun expenseAmounts(): Arb<List<Long>> = Arb.list(Arb.long(0L..60_000L), 0..10)

    private fun expensesOf(amounts: List<Long>): List<Expense> =
        amounts.filter { it > 0 }.mapIndexed { index, minor ->
            anExpense(
                amount = Money(minor),
                on = monday.plusDays((index % Week.DAYS).toLong()),
                categoryId = (index % 3) + 1L,
                id = index.toLong() + 1,
            )
        }

    private fun inputFor(
        expenses: List<Expense>,
        marks: Set<LocalDate> = emptySet(),
        goodWeeks: Int = 0,
    ) = SettlementInput(
        week = week,
        budgets = budgets,
        expenses = expenses,
        zeroSpendDates = marks,
        consecutiveGoodWeeksBefore = goodWeeks,
        settledAt = settledAt,
    )

    private fun world(): World {
        val expenses = FakeExpenseRepository()
        val settlements = FakeSettlementRepository()
        val energy = FakeEnergyRepository()
        val clock = FakeClock()
        return World(
            expenses,
            settlements,
            energy,
            clock,
            SettleDueWeeksUseCase(
                expenses = expenses,
                budgets = FakeBudgetRepository(budgets.map { it.copy(validFrom = monday) }),
                zeroSpend = FakeZeroSpendRepository(),
                settlements = settlements,
                energy = energy,
                settings = FakeSettingsRepository(),
                calculator = calculator,
                clock = clock,
            ),
        )
    }

    private class World(
        val expenses: FakeExpenseRepository,
        val settlements: FakeSettlementRepository,
        val energy: FakeEnergyRepository,
        val clock: FakeClock,
        val settle: SettleDueWeeksUseCase,
    )
}
