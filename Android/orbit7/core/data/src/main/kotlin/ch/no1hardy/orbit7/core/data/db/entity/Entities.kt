package ch.no1hardy.orbit7.core.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * The schema of `docs/05-architecture.md` §4, one table at a time.
 *
 * Money is `Long` minor units in every column that holds an amount; dates are `LocalDate` and
 * timestamps `Instant`, both stored through the converters in `Converters`.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String?,
    val customName: String?,
    val iconKey: String,
    val colorToken: String,
    val sortOrder: Int,
    /** Categories are archived, never hard-deleted, so old expenses always resolve to a name. */
    val archivedAt: Instant? = null,
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        // Every settlement and report query filters on the date; the compound index serves the
        // per-category range queries the budgets and baseline screens run.
        Index("occurredOn"),
        Index("categoryId", "occurredOn"),
    ],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMinor: Long,
    val currency: String,
    val categoryId: Long,
    val occurredOn: LocalDate,
    val note: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val source: String,
)

/**
 * A versioned budget row.
 *
 * Invariant, asserted by a DAO test: for a given category the validity ranges never overlap and
 * exactly one row has `validTo = null`.
 */
@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("categoryId", "validFrom")],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val amountMinorPerMonth: Long,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
)

@Entity(
    tableName = "contracts",
    indices = [Index("status"), Index("earliestCancellationOn")],
)
data class ContractEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: Long,
    val amountMinor: Long,
    val cadence: String,
    val nextChargeOn: LocalDate,
    /**
     * Notice periods are stored as months **and** days: Swiss contracts are stated in months, and
     * three months before 31 March is 31 December, which 90 days is not.
     */
    val noticePeriodMonths: Int,
    val noticePeriodDays: Int,
    val earliestCancellationOn: LocalDate,
    val status: String,
    val previousAmountMinor: Long?,
    val statusChangedOn: LocalDate?,
    val createdAt: Instant,
)

@Entity(tableName = "zero_spend_marks")
data class ZeroSpendMarkEntity(
    @PrimaryKey val date: LocalDate,
    val markedAt: Instant,
    val source: String,
)

/**
 * The energy ledger. **Append-only** — the DAO exposes no update and no delete
 * (`docs/05-architecture.md` §5, hard rule 3).
 */
@Entity(
    tableName = "energy_ledger",
    indices = [Index("reason", "referenceId")],
)
data class EnergyLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val delta: Int,
    val reason: String,
    val referenceId: String?,
    val occurredAt: Instant,
)

/** Immutable once written. The primary key on the week start is what makes settlement idempotent. */
@Entity(tableName = "weekly_settlements")
data class WeeklySettlementEntity(
    @PrimaryKey val weekStartDate: LocalDate,
    val baseEp: Int,
    val ventedEp: Int,
    val confidenceBasisPoints: Int,
    val streakBasisPoints: Int,
    val awardedEp: Int,
    val signalDays: Int,
    val goodWeek: Boolean,
    val budgetedMinor: Long,
    val spentMinor: Long,
    val bestCategoryId: Long?,
    val worstCategoryId: Long?,
    val settledAt: Instant,
)

/** The per-category detail behind a settlement, for the summary screen and reports. */
@Entity(
    tableName = "settlement_categories",
    primaryKeys = ["weekStartDate", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = WeeklySettlementEntity::class,
            parentColumns = ["weekStartDate"],
            childColumns = ["weekStartDate"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SettlementCategoryEntity(
    val weekStartDate: LocalDate,
    val categoryId: Long,
    val budgetedMinor: Long,
    val spentMinor: Long,
)

@Entity(tableName = "station_modules")
data class StationModuleEntity(
    @PrimaryKey val moduleKey: String,
    val level: Int,
    val unlockedAt: Instant?,
    val lastUpgradedAt: Instant?,
)

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey val id: String,
    val weekStartDate: LocalDate,
    val kind: String,
    val categoryId: Long?,
    val targetAmountMinor: Long?,
    val targetCount: Int?,
    val payoutEp: Int,
    val status: String,
)

/** Records which deadline reminders have already fired, so each threshold notifies exactly once. */
@Entity(tableName = "contract_notifications", primaryKeys = ["contractId", "thresholdDays"])
data class ContractNotificationEntity(
    val contractId: Long,
    val thresholdDays: Int,
    val notifiedOn: LocalDate,
)
