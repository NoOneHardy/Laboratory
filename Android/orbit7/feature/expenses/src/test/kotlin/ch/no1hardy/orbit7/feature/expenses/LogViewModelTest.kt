package ch.no1hardy.orbit7.feature.expenses

import app.cash.turbine.test
import ch.no1hardy.orbit7.core.domain.test.FakeCategoryRepository
import ch.no1hardy.orbit7.core.domain.test.FakeClock
import ch.no1hardy.orbit7.core.domain.test.FakeExpenseRepository
import ch.no1hardy.orbit7.core.domain.test.aCategory
import ch.no1hardy.orbit7.core.domain.test.anExpense
import ch.no1hardy.orbit7.core.domain.test.chf
import ch.no1hardy.orbit7.core.testing.MainDispatcherRule
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class LogViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = FakeClock().apply { setDate(LocalDate.of(2025, 4, 16)) }
    private val expenses = FakeExpenseRepository()
    private val categories = FakeCategoryRepository(listOf(aCategory(id = 1), aCategory(id = 2, sortOrder = 1)))

    @Test
    fun `an empty log is empty, not loading forever`() =
        runTest {
            val viewModel = LogViewModel(expenses, categories, clock)

            viewModel.state.test {
                awaitItem() // initial
                val loaded = awaitItem()
                loaded.loading shouldBe false
                loaded.isEmpty.shouldBeTrue()
                loaded.isFilteredEmpty shouldBe false
            }
        }

    @Test
    fun `entries group by day and month with their totals`() =
        runTest {
            expenses.add(anExpense(chf(10), LocalDate.of(2025, 4, 16)))
            expenses.add(anExpense(chf(5), LocalDate.of(2025, 4, 16), id = 2))
            expenses.add(anExpense(chf(20), LocalDate.of(2025, 4, 15), id = 3))
            val viewModel = LogViewModel(expenses, categories, clock)

            viewModel.state.test {
                awaitItem()
                val loaded = awaitItem()

                loaded.months shouldHaveSize 1
                loaded.months.single().total shouldBe chf(35)
                loaded.months.single().days shouldHaveSize 2
                // Newest first.
                loaded.months
                    .single()
                    .days
                    .first()
                    .date shouldBe LocalDate.of(2025, 4, 16)
                loaded.months
                    .single()
                    .days
                    .first()
                    .total shouldBe chf(15)
            }
        }

    @Test
    fun `a filtered-empty log says something different and can be cleared`() =
        runTest {
            expenses.add(anExpense(chf(10), LocalDate.of(2025, 4, 16), categoryId = 1))
            val viewModel = LogViewModel(expenses, categories, clock)
            advanceUntilIdle()

            viewModel.onCategoryFilter(2)
            advanceUntilIdle()

            viewModel.state.value.isFilteredEmpty
                .shouldBeTrue()

            viewModel.onClearFilters()
            advanceUntilIdle()
            viewModel.state.value.isEmpty shouldBe false
        }

    @Test
    fun `deleting offers undo, and undo restores the row`() =
        runTest {
            val id = expenses.add(anExpense(chf(10), LocalDate.of(2025, 4, 16)))
            val viewModel = LogViewModel(expenses, categories, clock)
            advanceUntilIdle()
            val expense = expenses.current.single { it.id == id }

            viewModel.effects.test {
                viewModel.onDelete(expense)
                advanceUntilIdle()

                val effect = awaitItem()
                effect.shouldBeInstanceOf<LogEffect.Deleted>()
                expenses.current shouldHaveSize 0

                viewModel.onUndoDelete(effect.expense)
                advanceUntilIdle()
                expenses.current shouldHaveSize 1
                expenses.current.single().amount shouldBe chf(10)
            }
        }
}
