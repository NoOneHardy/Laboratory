package ch.no1hardy.orbit7.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import ch.no1hardy.orbit7.core.data.db.entity.BudgetEntity
import ch.no1hardy.orbit7.core.data.db.entity.CategoryEntity
import ch.no1hardy.orbit7.core.data.db.entity.ContractEntity
import ch.no1hardy.orbit7.core.data.db.entity.ContractNotificationEntity
import ch.no1hardy.orbit7.core.data.db.entity.EnergyLedgerEntity
import ch.no1hardy.orbit7.core.data.db.entity.ExpenseEntity
import ch.no1hardy.orbit7.core.data.db.entity.MissionEntity
import ch.no1hardy.orbit7.core.data.db.entity.SettlementCategoryEntity
import ch.no1hardy.orbit7.core.data.db.entity.StationModuleEntity
import ch.no1hardy.orbit7.core.data.db.entity.WeeklySettlementEntity
import ch.no1hardy.orbit7.core.data.db.entity.ZeroSpendMarkEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE archivedAt IS NULL ORDER BY sortOrder ASC, id ASC")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    suspend fun all(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun byId(id: Long): CategoryEntity?

    @Upsert
    suspend fun upsert(category: CategoryEntity): Long

    /** Archival, not deletion: expenses keep resolving to a name forever. */
    @Query("UPDATE categories SET archivedAt = :at WHERE id = :id")
    suspend fun archive(
        id: Long,
        at: Instant,
    )

    @Query("UPDATE categories SET archivedAt = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE occurredOn BETWEEN :from AND :to ORDER BY occurredOn DESC, id DESC")
    fun observeBetween(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY occurredOn DESC, id DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE occurredOn BETWEEN :from AND :to ORDER BY occurredOn ASC, id ASC")
    suspend fun between(
        from: LocalDate,
        to: LocalDate,
    ): List<ExpenseEntity>

    @Query("SELECT * FROM expenses")
    suspend fun all(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun byId(id: Long): ExpenseEntity?

    @Query("SELECT MIN(occurredOn) FROM expenses")
    suspend fun earliestDate(): LocalDate?

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY categoryId ASC, validFrom ASC")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE validTo IS NULL ORDER BY categoryId ASC")
    fun observeOpen(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun all(): List<BudgetEntity>

    @Query("SELECT MIN(validFrom) FROM budgets")
    suspend fun earliestValidFrom(): LocalDate?

    @Query(
        """
        SELECT * FROM budgets
        WHERE validFrom <= :to AND (validTo IS NULL OR validTo >= :from)
        ORDER BY categoryId ASC, validFrom ASC
        """,
    )
    suspend fun versionsCovering(
        from: LocalDate,
        to: LocalDate,
    ): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND validTo IS NULL LIMIT 1")
    suspend fun openVersion(categoryId: Long): BudgetEntity?

    @Insert
    suspend fun insert(budget: BudgetEntity): Long

    @Query("UPDATE budgets SET validTo = :validTo WHERE id = :id")
    suspend fun close(
        id: Long,
        validTo: LocalDate,
    )

    /**
     * Opens a new budget version, closing the current one the day before.
     *
     * One transaction, so the invariant "exactly one open row per category, ranges never overlap"
     * cannot be observed broken.
     */
    @Transaction
    suspend fun openNewVersion(
        categoryId: Long,
        amountMinorPerMonth: Long,
        validFrom: LocalDate,
    ) {
        // Closing the day before `validFrom` leaves no gap and no overlap. A version opened and
        // changed on the same day ends up closed with validTo < validFrom, which `coversDate` reads
        // as covering nothing — exactly right, the newer figure is the one that counts that day.
        openVersion(categoryId)?.let { close(it.id, validFrom.minusDays(1)) }
        insert(
            BudgetEntity(
                categoryId = categoryId,
                amountMinorPerMonth = amountMinorPerMonth,
                validFrom = validFrom,
                validTo = null,
            ),
        )
    }

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}

@Dao
interface ContractDao {
    @Query("SELECT * FROM contracts ORDER BY amountMinor DESC")
    fun observeAll(): Flow<List<ContractEntity>>

    @Query("SELECT * FROM contracts")
    suspend fun all(): List<ContractEntity>

    @Query("SELECT * FROM contracts WHERE status = 'ACTIVE'")
    suspend fun active(): List<ContractEntity>

    @Query("SELECT * FROM contracts WHERE id = :id")
    suspend fun byId(id: Long): ContractEntity?

    @Upsert
    suspend fun upsert(contract: ContractEntity): Long

    @Query(
        """
        UPDATE contracts
        SET status = :status,
            statusChangedOn = :on,
            previousAmountMinor = CASE WHEN :newAmountMinor IS NULL THEN previousAmountMinor ELSE amountMinor END,
            amountMinor = COALESCE(:newAmountMinor, amountMinor)
        WHERE id = :id
        """,
    )
    suspend fun updateStatus(
        id: Long,
        status: String,
        on: LocalDate,
        newAmountMinor: Long?,
    )

    @Query("DELETE FROM contracts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM contracts")
    suspend fun deleteAll()

    @Query("SELECT thresholdDays FROM contract_notifications WHERE contractId = :contractId")
    suspend fun notifiedThresholds(contractId: Long): List<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun recordNotification(notification: ContractNotificationEntity)

    @Query("DELETE FROM contract_notifications")
    suspend fun deleteAllNotifications()
}

@Dao
interface ZeroSpendDao {
    @Query("SELECT * FROM zero_spend_marks WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun observeBetween(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<ZeroSpendMarkEntity>>

    @Query("SELECT * FROM zero_spend_marks WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    suspend fun between(
        from: LocalDate,
        to: LocalDate,
    ): List<ZeroSpendMarkEntity>

    @Query("SELECT * FROM zero_spend_marks")
    suspend fun all(): List<ZeroSpendMarkEntity>

    @Query("SELECT MIN(date) FROM zero_spend_marks")
    suspend fun earliestDate(): LocalDate?

    @Query("SELECT EXISTS(SELECT 1 FROM zero_spend_marks WHERE date = :date)")
    suspend fun isMarked(date: LocalDate): Boolean

    /** Marking twice is a no-op: one row per day, and the original source is preserved. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(mark: ZeroSpendMarkEntity)

    @Query("DELETE FROM zero_spend_marks WHERE date = :date")
    suspend fun delete(date: LocalDate)

    @Query("DELETE FROM zero_spend_marks")
    suspend fun deleteAll()
}

/**
 * The ledger DAO.
 *
 * Note what is missing: there is no `@Update` and no delete-by-id. EP changes only by appending a
 * row, and a correction is itself an appended row. The only delete is the wipe-all-data path.
 */
@Dao
interface EnergyLedgerDao {
    @Query("SELECT COALESCE(SUM(delta), 0) FROM energy_ledger")
    fun observeBalance(): Flow<Int>

    @Query("SELECT * FROM energy_ledger ORDER BY occurredAt DESC, id DESC LIMIT :limit")
    fun observeLedger(limit: Int): Flow<List<EnergyLedgerEntity>>

    @Query("SELECT COALESCE(SUM(delta), 0) FROM energy_ledger")
    suspend fun balance(): Int

    @Query("SELECT COALESCE(SUM(delta), 0) FROM energy_ledger WHERE delta > 0")
    suspend fun lifetimeEarned(): Int

    @Query("SELECT * FROM energy_ledger ORDER BY id ASC")
    suspend fun all(): List<EnergyLedgerEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM energy_ledger WHERE reason = :reason AND referenceId = :referenceId)")
    suspend fun hasEntryFor(
        reason: String,
        referenceId: String,
    ): Boolean

    @Insert
    suspend fun append(entry: EnergyLedgerEntity): Long

    @Query("DELETE FROM energy_ledger")
    suspend fun deleteAll()
}

@Dao
interface SettlementDao {
    @Query("SELECT * FROM weekly_settlements ORDER BY weekStartDate ASC")
    fun observeAll(): Flow<List<WeeklySettlementEntity>>

    @Query("SELECT * FROM weekly_settlements ORDER BY weekStartDate ASC")
    suspend fun all(): List<WeeklySettlementEntity>

    @Query("SELECT * FROM weekly_settlements WHERE weekStartDate = :weekStart")
    suspend fun byWeek(weekStart: LocalDate): WeeklySettlementEntity?

    @Query("SELECT * FROM weekly_settlements ORDER BY weekStartDate DESC LIMIT 1")
    suspend fun latest(): WeeklySettlementEntity?

    @Query("SELECT * FROM settlement_categories WHERE weekStartDate = :weekStart")
    suspend fun categoryDetail(weekStart: LocalDate): List<SettlementCategoryEntity>

    /** ABORT, not REPLACE: a week that is already settled must fail, never silently re-settle. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(settlement: WeeklySettlementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<SettlementCategoryEntity>)

    @Query("DELETE FROM weekly_settlements")
    suspend fun deleteAll()
}

@Dao
interface StationDao {
    @Query("SELECT * FROM station_modules")
    fun observeAll(): Flow<List<StationModuleEntity>>

    @Query("SELECT * FROM station_modules")
    suspend fun all(): List<StationModuleEntity>

    @Query("SELECT level FROM station_modules WHERE moduleKey = :moduleKey")
    suspend fun level(moduleKey: String): Int?

    @Upsert
    suspend fun upsert(module: StationModuleEntity)

    @Query("DELETE FROM station_modules")
    suspend fun deleteAll()
}

@Dao
interface MissionDao {
    @Query("SELECT * FROM missions WHERE weekStartDate = :weekStart")
    fun observeForWeek(weekStart: LocalDate): Flow<List<MissionEntity>>

    @Query("SELECT * FROM missions WHERE weekStartDate = :weekStart")
    suspend fun forWeek(weekStart: LocalDate): List<MissionEntity>

    @Query("SELECT * FROM missions")
    suspend fun all(): List<MissionEntity>

    @Query("DELETE FROM missions WHERE weekStartDate = :weekStart")
    suspend fun deleteForWeek(weekStart: LocalDate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(missions: List<MissionEntity>)

    @Transaction
    suspend fun replaceOffer(
        weekStart: LocalDate,
        missions: List<MissionEntity>,
    ) {
        deleteForWeek(weekStart)
        insertAll(missions)
    }

    @Query("UPDATE missions SET status = :status WHERE id = :id")
    suspend fun updateStatus(
        id: String,
        status: String,
    )

    @Query("DELETE FROM missions")
    suspend fun deleteAll()
}
