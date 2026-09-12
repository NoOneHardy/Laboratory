package ch.no1hardy.orbit7.core.domain.test

import ch.no1hardy.orbit7.core.domain.model.AppSettings
import ch.no1hardy.orbit7.core.domain.model.Budget
import ch.no1hardy.orbit7.core.domain.model.Category
import ch.no1hardy.orbit7.core.domain.model.CategoryKey
import ch.no1hardy.orbit7.core.domain.model.CategorySettlement
import ch.no1hardy.orbit7.core.domain.model.Contract
import ch.no1hardy.orbit7.core.domain.model.ContractStatus
import ch.no1hardy.orbit7.core.domain.model.EnergyLedgerEntry
import ch.no1hardy.orbit7.core.domain.model.EnergyReason
import ch.no1hardy.orbit7.core.domain.model.Expense
import ch.no1hardy.orbit7.core.domain.model.Mission
import ch.no1hardy.orbit7.core.domain.model.MissionStatus
import ch.no1hardy.orbit7.core.domain.model.StationModuleKey
import ch.no1hardy.orbit7.core.domain.model.StationModuleState
import ch.no1hardy.orbit7.core.domain.model.WeeklySettlement
import ch.no1hardy.orbit7.core.domain.model.ZeroSpendMark
import ch.no1hardy.orbit7.core.domain.model.ZeroSpendSource
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.repository.BudgetRepository
import ch.no1hardy.orbit7.core.domain.repository.CategoryRepository
import ch.no1hardy.orbit7.core.domain.repository.ContractRepository
import ch.no1hardy.orbit7.core.domain.repository.EnergyRepository
import ch.no1hardy.orbit7.core.domain.repository.ExpenseRepository
import ch.no1hardy.orbit7.core.domain.repository.MissionRepository
import ch.no1hardy.orbit7.core.domain.repository.SettingsRepository
import ch.no1hardy.orbit7.core.domain.repository.SettlementRepository
import ch.no1hardy.orbit7.core.domain.repository.StationRepository
import ch.no1hardy.orbit7.core.domain.repository.ZeroSpendRepository
import ch.no1hardy.orbit7.core.domain.time.Week
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

/**
 * In-memory repositories with real Flow emission, used by the domain's own tests and — through
 * `:core:testing` — by every ViewModel and Compose test in the project.
 *
 * They are deliberately simple: a list, a `MutableStateFlow`, and the same invariants the Room
 * implementations carry (the ledger has no update or delete; a settled week cannot be settled
 * twice; categories are archived rather than removed).
 */
class FakeCategoryRepository(
    initial: List<Category> = emptyList(),
) : CategoryRepository {
    private val state = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observeCategories(includeArchived: Boolean): Flow<List<Category>> =
        state.map { categories -> categories.filter { includeArchived || !it.isArchived }.sortedBy { it.sortOrder } }

    override suspend fun categories(includeArchived: Boolean): List<Category> =
        state.value.filter { includeArchived || !it.isArchived }

    override suspend fun category(id: Long): Category? = state.value.firstOrNull { it.id == id }

    override suspend fun upsert(category: Category): Long {
        val id = if (category.id == 0L) nextId++ else category.id
        state.value = state.value.filterNot { it.id == id } + category.copy(id = id)
        return id
    }

    override suspend fun archive(
        id: Long,
        at: Instant,
    ) {
        state.value = state.value.map { if (it.id == id) it.copy(archivedAt = at) else it }
    }

    override suspend fun restore(id: Long) {
        state.value = state.value.map { if (it.id == id) it.copy(archivedAt = null) else it }
    }

    override suspend fun seedDefaults(keys: List<CategoryKey>): List<Category> =
        keys.mapIndexed { index, key ->
            val category = aCategory(id = 0, key = key, sortOrder = index)
            category.copy(id = upsert(category))
        }
}

class FakeExpenseRepository(
    initial: List<Expense> = emptyList(),
) : ExpenseRepository {
    private val state = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    val current: List<Expense> get() = state.value

    override fun observeBetween(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<Expense>> = state.map { expenses -> expenses.filter { it.occurredOn in from..to } }

    override fun observeAll(): Flow<List<Expense>> = state

    override fun observeRecent(limit: Int): Flow<List<Expense>> =
        state.map { expenses -> expenses.sortedByDescending { it.createdAt }.take(limit) }

    override suspend fun between(
        from: LocalDate,
        to: LocalDate,
    ): List<Expense> = state.value.filter { it.occurredOn in from..to }

    override suspend fun expense(id: Long): Expense? = state.value.firstOrNull { it.id == id }

    override suspend fun earliestExpenseDate(): LocalDate? = state.value.minOfOrNull { it.occurredOn }

    override suspend fun add(expense: Expense): Long {
        val id = nextId++
        state.value = state.value + expense.copy(id = id)
        return id
    }

    override suspend fun update(expense: Expense) {
        state.value = state.value.map { if (it.id == expense.id) expense else it }
    }

    override suspend fun delete(id: Long) {
        state.value = state.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        state.value = emptyList()
    }
}

class FakeBudgetRepository(
    initial: List<Budget> = emptyList(),
) : BudgetRepository {
    private val state = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observeActiveBudgets(): Flow<List<Budget>> = state.map { budgets -> budgets.filter { it.isOpen } }

    override fun observeAllVersions(): Flow<List<Budget>> = state

    override suspend fun versionsCovering(
        from: LocalDate,
        to: LocalDate,
    ): List<Budget> =
        state.value.filter { budget ->
            !budget.validFrom.isAfter(to) && (budget.validTo == null || !budget.validTo.isBefore(from))
        }

    override suspend fun allVersions(): List<Budget> = state.value

    override suspend fun earliestValidFrom(): LocalDate? = state.value.minOfOrNull { it.validFrom }

    override suspend fun setMonthlyBudget(
        categoryId: Long,
        amount: Money,
        validFrom: LocalDate,
    ) {
        val closed =
            state.value.map { budget ->
                if (budget.categoryId == categoryId && budget.isOpen) {
                    budget.copy(validTo = validFrom.minusDays(1))
                } else {
                    budget
                }
            }
        state.value = closed + Budget(nextId++, categoryId, amount, validFrom, null)
    }
}

class FakeZeroSpendRepository(
    initial: List<ZeroSpendMark> = emptyList(),
) : ZeroSpendRepository {
    private val state = MutableStateFlow(initial)

    override fun observeBetween(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<ZeroSpendMark>> = state.map { marks -> marks.filter { it.date in from..to } }

    override suspend fun between(
        from: LocalDate,
        to: LocalDate,
    ): List<ZeroSpendMark> = state.value.filter { it.date in from..to }

    override suspend fun mark(
        date: LocalDate,
        at: Instant,
        source: ZeroSpendSource,
    ) {
        if (state.value.any { it.date == date }) return
        state.value = state.value + ZeroSpendMark(date, at, source)
    }

    override suspend fun unmark(date: LocalDate) {
        state.value = state.value.filterNot { it.date == date }
    }

    override suspend fun isMarked(date: LocalDate): Boolean = state.value.any { it.date == date }

    override suspend fun earliestMarkDate(): LocalDate? = state.value.minOfOrNull { it.date }
}

/** Append-only, exactly like the real thing: there is no way to change or remove an entry. */
class FakeEnergyRepository(
    initial: List<EnergyLedgerEntry> = emptyList(),
) : EnergyRepository {
    private val state = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    val entries: List<EnergyLedgerEntry> get() = state.value

    override fun observeBalance(): Flow<Int> = state.map { entries -> entries.sumOf { it.delta } }

    override fun observeLedger(limit: Int): Flow<List<EnergyLedgerEntry>> =
        state.map { entries -> entries.sortedByDescending { it.id }.take(limit) }

    override suspend fun balance(): Int = state.value.sumOf { it.delta }

    override suspend fun append(
        delta: Int,
        reason: EnergyReason,
        referenceId: String?,
        at: Instant,
    ) {
        state.value = state.value + EnergyLedgerEntry(nextId++, delta, reason, referenceId, at)
    }

    override suspend fun hasEntryFor(
        reason: EnergyReason,
        referenceId: String,
    ): Boolean = state.value.any { it.reason == reason && it.referenceId == referenceId }

    override suspend fun lifetimeEarned(): Int = state.value.filter { it.delta > 0 }.sumOf { it.delta }
}

class FakeSettlementRepository(
    initial: List<WeeklySettlement> = emptyList(),
) : SettlementRepository {
    private val state = MutableStateFlow(initial)
    private val details = mutableMapOf<String, List<CategorySettlement>>()

    override fun observeSettlements(): Flow<List<WeeklySettlement>> = state

    override suspend fun settlements(): List<WeeklySettlement> = state.value

    override suspend fun settlement(week: Week): WeeklySettlement? = state.value.firstOrNull { it.week == week }

    override suspend fun latest(): WeeklySettlement? = state.value.maxByOrNull { it.week }

    override suspend fun insert(
        settlement: WeeklySettlement,
        categories: List<CategorySettlement>,
    ): Boolean {
        if (state.value.any { it.week == settlement.week }) return false
        state.value = state.value + settlement
        details[settlement.week.id] = categories
        return true
    }

    override suspend fun categoryDetail(week: Week): List<CategorySettlement> = details[week.id].orEmpty()
}

class FakeStationRepository(
    initial: List<StationModuleState> = emptyList(),
) : StationRepository {
    private val state = MutableStateFlow(initial)

    override fun observeModules(): Flow<List<StationModuleState>> = state

    override suspend fun modules(): List<StationModuleState> = state.value

    override suspend fun level(moduleKey: StationModuleKey): Int =
        state.value.firstOrNull { it.moduleKey == moduleKey }?.level ?: 0

    override suspend fun setLevel(
        moduleKey: StationModuleKey,
        level: Int,
        at: Instant,
    ) {
        val existing = state.value.firstOrNull { it.moduleKey == moduleKey }
        val updated =
            existing?.copy(level = level, lastUpgradedAt = at)
                ?: StationModuleState(moduleKey, level, unlockedAt = at, lastUpgradedAt = at)
        state.value = state.value.filterNot { it.moduleKey == moduleKey } + updated
    }
}

class FakeContractRepository(
    initial: List<Contract> = emptyList(),
) : ContractRepository {
    private val state = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observeContracts(): Flow<List<Contract>> = state

    override suspend fun contracts(): List<Contract> = state.value

    override suspend fun contract(id: Long): Contract? = state.value.firstOrNull { it.id == id }

    override suspend fun upsert(contract: Contract): Long {
        val id = if (contract.id == 0L) nextId++ else contract.id
        state.value = state.value.filterNot { it.id == id } + contract.copy(id = id)
        return id
    }

    override suspend fun updateStatus(
        id: Long,
        status: ContractStatus,
        on: LocalDate,
        newAmount: Money?,
    ) {
        state.value =
            state.value.map { contract ->
                if (contract.id != id) {
                    contract
                } else {
                    contract.copy(
                        status = status,
                        statusChangedOn = on,
                        previousAmount = newAmount?.let { contract.amount } ?: contract.previousAmount,
                        amount = newAmount ?: contract.amount,
                    )
                }
            }
    }

    override suspend fun delete(id: Long) {
        state.value = state.value.filterNot { it.id == id }
    }
}

class FakeMissionRepository : MissionRepository {
    private val state = MutableStateFlow<List<Mission>>(emptyList())

    override fun observeMissions(week: Week): Flow<List<Mission>> =
        state.map { missions -> missions.filter { it.week == week } }

    override suspend fun missions(week: Week): List<Mission> = state.value.filter { it.week == week }

    override suspend fun replaceOffer(
        week: Week,
        missions: List<Mission>,
    ) {
        state.value = state.value.filterNot { it.week == week } + missions
    }

    override suspend fun updateStatus(
        missionId: String,
        status: MissionStatus,
    ) {
        state.value = state.value.map { if (it.id == missionId) it.copy(status = status) else it }
    }
}

class FakeSettingsRepository(
    initial: AppSettings = AppSettings(),
) : SettingsRepository {
    private val state = MutableStateFlow(initial)

    override fun observeSettings(): Flow<AppSettings> = state

    override suspend fun settings(): AppSettings = state.value

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        state.value = transform(state.value)
    }
}
