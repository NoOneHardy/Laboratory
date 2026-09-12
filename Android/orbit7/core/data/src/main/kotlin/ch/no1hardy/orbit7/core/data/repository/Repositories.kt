package ch.no1hardy.orbit7.core.data.repository

import android.database.sqlite.SQLiteConstraintException
import ch.no1hardy.orbit7.core.data.db.dao.BudgetDao
import ch.no1hardy.orbit7.core.data.db.dao.CategoryDao
import ch.no1hardy.orbit7.core.data.db.dao.ContractDao
import ch.no1hardy.orbit7.core.data.db.dao.EnergyLedgerDao
import ch.no1hardy.orbit7.core.data.db.dao.ExpenseDao
import ch.no1hardy.orbit7.core.data.db.dao.MissionDao
import ch.no1hardy.orbit7.core.data.db.dao.SettlementDao
import ch.no1hardy.orbit7.core.data.db.dao.StationDao
import ch.no1hardy.orbit7.core.data.db.dao.ZeroSpendDao
import ch.no1hardy.orbit7.core.data.db.entity.CategoryEntity
import ch.no1hardy.orbit7.core.data.db.entity.EnergyLedgerEntity
import ch.no1hardy.orbit7.core.data.db.entity.StationModuleEntity
import ch.no1hardy.orbit7.core.data.db.entity.ZeroSpendMarkEntity
import ch.no1hardy.orbit7.core.data.mapper.toDomain
import ch.no1hardy.orbit7.core.data.mapper.toEntity
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
import ch.no1hardy.orbit7.core.domain.repository.SettlementRepository
import ch.no1hardy.orbit7.core.domain.repository.StationRepository
import ch.no1hardy.orbit7.core.domain.repository.ZeroSpendRepository
import ch.no1hardy.orbit7.core.domain.time.Week
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** The default Swiss category set offered at onboarding, with its icons and colour tokens. */
internal val DEFAULT_CATEGORY_PRESENTATION: Map<CategoryKey, Pair<String, String>> =
    mapOf(
        CategoryKey.GROCERIES to ("cart" to "accent"),
        CategoryKey.EATING_OUT to ("cutlery" to "energy"),
        CategoryKey.TRANSPORT to ("tram" to "accent"),
        CategoryKey.HOUSEHOLD to ("home" to "onSurfaceMuted"),
        CategoryKey.HEALTH to ("cross" to "warning"),
        CategoryKey.LEISURE to ("spark" to "energy"),
        CategoryKey.SUBSCRIPTIONS to ("loop" to "warning"),
        CategoryKey.OTHER to ("dot" to "onSurfaceMuted"),
    )

@Singleton
class RoomCategoryRepository
    @Inject
    constructor(
        private val dao: CategoryDao,
    ) : CategoryRepository {
        override fun observeCategories(includeArchived: Boolean): Flow<List<Category>> =
            (if (includeArchived) dao.observeAll() else dao.observeActive())
                .map { entities -> entities.map { it.toDomain() } }

        override suspend fun categories(includeArchived: Boolean): List<Category> =
            dao.all().filter { includeArchived || it.archivedAt == null }.map { it.toDomain() }

        override suspend fun category(id: Long): Category? = dao.byId(id)?.toDomain()

        override suspend fun upsert(category: Category): Long = dao.upsert(category.toEntity())

        override suspend fun archive(
            id: Long,
            at: Instant,
        ) = dao.archive(id, at)

        override suspend fun restore(id: Long) = dao.restore(id)

        override suspend fun seedDefaults(keys: List<CategoryKey>): List<Category> =
            keys.mapIndexed { index, key ->
                val (icon, token) = DEFAULT_CATEGORY_PRESENTATION.getValue(key)
                val id =
                    dao.upsert(
                        CategoryEntity(
                            key = key.name,
                            customName = null,
                            iconKey = icon,
                            colorToken = token,
                            sortOrder = index,
                        ),
                    )
                dao.byId(id)!!.toDomain()
            }
    }

@Singleton
class RoomExpenseRepository
    @Inject
    constructor(
        private val dao: ExpenseDao,
    ) : ExpenseRepository {
        override fun observeBetween(
            from: LocalDate,
            to: LocalDate,
        ): Flow<List<Expense>> = dao.observeBetween(from, to).map { entities -> entities.map { it.toDomain() } }

        override fun observeAll(): Flow<List<Expense>> =
            dao.observeAll().map { entities -> entities.map { it.toDomain() } }

        override fun observeRecent(limit: Int): Flow<List<Expense>> =
            dao.observeRecent(limit).map { entities -> entities.map { it.toDomain() } }

        override suspend fun between(
            from: LocalDate,
            to: LocalDate,
        ): List<Expense> = dao.between(from, to).map { it.toDomain() }

        override suspend fun expense(id: Long): Expense? = dao.byId(id)?.toDomain()

        override suspend fun earliestExpenseDate(): LocalDate? = dao.earliestDate()

        override suspend fun add(expense: Expense): Long = dao.insert(expense.toEntity())

        override suspend fun update(expense: Expense) = dao.update(expense.toEntity())

        override suspend fun delete(id: Long) = dao.delete(id)

        override suspend fun deleteAll() = dao.deleteAll()
    }

@Singleton
class RoomBudgetRepository
    @Inject
    constructor(
        private val dao: BudgetDao,
    ) : BudgetRepository {
        override fun observeActiveBudgets(): Flow<List<Budget>> =
            dao.observeOpen().map { entities -> entities.map { it.toDomain() } }

        override fun observeAllVersions(): Flow<List<Budget>> =
            dao.observeAll().map { entities -> entities.map { it.toDomain() } }

        override suspend fun versionsCovering(
            from: LocalDate,
            to: LocalDate,
        ): List<Budget> = dao.versionsCovering(from, to).map { it.toDomain() }

        override suspend fun allVersions(): List<Budget> = dao.all().map { it.toDomain() }

        override suspend fun earliestValidFrom(): LocalDate? = dao.earliestValidFrom()

        override suspend fun setMonthlyBudget(
            categoryId: Long,
            amount: Money,
            validFrom: LocalDate,
        ) = dao.openNewVersion(categoryId, amount.minor, validFrom)
    }

@Singleton
class RoomContractRepository
    @Inject
    constructor(
        private val dao: ContractDao,
    ) : ContractRepository {
        override fun observeContracts(): Flow<List<Contract>> =
            dao.observeAll().map { entities -> entities.map { it.toDomain() } }

        override suspend fun contracts(): List<Contract> = dao.all().map { it.toDomain() }

        override suspend fun contract(id: Long): Contract? = dao.byId(id)?.toDomain()

        override suspend fun upsert(contract: Contract): Long = dao.upsert(contract.toEntity())

        override suspend fun updateStatus(
            id: Long,
            status: ContractStatus,
            on: LocalDate,
            newAmount: Money?,
        ) = dao.updateStatus(id, status.name, on, newAmount?.minor)

        override suspend fun delete(id: Long) = dao.delete(id)
    }

@Singleton
class RoomZeroSpendRepository
    @Inject
    constructor(
        private val dao: ZeroSpendDao,
    ) : ZeroSpendRepository {
        override fun observeBetween(
            from: LocalDate,
            to: LocalDate,
        ): Flow<List<ZeroSpendMark>> = dao.observeBetween(from, to).map { entities -> entities.map { it.toDomain() } }

        override suspend fun between(
            from: LocalDate,
            to: LocalDate,
        ): List<ZeroSpendMark> = dao.between(from, to).map { it.toDomain() }

        override suspend fun mark(
            date: LocalDate,
            at: Instant,
            source: ZeroSpendSource,
        ) = dao.insert(ZeroSpendMarkEntity(date = date, markedAt = at, source = source.name))

        override suspend fun unmark(date: LocalDate) = dao.delete(date)

        override suspend fun isMarked(date: LocalDate): Boolean = dao.isMarked(date)

        override suspend fun earliestMarkDate(): LocalDate? = dao.earliestDate()
    }

/**
 * The ledger.
 *
 * Every EP movement in the app funnels through [append]; nothing else can change the balance,
 * because nothing else is offered.
 */
@Singleton
class RoomEnergyRepository
    @Inject
    constructor(
        private val dao: EnergyLedgerDao,
    ) : EnergyRepository {
        override fun observeBalance(): Flow<Int> = dao.observeBalance()

        override fun observeLedger(limit: Int): Flow<List<EnergyLedgerEntry>> =
            dao.observeLedger(limit).map { entities -> entities.map { it.toDomain() } }

        override suspend fun balance(): Int = dao.balance()

        override suspend fun append(
            delta: Int,
            reason: EnergyReason,
            referenceId: String?,
            at: Instant,
        ) {
            dao.append(
                EnergyLedgerEntity(
                    delta = delta,
                    reason = reason.name,
                    referenceId = referenceId,
                    occurredAt = at,
                ),
            )
        }

        override suspend fun hasEntryFor(
            reason: EnergyReason,
            referenceId: String,
        ): Boolean = dao.hasEntryFor(reason.name, referenceId)

        override suspend fun lifetimeEarned(): Int = dao.lifetimeEarned()
    }

@Singleton
class RoomSettlementRepository
    @Inject
    constructor(
        private val dao: SettlementDao,
    ) : SettlementRepository {
        override fun observeSettlements(): Flow<List<WeeklySettlement>> =
            dao.observeAll().map { entities -> entities.map { it.toDomain() } }

        override suspend fun settlements(): List<WeeklySettlement> = dao.all().map { it.toDomain() }

        override suspend fun settlement(week: Week): WeeklySettlement? = dao.byWeek(week.start)?.toDomain()

        override suspend fun latest(): WeeklySettlement? = dao.latest()?.toDomain()

        /**
         * Returns false when the week was already settled.
         *
         * The primary key does the work: two workers racing on the same week produce one row and one
         * ledger entry, not two.
         */
        override suspend fun insert(
            settlement: WeeklySettlement,
            categories: List<CategorySettlement>,
        ): Boolean =
            try {
                dao.insert(settlement.toEntity())
                dao.insertCategories(categories.map { it.toEntity(settlement.week) })
                true
            } catch (conflict: SQLiteConstraintException) {
                // Expected: this week is already settled, and settlement is idempotent by design.
                check(dao.byWeek(settlement.week.start) != null) { "Unexpected constraint failure: $conflict" }
                false
            }

        override suspend fun categoryDetail(week: Week): List<CategorySettlement> =
            dao.categoryDetail(week.start).map { it.toDomain() }
    }

@Singleton
class RoomStationRepository
    @Inject
    constructor(
        private val dao: StationDao,
    ) : StationRepository {
        override fun observeModules(): Flow<List<StationModuleState>> =
            dao.observeAll().map { entities -> entities.map { it.toDomain() } }

        override suspend fun modules(): List<StationModuleState> = dao.all().map { it.toDomain() }

        override suspend fun level(moduleKey: StationModuleKey): Int = dao.level(moduleKey.name) ?: 0

        override suspend fun setLevel(
            moduleKey: StationModuleKey,
            level: Int,
            at: Instant,
        ) {
            val existing = dao.all().firstOrNull { it.moduleKey == moduleKey.name }
            require(level >= (existing?.level ?: 0)) { "A module level never decreases" }
            dao.upsert(
                StationModuleEntity(
                    moduleKey = moduleKey.name,
                    level = level,
                    unlockedAt = existing?.unlockedAt ?: at,
                    lastUpgradedAt = at,
                ),
            )
        }
    }

@Singleton
class RoomMissionRepository
    @Inject
    constructor(
        private val dao: MissionDao,
    ) : MissionRepository {
        override fun observeMissions(week: Week): Flow<List<Mission>> =
            dao.observeForWeek(week.start).map { entities -> entities.map { it.toDomain() } }

        override suspend fun missions(week: Week): List<Mission> = dao.forWeek(week.start).map { it.toDomain() }

        override suspend fun replaceOffer(
            week: Week,
            missions: List<Mission>,
        ) = dao.replaceOffer(week.start, missions.map { it.toEntity() })

        override suspend fun updateStatus(
            missionId: String,
            status: MissionStatus,
        ) = dao.updateStatus(missionId, status.name)
    }
