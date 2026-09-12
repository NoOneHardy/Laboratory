package ch.no1hardy.orbit7.feature.budget

import app.cash.turbine.test
import ch.no1hardy.orbit7.core.domain.economy.BaselineCalculator
import ch.no1hardy.orbit7.core.domain.test.FakeBudgetRepository
import ch.no1hardy.orbit7.core.domain.test.FakeCategoryRepository
import ch.no1hardy.orbit7.core.domain.test.FakeClock
import ch.no1hardy.orbit7.core.domain.test.FakeExpenseRepository
import ch.no1hardy.orbit7.core.domain.test.FakeSettingsRepository
import ch.no1hardy.orbit7.core.domain.test.aBudget
import ch.no1hardy.orbit7.core.domain.test.aCategory
import ch.no1hardy.orbit7.core.domain.test.anExpense
import ch.no1hardy.orbit7.core.domain.test.chf
import ch.no1hardy.orbit7.core.domain.usecase.AcceptBaselineSuggestionUseCase
import ch.no1hardy.orbit7.core.domain.usecase.DismissBaselineSuggestionUseCase
import ch.no1hardy.orbit7.core.domain.usecase.ObserveBudgetsUseCase
import ch.no1hardy.orbit7.core.testing.MainDispatcherRule
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class BudgetsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2025, 4, 14)
    private val clock = FakeClock().apply { setDate(today) }
    private val categories = FakeCategoryRepository(listOf(aCategory(id = 1)))
    private val expenses = FakeExpenseRepository()
    private val budgets = FakeBudgetRepository(listOf(aBudget(monthly = chf(900))))
    private val settings = FakeSettingsRepository()

    @Test
    fun `before 28 days of data there is no baseline, and the screen says so`() =
        runTest {
            expenses.add(anExpense(chf(100), today.minusDays(3)))
            val viewModel = viewModel()

            viewModel.state.test {
                awaitItem()
                val loaded = awaitItem()

                loaded.rows
                    .single()
                    .weeklyBaseline
                    .shouldBeNull()
                loaded.anyBaselines.shouldBeFalse()
            }
        }

    @Test
    fun `a category with history gets a baseline and a suggestion`() =
        runTest {
            (1..6).forEach { week ->
                expenses.add(anExpense(chf(180), today.minusWeeks(week.toLong()), id = week.toLong()))
            }
            val viewModel = viewModel()

            viewModel.state.test {
                awaitItem()
                val loaded = awaitItem()
                val row = loaded.rows.single()

                row.weeklyBaseline shouldBe chf(180)
                // The monthly budget of 900 prorates to 210 per week in April, which is above the
                // baseline — so a suggestion is offered.
                row.suggestion.shouldNotBeNull()
                row.suggestion?.weeklyTarget shouldBe chf(162)
            }
        }

    @Test
    fun `accepting a suggestion opens a new budget version`() =
        runTest {
            (1..6).forEach { week ->
                expenses.add(anExpense(chf(180), today.minusWeeks(week.toLong()), id = week.toLong()))
            }
            val viewModel = viewModel()
            advanceUntilIdle()
            val suggestion =
                viewModel.state.value.rows
                    .single()
                    .suggestion!!

            viewModel.onAcceptSuggestion(1, suggestion.suggestedMonthlyBudget)
            advanceUntilIdle()

            val versions = budgets.allVersions().sortedBy { it.validFrom }
            versions.size shouldBe 2
            versions.last().amountPerMonth shouldBe suggestion.suggestedMonthlyBudget
            versions.first().validTo shouldBe today.minusDays(1)
        }

    @Test
    fun `dismissing a suggestion silences it for 28 days`() =
        runTest {
            (1..6).forEach { week ->
                expenses.add(anExpense(chf(180), today.minusWeeks(week.toLong()), id = week.toLong()))
            }
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onDismissSuggestion(1)
            advanceUntilIdle()

            viewModel.state.value.rows
                .single()
                .suggestion
                .shouldBeNull()
            settings.settings().dismissedSuggestions[1] shouldBe today
        }

    @Test
    fun `the period toggle switches every figure between month and week`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.state.value.period shouldBe BudgetPeriod.MONTH
            viewModel.onPeriodChanged(BudgetPeriod.WEEK)
            advanceUntilIdle()
            viewModel.state.value.period shouldBe BudgetPeriod.WEEK
            // 900 × 7 / 30 = 210.00
            viewModel.state.value.rows
                .single()
                .weeklyBudget shouldBe chf(210)
        }

    private fun viewModel() =
        BudgetsViewModel(
            observeBudgets =
                ObserveBudgetsUseCase(
                    categories,
                    budgets,
                    expenses,
                    settings,
                    BaselineCalculator(),
                    clock,
                ),
            budgets = budgets,
            acceptSuggestion = AcceptBaselineSuggestionUseCase(budgets, clock),
            dismissSuggestion = DismissBaselineSuggestionUseCase(settings, clock),
            clock = clock,
        )
}
