package ch.no1hardy.orbit7.feature.expenses

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import ch.no1hardy.orbit7.core.domain.test.FakeCategoryRepository
import ch.no1hardy.orbit7.core.domain.test.FakeClock
import ch.no1hardy.orbit7.core.domain.test.FakeExpenseRepository
import ch.no1hardy.orbit7.core.domain.test.FakeSettingsRepository
import ch.no1hardy.orbit7.core.domain.test.FakeZeroSpendRepository
import ch.no1hardy.orbit7.core.domain.test.aCategory
import ch.no1hardy.orbit7.core.domain.test.anExpense
import ch.no1hardy.orbit7.core.domain.test.chf
import ch.no1hardy.orbit7.core.domain.usecase.LogExpenseUseCase
import ch.no1hardy.orbit7.core.domain.usecase.UpdateExpenseUseCase
import ch.no1hardy.orbit7.core.testing.MainDispatcherRule
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** Every UiState transition of Quick Add (`docs/06-test-strategy.md` §5). */
class QuickAddViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = FakeClock().apply { setDate(LocalDate.of(2025, 4, 16)) }
    private val categories = FakeCategoryRepository(listOf(aCategory(id = 1), aCategory(id = 2, sortOrder = 1)))
    private val expenses = FakeExpenseRepository()
    private val zeroSpend = FakeZeroSpendRepository()
    private val settings = FakeSettingsRepository()

    @Test
    fun `initial state loads categories and preselects one`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            val state = viewModel.state.value
            state.loading.shouldBeFalse()
            state.categories shouldHaveSize 2
            state.selectedCategoryId shouldBe 1L
            state.date shouldBe LocalDate.of(2025, 4, 16)
            state.canSave.shouldBeFalse()
        }

    @Test
    fun `rappen-first digits build the amount from the right`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            listOf(1, 2, 5, 0).forEach(viewModel::onDigit)

            viewModel.state.value.amount shouldBe chf(12, 50)
        }

    @Test
    fun `with rappen-first off, digits fill the francs`() =
        runTest {
            settings.update { it.copy(rappenFirstInput = false) }
            val viewModel = viewModel()
            advanceUntilIdle()

            listOf(1, 2).forEach(viewModel::onDigit)

            viewModel.state.value.amount shouldBe chf(12)
        }

    @Test
    fun `backspace and clear walk the amount back`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            listOf(1, 2, 5, 0).forEach(viewModel::onDigit)

            viewModel.onBackspace()
            viewModel.state.value.amount shouldBe chf(1, 25)

            viewModel.onClear()
            viewModel.state.value.amountMinor shouldBe 0L
        }

    @Test
    fun `save is disabled at zero, and the reason is shown`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.state.value.canSave
                .shouldBeFalse()
            viewModel.state.value.showsAmountRequired
                .shouldBeTrue()

            viewModel.onDigit(5)
            viewModel.state.value.canSave
                .shouldBeTrue()
            viewModel.state.value.showsAmountRequired
                .shouldBeFalse()
        }

    @Test
    fun `three interactions save an expense`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onDigit(5) // amount
                viewModel.onCategorySelected(2) // category
                viewModel.onSave() // save
                advanceUntilIdle()

                val effect = awaitItem()
                effect.shouldBeInstanceOf<QuickAddEffect.Saved>()
                effect.isNew.shouldBeTrue()
            }
            expenses.current.single().categoryId shouldBe 2L
            expenses.current.single().amount shouldBe chf(0, 5)
        }

    @Test
    fun `a one-shot effect arrives exactly once`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onDigit(5)
                viewModel.onSave()
                advanceUntilIdle()

                awaitItem().shouldBeInstanceOf<QuickAddEffect.Saved>()
                expectNoEvents()
            }
        }

    @Test
    fun `logging on a marked day clears the zero-spend mark`() =
        runTest {
            zeroSpend.mark(
                LocalDate.of(2025, 4, 16),
                clock.instant(),
                ch.no1hardy.orbit7.core.domain.model.ZeroSpendSource.APP,
            )
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onDigit(5)
            viewModel.onSave()
            advanceUntilIdle()

            zeroSpend.isMarked(LocalDate.of(2025, 4, 16)).shouldBeFalse()
        }

    @Test
    fun `editing loads the existing expense and saves in place`() =
        runTest {
            val id = expenses.add(anExpense(chf(20), LocalDate.of(2025, 4, 15), categoryId = 2))
            val viewModel = viewModel(editingId = id)
            advanceUntilIdle()

            viewModel.state.value.isEditing
                .shouldBeTrue()
            viewModel.state.value.amount shouldBe chf(20)
            viewModel.state.value.selectedCategoryId shouldBe 2L

            viewModel.onClear()
            listOf(3, 0, 0, 0).forEach(viewModel::onDigit)
            viewModel.onSave()
            advanceUntilIdle()

            expenses.current shouldHaveSize 1
            expenses.current.single().amount shouldBe chf(30)
        }

    @Test
    fun `a date cannot be stepped into the future`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onDateStep(1)

            viewModel.state.value.date shouldBe LocalDate.of(2025, 4, 16)

            viewModel.onDateStep(-2)
            viewModel.state.value.date shouldBe LocalDate.of(2025, 4, 14)
        }

    private fun viewModel(editingId: Long? = null) =
        QuickAddViewModel(
            categories = categories,
            expenses = expenses,
            settings = settings,
            logExpense = LogExpenseUseCase(expenses, zeroSpend, clock),
            updateExpense = UpdateExpenseUseCase(expenses, zeroSpend, clock),
            clock = clock,
            savedStateHandle =
                SavedStateHandle(
                    editingId?.let { mapOf(QuickAddViewModel.ARG_EXPENSE_ID to it) } ?: emptyMap(),
                ),
        )
}
