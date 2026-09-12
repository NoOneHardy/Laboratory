package ch.no1hardy.orbit7.core.domain.repository

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
import ch.no1hardy.orbit7.core.domain.time.Week
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/**
 * The ports of the domain.
 *
 * `:core:domain` owns these interfaces; `:core:data` implements them with Room and DataStore. The
 * dependency arrow therefore points inward, and every use case below is testable against the fakes
 * in `:core:testing` with no Android framework in sight.
 */
interface CategoryRepository {
    fun observeCategories(includeArchived: Boolean = false): Flow<List<Category>>

    suspend fun categories(includeArchived: Boolean = false): List<Category>

    suspend fun category(id: Long): Category?

    suspend fun upsert(category: Category): Long

    suspend fun archive(
        id: Long,
        at: Instant,
    )

    suspend fun restore(id: Long)

    /** Seeds the Swiss default set during onboarding; returns the created categories. */
    suspend fun seedDefaults(keys: List<CategoryKey>): List<Category>
}

interface ExpenseRepository {
    fun observeBetween(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<Expense>>

    fun observeAll(): Flow<List<Expense>>

    fun observeRecent(limit: Int): Flow<List<Expense>>

    suspend fun between(
        from: LocalDate,
        to: LocalDate,
    ): List<Expense>

    suspend fun expense(id: Long): Expense?

    /** The day the very first expense was logged — where settlement catch-up starts. */
    suspend fun earliestExpenseDate(): LocalDate?

    suspend fun add(expense: Expense): Long

    suspend fun update(expense: Expense)

    suspend fun delete(id: Long)

    suspend fun deleteAll()
}

interface BudgetRepository {
    fun observeActiveBudgets(): Flow<List<Budget>>

    fun observeAllVersions(): Flow<List<Budget>>

    suspend fun versionsCovering(
        from: LocalDate,
        to: LocalDate,
    ): List<Budget>

    suspend fun allVersions(): List<Budget>

    /** The day budgeting started — the economy's anchor when nothing has been logged yet. */
    suspend fun earliestValidFrom(): LocalDate?

    /** Closes the open version and opens a new one — budgets are versioned, never overwritten. */
    suspend fun setMonthlyBudget(
        categoryId: Long,
        amount: Money,
        validFrom: LocalDate,
    )
}

interface ContractRepository {
    fun observeContracts(): Flow<List<Contract>>

    suspend fun contracts(): List<Contract>

    suspend fun contract(id: Long): Contract?

    suspend fun upsert(contract: Contract): Long

    suspend fun updateStatus(
        id: Long,
        status: ContractStatus,
        on: LocalDate,
        newAmount: Money?,
    )

    suspend fun delete(id: Long)
}

interface ZeroSpendRepository {
    fun observeBetween(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<ZeroSpendMark>>

    suspend fun between(
        from: LocalDate,
        to: LocalDate,
    ): List<ZeroSpendMark>

    suspend fun mark(
        date: LocalDate,
        at: Instant,
        source: ZeroSpendSource,
    )

    suspend fun unmark(date: LocalDate)

    suspend fun isMarked(date: LocalDate): Boolean

    suspend fun earliestMarkDate(): LocalDate?
}

/**
 * The append-only energy ledger.
 *
 * There is deliberately no update and no delete: EP changes only by appending
 * (`docs/05-architecture.md` §5, hard rule 3). A correction is itself an appended row.
 */
interface EnergyRepository {
    fun observeBalance(): Flow<Int>

    fun observeLedger(limit: Int = 200): Flow<List<EnergyLedgerEntry>>

    suspend fun balance(): Int

    suspend fun append(
        delta: Int,
        reason: EnergyReason,
        referenceId: String?,
        at: Instant,
    )

    suspend fun hasEntryFor(
        reason: EnergyReason,
        referenceId: String,
    ): Boolean

    suspend fun lifetimeEarned(): Int
}

interface SettlementRepository {
    fun observeSettlements(): Flow<List<WeeklySettlement>>

    suspend fun settlements(): List<WeeklySettlement>

    suspend fun settlement(week: Week): WeeklySettlement?

    suspend fun latest(): WeeklySettlement?

    /** Fails rather than duplicates if the week was already settled — the PK is the week start. */
    suspend fun insert(
        settlement: WeeklySettlement,
        categories: List<CategorySettlement>,
    ): Boolean

    suspend fun categoryDetail(week: Week): List<CategorySettlement>
}

interface StationRepository {
    fun observeModules(): Flow<List<StationModuleState>>

    suspend fun modules(): List<StationModuleState>

    suspend fun level(moduleKey: StationModuleKey): Int

    suspend fun setLevel(
        moduleKey: StationModuleKey,
        level: Int,
        at: Instant,
    )
}

interface MissionRepository {
    fun observeMissions(week: Week): Flow<List<Mission>>

    suspend fun missions(week: Week): List<Mission>

    suspend fun replaceOffer(
        week: Week,
        missions: List<Mission>,
    )

    suspend fun updateStatus(
        missionId: String,
        status: MissionStatus,
    )
}

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>

    suspend fun settings(): AppSettings

    suspend fun update(transform: (AppSettings) -> AppSettings)
}

/** Backup: a single JSON document, exported and imported by the user. Replace-all on import. */
interface BackupRepository {
    suspend fun export(): String

    suspend fun import(json: String): ImportResult

    suspend fun wipeAllData()
}

sealed interface ImportResult {
    data class Success(
        val expenses: Int,
        val contracts: Int,
        val ledgerEntries: Int,
    ) : ImportResult

    data class Failure(
        val reason: ImportFailure,
    ) : ImportResult
}

enum class ImportFailure {
    MALFORMED_JSON,
    UNSUPPORTED_SCHEMA_VERSION,
    INCONSISTENT_DATA,
}
