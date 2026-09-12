package ch.no1hardy.orbit7.core.data.backup

import kotlinx.serialization.Serializable

/**
 * The backup document (`docs/05-architecture.md` §7).
 *
 * One JSON file containing every table, a schema version and an export timestamp. This is the only
 * backup ORBIT-7 has — there is no cloud — so the format is explicit, boring and versioned.
 */
@Serializable
data class BackupDocument(
    val schemaVersion: Int,
    val exportedAt: String,
    val appVersion: String,
    val categories: List<BackupCategory> = emptyList(),
    val expenses: List<BackupExpense> = emptyList(),
    val budgets: List<BackupBudget> = emptyList(),
    val contracts: List<BackupContract> = emptyList(),
    val zeroSpendMarks: List<BackupZeroSpendMark> = emptyList(),
    val energyLedger: List<BackupLedgerEntry> = emptyList(),
    val settlements: List<BackupSettlement> = emptyList(),
    val settlementCategories: List<BackupSettlementCategory> = emptyList(),
    val stationModules: List<BackupStationModule> = emptyList(),
    val missions: List<BackupMission> = emptyList(),
) {
    companion object {
        /** Bumped whenever the document shape changes; an unknown version is refused on import. */
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

@Serializable
data class BackupCategory(
    val id: Long,
    val key: String?,
    val customName: String?,
    val iconKey: String,
    val colorToken: String,
    val sortOrder: Int,
    val archivedAt: Long?,
)

@Serializable
data class BackupExpense(
    val id: Long,
    val amountMinor: Long,
    val currency: String,
    val categoryId: Long,
    val occurredOn: String,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val source: String,
)

@Serializable
data class BackupBudget(
    val id: Long,
    val categoryId: Long,
    val amountMinorPerMonth: Long,
    val validFrom: String,
    val validTo: String?,
)

@Serializable
data class BackupContract(
    val id: Long,
    val name: String,
    val categoryId: Long,
    val amountMinor: Long,
    val cadence: String,
    val nextChargeOn: String,
    val noticePeriodMonths: Int,
    val noticePeriodDays: Int,
    val earliestCancellationOn: String,
    val status: String,
    val previousAmountMinor: Long?,
    val statusChangedOn: String?,
    val createdAt: Long,
)

@Serializable
data class BackupZeroSpendMark(
    val date: String,
    val markedAt: Long,
    val source: String,
)

@Serializable
data class BackupLedgerEntry(
    val id: Long,
    val delta: Int,
    val reason: String,
    val referenceId: String?,
    val occurredAt: Long,
)

@Serializable
data class BackupSettlement(
    val weekStartDate: String,
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
    val settledAt: Long,
)

@Serializable
data class BackupSettlementCategory(
    val weekStartDate: String,
    val categoryId: Long,
    val budgetedMinor: Long,
    val spentMinor: Long,
)

@Serializable
data class BackupStationModule(
    val moduleKey: String,
    val level: Int,
    val unlockedAt: Long?,
    val lastUpgradedAt: Long?,
)

@Serializable
data class BackupMission(
    val id: String,
    val weekStartDate: String,
    val kind: String,
    val categoryId: Long?,
    val targetAmountMinor: Long?,
    val targetCount: Int?,
    val payoutEp: Int,
    val status: String,
)
