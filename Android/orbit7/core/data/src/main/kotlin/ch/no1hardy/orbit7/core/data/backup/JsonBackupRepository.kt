package ch.no1hardy.orbit7.core.data.backup

import ch.no1hardy.orbit7.core.data.db.Orbit7Database
import ch.no1hardy.orbit7.core.data.db.entity.BudgetEntity
import ch.no1hardy.orbit7.core.data.db.entity.CategoryEntity
import ch.no1hardy.orbit7.core.data.db.entity.ContractEntity
import ch.no1hardy.orbit7.core.data.db.entity.EnergyLedgerEntity
import ch.no1hardy.orbit7.core.data.db.entity.ExpenseEntity
import ch.no1hardy.orbit7.core.data.db.entity.MissionEntity
import ch.no1hardy.orbit7.core.data.db.entity.SettlementCategoryEntity
import ch.no1hardy.orbit7.core.data.db.entity.StationModuleEntity
import ch.no1hardy.orbit7.core.data.db.entity.WeeklySettlementEntity
import ch.no1hardy.orbit7.core.data.db.entity.ZeroSpendMarkEntity
import ch.no1hardy.orbit7.core.domain.repository.BackupRepository
import ch.no1hardy.orbit7.core.domain.repository.ImportFailure
import ch.no1hardy.orbit7.core.domain.repository.ImportResult
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Export and import (`docs/05-architecture.md` §7).
 *
 * Import is **replace-all**, behind an explicit confirmation in the UI, and validates the schema
 * version before touching the database. Round-tripping export → wipe → import is an end-to-end test.
 */
@Singleton
class JsonBackupRepository
    @Inject
    constructor(
        private val database: Orbit7Database,
        private val clock: Clock,
        private val ioContext: CoroutineContext,
        private val appVersion: String,
    ) : BackupRepository {
        private val json =
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        override suspend fun export(): String =
            withContext(ioContext) {
                val document =
                    BackupDocument(
                        schemaVersion = BackupDocument.CURRENT_SCHEMA_VERSION,
                        exportedAt = clock.instant().toString(),
                        appVersion = appVersion,
                        categories =
                            database.categoryDao().all().map {
                                BackupCategory(
                                    it.id,
                                    it.key,
                                    it.customName,
                                    it.iconKey,
                                    it.colorToken,
                                    it.sortOrder,
                                    it.archivedAt?.toEpochMilli(),
                                )
                            },
                        expenses =
                            database.expenseDao().all().map {
                                BackupExpense(
                                    it.id,
                                    it.amountMinor,
                                    it.currency,
                                    it.categoryId,
                                    it.occurredOn.toString(),
                                    it.note,
                                    it.createdAt.toEpochMilli(),
                                    it.updatedAt.toEpochMilli(),
                                    it.source,
                                )
                            },
                        budgets =
                            database.budgetDao().all().map {
                                BackupBudget(
                                    it.id,
                                    it.categoryId,
                                    it.amountMinorPerMonth,
                                    it.validFrom.toString(),
                                    it.validTo?.toString(),
                                )
                            },
                        contracts =
                            database.contractDao().all().map {
                                BackupContract(
                                    it.id,
                                    it.name,
                                    it.categoryId,
                                    it.amountMinor,
                                    it.cadence,
                                    it.nextChargeOn.toString(),
                                    it.noticePeriodMonths,
                                    it.noticePeriodDays,
                                    it.earliestCancellationOn.toString(),
                                    it.status,
                                    it.previousAmountMinor,
                                    it.statusChangedOn?.toString(),
                                    it.createdAt.toEpochMilli(),
                                )
                            },
                        zeroSpendMarks =
                            database.zeroSpendDao().all().map {
                                BackupZeroSpendMark(it.date.toString(), it.markedAt.toEpochMilli(), it.source)
                            },
                        energyLedger =
                            database.energyLedgerDao().all().map {
                                BackupLedgerEntry(
                                    it.id,
                                    it.delta,
                                    it.reason,
                                    it.referenceId,
                                    it.occurredAt.toEpochMilli(),
                                )
                            },
                        settlements =
                            database.settlementDao().all().map {
                                BackupSettlement(
                                    it.weekStartDate.toString(),
                                    it.baseEp,
                                    it.ventedEp,
                                    it.confidenceBasisPoints,
                                    it.streakBasisPoints,
                                    it.awardedEp,
                                    it.signalDays,
                                    it.goodWeek,
                                    it.budgetedMinor,
                                    it.spentMinor,
                                    it.bestCategoryId,
                                    it.worstCategoryId,
                                    it.settledAt.toEpochMilli(),
                                )
                            },
                        settlementCategories =
                            database.settlementDao().all().flatMap { settlement ->
                                database.settlementDao().categoryDetail(settlement.weekStartDate).map {
                                    BackupSettlementCategory(
                                        it.weekStartDate.toString(),
                                        it.categoryId,
                                        it.budgetedMinor,
                                        it.spentMinor,
                                    )
                                }
                            },
                        stationModules =
                            database.stationDao().all().map {
                                BackupStationModule(
                                    it.moduleKey,
                                    it.level,
                                    it.unlockedAt?.toEpochMilli(),
                                    it.lastUpgradedAt?.toEpochMilli(),
                                )
                            },
                        missions =
                            database.missionDao().all().map {
                                BackupMission(
                                    it.id,
                                    it.weekStartDate.toString(),
                                    it.kind,
                                    it.categoryId,
                                    it.targetAmountMinor,
                                    it.targetCount,
                                    it.payoutEp,
                                    it.status,
                                )
                            },
                    )
                json.encodeToString(BackupDocument.serializer(), document)
            }

        override suspend fun import(json: String): ImportResult =
            withContext(ioContext) {
                val document =
                    try {
                        this@JsonBackupRepository.json.decodeFromString(BackupDocument.serializer(), json)
                    } catch (malformed: SerializationException) {
                        // Not swallowed: the failure becomes UI state with a readable reason, which is the
                        // defined behaviour for a corrupt import file (docs/05-architecture.md §8).
                        return@withContext ImportResult.Failure(ImportFailure.MALFORMED_JSON)
                    }
                if (document.schemaVersion > BackupDocument.CURRENT_SCHEMA_VERSION) {
                    return@withContext ImportResult.Failure(ImportFailure.UNSUPPORTED_SCHEMA_VERSION)
                }

                val categoryIds = document.categories.map { it.id }.toSet()
                val orphaned = document.expenses.any { it.categoryId !in categoryIds }
                if (orphaned) return@withContext ImportResult.Failure(ImportFailure.INCONSISTENT_DATA)

                // Replace-all, as confirmed by the user before we got here.
                wipeAllData()
                writeDocument(document)

                ImportResult.Success(
                    expenses = document.expenses.size,
                    contracts = document.contracts.size,
                    ledgerEntries = document.energyLedger.size,
                )
            }

        override suspend fun wipeAllData() =
            withContext(ioContext) {
                with(database) {
                    missionDao().deleteAll()
                    settlementDao().deleteAll()
                    energyLedgerDao().deleteAll()
                    zeroSpendDao().deleteAll()
                    contractDao().deleteAllNotifications()
                    contractDao().deleteAll()
                    stationDao().deleteAll()
                    expenseDao().deleteAll()
                    budgetDao().deleteAll()
                    categoryDao().deleteAll()
                }
            }

        private suspend fun writeDocument(document: BackupDocument) {
            document.categories.forEach {
                database.categoryDao().upsert(
                    CategoryEntity(
                        it.id,
                        it.key,
                        it.customName,
                        it.iconKey,
                        it.colorToken,
                        it.sortOrder,
                        it.archivedAt?.let(Instant::ofEpochMilli),
                    ),
                )
            }
            document.budgets.forEach {
                database.budgetDao().insert(
                    BudgetEntity(
                        it.id,
                        it.categoryId,
                        it.amountMinorPerMonth,
                        LocalDate.parse(it.validFrom),
                        it.validTo?.let(LocalDate::parse),
                    ),
                )
            }
            document.expenses.forEach {
                database.expenseDao().insert(
                    ExpenseEntity(
                        it.id,
                        it.amountMinor,
                        it.currency,
                        it.categoryId,
                        LocalDate.parse(it.occurredOn),
                        it.note,
                        Instant.ofEpochMilli(it.createdAt),
                        Instant.ofEpochMilli(it.updatedAt),
                        it.source,
                    ),
                )
            }
            document.contracts.forEach {
                database.contractDao().upsert(
                    ContractEntity(
                        it.id,
                        it.name,
                        it.categoryId,
                        it.amountMinor,
                        it.cadence,
                        LocalDate.parse(it.nextChargeOn),
                        it.noticePeriodMonths,
                        it.noticePeriodDays,
                        LocalDate.parse(it.earliestCancellationOn),
                        it.status,
                        it.previousAmountMinor,
                        it.statusChangedOn?.let(LocalDate::parse),
                        Instant.ofEpochMilli(it.createdAt),
                    ),
                )
            }
            document.zeroSpendMarks.forEach {
                database.zeroSpendDao().insert(
                    ZeroSpendMarkEntity(LocalDate.parse(it.date), Instant.ofEpochMilli(it.markedAt), it.source),
                )
            }
            document.energyLedger.forEach {
                database.energyLedgerDao().append(
                    EnergyLedgerEntity(it.id, it.delta, it.reason, it.referenceId, Instant.ofEpochMilli(it.occurredAt)),
                )
            }
            document.settlements.forEach {
                database.settlementDao().insert(
                    WeeklySettlementEntity(
                        LocalDate.parse(it.weekStartDate),
                        it.baseEp,
                        it.ventedEp,
                        it.confidenceBasisPoints,
                        it.streakBasisPoints,
                        it.awardedEp,
                        it.signalDays,
                        it.goodWeek,
                        it.budgetedMinor,
                        it.spentMinor,
                        it.bestCategoryId,
                        it.worstCategoryId,
                        Instant.ofEpochMilli(it.settledAt),
                    ),
                )
            }
            database.settlementDao().insertCategories(
                document.settlementCategories.map {
                    SettlementCategoryEntity(
                        LocalDate.parse(it.weekStartDate),
                        it.categoryId,
                        it.budgetedMinor,
                        it.spentMinor,
                    )
                },
            )
            document.stationModules.forEach {
                database.stationDao().upsert(
                    StationModuleEntity(
                        it.moduleKey,
                        it.level,
                        it.unlockedAt?.let(Instant::ofEpochMilli),
                        it.lastUpgradedAt?.let(Instant::ofEpochMilli),
                    ),
                )
            }
            database.missionDao().insertAll(
                document.missions.map {
                    MissionEntity(
                        it.id,
                        LocalDate.parse(it.weekStartDate),
                        it.kind,
                        it.categoryId,
                        it.targetAmountMinor,
                        it.targetCount,
                        it.payoutEp,
                        it.status,
                    )
                },
            )
        }
    }
