package ch.no1hardy.orbit7.feature.settings

import app.cash.turbine.test
import ch.no1hardy.orbit7.core.domain.model.CategoryKey
import ch.no1hardy.orbit7.core.domain.model.StationModuleKey
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.test.FakeBudgetRepository
import ch.no1hardy.orbit7.core.domain.test.FakeCategoryRepository
import ch.no1hardy.orbit7.core.domain.test.FakeClock
import ch.no1hardy.orbit7.core.domain.test.FakeSettingsRepository
import ch.no1hardy.orbit7.core.domain.test.FakeStationRepository
import ch.no1hardy.orbit7.core.testing.MainDispatcherRule
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class OnboardingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2025, 4, 14)
    private val clock = FakeClock().apply { setDate(today) }
    private val categories = FakeCategoryRepository()
    private val budgets = FakeBudgetRepository()
    private val station = FakeStationRepository()
    private val settings = FakeSettingsRepository()

    @Test
    fun `the three steps advance in order`() =
        runTest {
            val viewModel = viewModel()

            viewModel.state.value.step shouldBe OnboardingStep.PITCH
            viewModel.onNext()
            viewModel.state.value.step shouldBe OnboardingStep.CATEGORIES
            viewModel.onNext()
            viewModel.state.value.step shouldBe OnboardingStep.REMINDER
        }

    @Test
    fun `skipping applies the defaults, seeds categories and lights the reactor`() =
        runTest {
            val viewModel = viewModel()

            viewModel.completed.test {
                viewModel.onSkip()
                advanceUntilIdle()
                awaitItem()
            }

            categories.categories() shouldHaveSize 8
            budgets.allVersions() shouldHaveSize 8
            // One module already lit, so the first Bridge is never an empty screen.
            station.level(StationModuleKey.REACTOR_CORE) shouldBe 1
            settings.settings().onboardingCompleted.shouldBeTrue()
        }

    @Test
    fun `deselected categories are not created`() =
        runTest {
            val viewModel = viewModel()
            viewModel.onCategoryToggled(CategoryKey.SUBSCRIPTIONS)
            viewModel.onCategoryToggled(CategoryKey.HEALTH)

            viewModel.completed.test {
                viewModel.onSkip()
                advanceUntilIdle()
                awaitItem()
            }

            categories.categories().mapNotNull { it.key } shouldHaveSize 6
        }

    @Test
    fun `an edited budget is the one that gets stored`() =
        runTest {
            val viewModel = viewModel()
            viewModel.onBudgetChanged(CategoryKey.GROCERIES, 450)

            viewModel.completed.test {
                viewModel.onSkip()
                advanceUntilIdle()
                awaitItem()
            }

            val groceries = categories.categories().first { it.key == CategoryKey.GROCERIES }
            val budget = budgets.allVersions().first { it.categoryId == groceries.id }
            budget.amountPerMonth shouldBe Money.ofMajor(450)
            budget.validFrom shouldBe today
        }

    @Test
    fun `declining the reminder is recorded`() =
        runTest {
            val viewModel = viewModel()
            viewModel.onReminderChanged(false)

            viewModel.completed.test {
                viewModel.onSkip()
                advanceUntilIdle()
                awaitItem()
            }

            settings.settings().dailyReminderEnabled shouldBe false
        }

    @Test
    fun `a chosen reminder time is recorded`() =
        runTest {
            val viewModel = viewModel()
            viewModel.onReminderTimeChanged(LocalTime.of(18, 0))

            viewModel.completed.test {
                viewModel.onSkip()
                advanceUntilIdle()
                awaitItem()
            }

            settings.settings().dailyReminderTime shouldBe LocalTime.of(18, 0)
        }

    private fun viewModel() = OnboardingViewModel(categories, budgets, station, settings, clock)
}
