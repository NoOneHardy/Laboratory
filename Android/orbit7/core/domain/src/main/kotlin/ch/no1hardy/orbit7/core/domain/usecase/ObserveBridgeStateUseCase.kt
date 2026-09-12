package ch.no1hardy.orbit7.core.domain.usecase

import ch.no1hardy.orbit7.core.domain.economy.ContractSchedule
import ch.no1hardy.orbit7.core.domain.economy.ProrationCalculator
import ch.no1hardy.orbit7.core.domain.economy.SettlementCalculator
import ch.no1hardy.orbit7.core.domain.model.AppSettings
import ch.no1hardy.orbit7.core.domain.model.Budget
import ch.no1hardy.orbit7.core.domain.model.Contract
import ch.no1hardy.orbit7.core.domain.model.Expense
import ch.no1hardy.orbit7.core.domain.model.StationModuleKey
import ch.no1hardy.orbit7.core.domain.model.StationModuleState
import ch.no1hardy.orbit7.core.domain.model.WeeklySettlement
import ch.no1hardy.orbit7.core.domain.model.ZeroSpendMark
import ch.no1hardy.orbit7.core.domain.money.IntegerMath
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.money.sum
import ch.no1hardy.orbit7.core.domain.repository.BudgetRepository
import ch.no1hardy.orbit7.core.domain.repository.ContractRepository
import ch.no1hardy.orbit7.core.domain.repository.EnergyRepository
import ch.no1hardy.orbit7.core.domain.repository.ExpenseRepository
import ch.no1hardy.orbit7.core.domain.repository.SettingsRepository
import ch.no1hardy.orbit7.core.domain.repository.SettlementRepository
import ch.no1hardy.orbit7.core.domain.repository.StationRepository
import ch.no1hardy.orbit7.core.domain.repository.ZeroSpendRepository
import ch.no1hardy.orbit7.core.domain.time.Week
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Everything the Bridge shows, computed in the domain so the ViewModel only has to format it. */
data class BridgeState(
    val week: Week,
    val today: LocalDate,
    val balanceEp: Int,
    val provisionalEp: Int,
    val weeklyBudget: Money,
    val weeklySpend: Money,
    val todaySpend: Money,
    val todayEntries: List<Expense>,
    val zeroSpendMarkedToday: Boolean,
    val streakBonusPercent: Int,
    val consecutiveGoodWeeks: Int,
    val signalDays: Int,
    val modules: List<StationModuleState>,
    val nextDeadline: ContractDeadline?,
    val pendingSettlementWeekId: String?,
    val hasAnyData: Boolean,
) {
    val remainingBudget: Money get() = weeklyBudget - weeklySpend
    val isOverdrawn: Boolean get() = remainingBudget.minor < 0
    val stationLevels: Map<StationModuleKey, Int> get() = modules.associate { it.moduleKey to it.level }
}

/** The nearest contract deadline, when one is close enough to be worth saying. */
data class ContractDeadline(
    val contract: Contract,
    val daysRemaining: Long,
    val urgent: Boolean,
)

/**
 * Assembles the Bridge's state (`docs/03-screens.md` §1).
 *
 * The provisional EP figure is explicitly **not** banked: it is what the week would pay if it closed
 * right now, and the UI is required to label it as such.
 */
class ObserveBridgeStateUseCase
    @Inject
    constructor(
        private val expenses: ExpenseRepository,
        private val budgets: BudgetRepository,
        private val zeroSpend: ZeroSpendRepository,
        private val energy: EnergyRepository,
        private val station: StationRepository,
        private val contracts: ContractRepository,
        private val settlements: SettlementRepository,
        private val settings: SettingsRepository,
        private val calculator: SettlementCalculator,
        private val schedule: ContractSchedule,
        private val clock: Clock,
    ) {
        @OptIn(ExperimentalCoroutinesApi::class)
        operator fun invoke(): Flow<BridgeState> {
            val today = LocalDate.now(clock)
            return settings.observeSettings().flatMapLatest { appSettings ->
                val week = Week.containing(today, appSettings.firstDayOfWeek)
                val weekSlice =
                    combine(
                        expenses.observeBetween(week.start, week.endInclusive),
                        budgets.observeAllVersions(),
                        zeroSpend.observeBetween(week.start, week.endInclusive),
                        energy.observeBalance(),
                        ::WeekSlice,
                    )
                val stationSlice =
                    combine(
                        station.observeModules(),
                        contracts.observeContracts(),
                        settlements.observeSettlements(),
                        ::StationSlice,
                    )
                combine(weekSlice, stationSlice) { week1, station1 ->
                    build(appSettings, week, today, week1, station1)
                }
            }
        }

        private fun build(
            appSettings: AppSettings,
            week: Week,
            today: LocalDate,
            slice: WeekSlice,
            stationSlice: StationSlice,
        ): BridgeState {
            val weeklyBudget =
                ProrationCalculator
                    .weeklyBudgets(week, slice.budgets)
                    .values
                    .fold(Money.ZERO) { acc, value -> acc + value }
            val weekSpend = slice.expenses.map { it.amount }.sum()
            val todayEntries = slice.expenses.filter { it.occurredOn == today }
            val consecutiveGoodWeeks =
                stationSlice.settlements
                    .sortedByDescending { it.week }
                    .takeWhile { it.goodWeek }
                    .count()
            val signalDays = (slice.expenses.map { it.occurredOn } + slice.marks.map { it.date }).distinct().size

            return BridgeState(
                week = week,
                today = today,
                balanceEp = slice.balance,
                provisionalEp =
                    provisionalEp(
                        surplus = (weeklyBudget - weekSpend).coerceAtLeastZero(),
                        signalDays = signalDays,
                        consecutiveGoodWeeks = consecutiveGoodWeeks,
                    ),
                weeklyBudget = weeklyBudget,
                weeklySpend = weekSpend,
                todaySpend = todayEntries.map { it.amount }.sum(),
                todayEntries = todayEntries.sortedByDescending { it.createdAt }.take(TODAY_ENTRIES),
                zeroSpendMarkedToday = slice.marks.any { it.date == today },
                streakBonusPercent = bonusPercent(consecutiveGoodWeeks),
                consecutiveGoodWeeks = consecutiveGoodWeeks,
                signalDays = signalDays,
                modules = stationSlice.modules,
                nextDeadline = nearestDeadline(stationSlice.contracts, today),
                pendingSettlementWeekId =
                    stationSlice.settlements
                        .maxByOrNull { it.week }
                        ?.takeIf { it.week.id != appSettings.lastAcknowledgedSettlementWeekId }
                        ?.week
                        ?.id,
                hasAnyData =
                    slice.expenses.isNotEmpty() ||
                        stationSlice.settlements.isNotEmpty() ||
                        slice.balance > 0,
            )
        }

        private fun nearestDeadline(
            contracts: List<Contract>,
            today: LocalDate,
        ): ContractDeadline? =
            contracts
                .filter { it.isActive }
                .map { it to schedule.daysUntilDeadline(it, today) }
                .filter { (_, days) -> days in 0..DEADLINE_HORIZON_DAYS }
                .minByOrNull { (_, days) -> days }
                ?.let { (contract, days) -> ContractDeadline(contract, days, schedule.isUrgent(contract, today)) }

        private fun provisionalEp(
            surplus: Money,
            signalDays: Int,
            consecutiveGoodWeeks: Int,
        ): Int {
            val base = calculator.toEp(surplus)
            val confidence = calculator.confidenceBasisPoints(signalDays)
            val streak = calculator.streakBasisPoints(consecutiveGoodWeeks)
            return IntegerMath
                .roundHalfUpDiv(
                    base * confidence * streak,
                    IntegerMath.ONE_BP * IntegerMath.ONE_BP,
                ).toInt()
        }

        private fun bonusPercent(consecutiveGoodWeeks: Int): Int =
            ((calculator.streakBasisPoints(consecutiveGoodWeeks) - IntegerMath.ONE_BP) / BP_PER_PERCENT).toInt()

        private data class WeekSlice(
            val expenses: List<Expense>,
            val budgets: List<Budget>,
            val marks: List<ZeroSpendMark>,
            val balance: Int,
        )

        private data class StationSlice(
            val modules: List<StationModuleState>,
            val contracts: List<Contract>,
            val settlements: List<WeeklySettlement>,
        )

        private companion object {
            const val TODAY_ENTRIES = 3
            const val DEADLINE_HORIZON_DAYS = 30L
            const val BP_PER_PERCENT = 100L
        }
    }
