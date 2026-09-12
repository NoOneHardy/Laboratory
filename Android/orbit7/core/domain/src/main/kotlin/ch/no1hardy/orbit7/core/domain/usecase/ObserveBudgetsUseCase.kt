package ch.no1hardy.orbit7.core.domain.usecase

import ch.no1hardy.orbit7.core.domain.economy.BaselineCalculator
import ch.no1hardy.orbit7.core.domain.economy.BaselineSuggestion
import ch.no1hardy.orbit7.core.domain.economy.ProrationCalculator
import ch.no1hardy.orbit7.core.domain.model.Category
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.money.sum
import ch.no1hardy.orbit7.core.domain.repository.BudgetRepository
import ch.no1hardy.orbit7.core.domain.repository.CategoryRepository
import ch.no1hardy.orbit7.core.domain.repository.ExpenseRepository
import ch.no1hardy.orbit7.core.domain.repository.SettingsRepository
import ch.no1hardy.orbit7.core.domain.time.Week
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** One row of the Budgets screen. */
data class CategoryBudget(
    val category: Category,
    val monthlyBudget: Money,
    val weeklyBudget: Money,
    val spentThisMonth: Money,
    val spentThisWeek: Money,
    val weeklyBaseline: Money?,
    val sparkline: List<Money>,
    val suggestion: BaselineSuggestion?,
) {
    val monthlyRemaining: Money get() = monthlyBudget - spentThisMonth
    val hasBaseline: Boolean get() = weeklyBaseline != null
}

/**
 * The Budgets screen's state, baselines and suggestions included
 * (`docs/02-game-design.md` §4.3, `docs/03-screens.md` §4).
 *
 * The suggestion rules live in [BaselineCalculator]; what this use case adds is the dismissal
 * memory, which is a setting rather than a calculation.
 */
class ObserveBudgetsUseCase
    @Inject
    constructor(
        private val categories: CategoryRepository,
        private val budgets: BudgetRepository,
        private val expenses: ExpenseRepository,
        private val settings: SettingsRepository,
        private val baselines: BaselineCalculator,
        private val clock: Clock,
    ) {
        operator fun invoke(): Flow<List<CategoryBudget>> {
            val today = LocalDate.now(clock)
            return combine(
                categories.observeCategories(),
                budgets.observeAllVersions(),
                expenses.observeAll(),
                settings.observeSettings(),
            ) { allCategories, budgetVersions, allExpenses, appSettings ->
                val week = Week.containing(today, appSettings.firstDayOfWeek)
                val monthStart = today.withDayOfMonth(1)

                allCategories.map { category ->
                    val monthly = ProrationCalculator.monthlyBudget(category.id, today, budgetVersions)
                    val weekly = ProrationCalculator.weeklyBudget(category.id, week, budgetVersions)
                    val baseline = baselines.baseline(category.id, allExpenses, today, appSettings.firstDayOfWeek)
                    CategoryBudget(
                        category = category,
                        monthlyBudget = monthly,
                        weeklyBudget = weekly,
                        spentThisMonth =
                            allExpenses
                                .filter { it.categoryId == category.id && !it.occurredOn.isBefore(monthStart) }
                                .filter { !it.occurredOn.isAfter(today) }
                                .map { it.amount }
                                .sum(),
                        spentThisWeek =
                            allExpenses
                                .filter { it.categoryId == category.id && it.occurredOn in week }
                                .map { it.amount }
                                .sum(),
                        weeklyBaseline = baseline,
                        sparkline =
                            baselines.weeklySpendHistory(
                                category.id,
                                allExpenses,
                                today,
                                appSettings.firstDayOfWeek,
                            ),
                        suggestion =
                            baselines.suggestion(
                                categoryId = category.id,
                                baseline = baseline,
                                currentWeeklyBudget = weekly,
                                today = today,
                                lastDismissedOn = appSettings.dismissedSuggestions[category.id],
                            ),
                    )
                }
            }
        }
    }

/** Applies a baseline suggestion by opening a new budget version. */
class AcceptBaselineSuggestionUseCase
    @Inject
    constructor(
        private val budgets: BudgetRepository,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(
            categoryId: Long,
            monthlyAmount: Money,
        ) {
            budgets.setMonthlyBudget(categoryId, monthlyAmount, LocalDate.now(clock))
        }
    }

/** Remembers a dismissal so the same suggestion is not repeated for 28 days. */
class DismissBaselineSuggestionUseCase
    @Inject
    constructor(
        private val settings: SettingsRepository,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(categoryId: Long) {
            val today = LocalDate.now(clock)
            settings.update { current ->
                current.copy(dismissedSuggestions = current.dismissedSuggestions + (categoryId to today))
            }
        }
    }
