package ch.no1hardy.orbit7.core.domain.usecase

import ch.no1hardy.orbit7.core.domain.economy.BaselineCalculator
import ch.no1hardy.orbit7.core.domain.economy.StationCatalog
import ch.no1hardy.orbit7.core.domain.model.Category
import ch.no1hardy.orbit7.core.domain.model.ContractStatus
import ch.no1hardy.orbit7.core.domain.model.ModuleUnlock
import ch.no1hardy.orbit7.core.domain.model.WeeklySettlement
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.money.sum
import ch.no1hardy.orbit7.core.domain.repository.CategoryRepository
import ch.no1hardy.orbit7.core.domain.repository.ContractRepository
import ch.no1hardy.orbit7.core.domain.repository.EnergyRepository
import ch.no1hardy.orbit7.core.domain.repository.ExpenseRepository
import ch.no1hardy.orbit7.core.domain.repository.SettingsRepository
import ch.no1hardy.orbit7.core.domain.repository.SettlementRepository
import ch.no1hardy.orbit7.core.domain.repository.StationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Per-category trend, with the baseline ghost the charts draw. */
data class CategoryTrend(
    val category: Category,
    val weeklySpend: List<Money>,
    val baseline: Money?,
    val baselineAtStart: Money?,
)

/**
 * The success-criteria panel from `docs/01-concept.md`.
 *
 * The app measures its own three success criteria from the user's own data, with no telemetry:
 * the habit survived, a contract was salvaged, a baseline came down.
 */
data class SuccessCriteria(
    val contractsSalvaged: Int,
    val monthlyFixedCostAtStart: Money,
    val monthlyFixedCostNow: Money,
    val categoriesWithLowerBaseline: Int,
    val weeksSettled: Int,
)

data class ReportsState(
    val unlocked: Boolean,
    val settlements: List<WeeklySettlement>,
    val trends: List<CategoryTrend>,
    val lifetimeEp: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val successCriteria: SuccessCriteria,
)

/**
 * Reports (`docs/03-screens.md` §8), gated behind the Observation Deck module — an intentional
 * reward for progressing rather than a screen that is simply there.
 */
class ObserveReportsUseCase
    @Inject
    constructor(
        private val settlements: SettlementRepository,
        private val categories: CategoryRepository,
        private val expenses: ExpenseRepository,
        private val contracts: ContractRepository,
        private val station: StationRepository,
        private val energy: EnergyRepository,
        private val settings: SettingsRepository,
        private val baselines: BaselineCalculator,
        private val catalog: StationCatalog,
        private val clock: Clock,
    ) {
        operator fun invoke(): Flow<ReportsState> {
            val today = LocalDate.now(clock)
            return combine(
                settlements.observeSettlements(),
                categories.observeCategories(),
                expenses.observeAll(),
                contracts.observeContracts(),
                station.observeModules(),
            ) { history, allCategories, allExpenses, allContracts, modules ->
                val moduleStates = modules.associateBy { it.moduleKey }
                val firstDayOfWeek = settings.settings().firstDayOfWeek
                val salvaged = allContracts.filter { it.status != ContractStatus.ACTIVE }

                val trends =
                    allCategories.map { category ->
                        val history12 = baselines.weeklySpendHistory(category.id, allExpenses, today, firstDayOfWeek)
                        CategoryTrend(
                            category = category,
                            weeklySpend = history12,
                            baseline = baselines.baseline(category.id, allExpenses, today, firstDayOfWeek),
                            baselineAtStart = history12.firstOrNull(),
                        )
                    }

                ReportsState(
                    unlocked = catalog.isUnlocked(ModuleUnlock.REPORTS, moduleStates),
                    settlements = history.sortedByDescending { it.week },
                    trends = trends,
                    lifetimeEp = energy.lifetimeEarned(),
                    currentStreak = history.sortedByDescending { it.week }.takeWhile { it.goodWeek }.count(),
                    bestStreak = bestStreak(history),
                    successCriteria =
                        SuccessCriteria(
                            contractsSalvaged = salvaged.size,
                            monthlyFixedCostAtStart =
                                (
                                    allContracts.map { it.monthlyEquivalent } +
                                        salvaged.mapNotNull { it.previousMonthlyEquivalent }
                                ).sum(),
                            monthlyFixedCostNow =
                                allContracts
                                    .filter {
                                        it.isActive
                                    }.map { it.monthlyEquivalent }
                                    .sum(),
                            categoriesWithLowerBaseline =
                                trends.count { trend ->
                                    val start = trend.baselineAtStart
                                    val now = trend.baseline
                                    start != null && now != null && now < start
                                },
                            weeksSettled = history.size,
                        ),
                )
            }
        }

        private fun bestStreak(history: List<WeeklySettlement>): Int {
            var best = 0
            var current = 0
            history.sortedBy { it.week }.forEach { settlement ->
                current = if (settlement.goodWeek) current + 1 else 0
                best = maxOf(best, current)
            }
            return best
        }
    }
