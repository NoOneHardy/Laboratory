package ch.no1hardy.orbit7.core.data.mapper

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
import ch.no1hardy.orbit7.core.domain.model.Budget
import ch.no1hardy.orbit7.core.domain.model.Cadence
import ch.no1hardy.orbit7.core.domain.model.Category
import ch.no1hardy.orbit7.core.domain.model.CategoryKey
import ch.no1hardy.orbit7.core.domain.model.CategorySettlement
import ch.no1hardy.orbit7.core.domain.model.Contract
import ch.no1hardy.orbit7.core.domain.model.ContractStatus
import ch.no1hardy.orbit7.core.domain.model.EnergyLedgerEntry
import ch.no1hardy.orbit7.core.domain.model.EnergyReason
import ch.no1hardy.orbit7.core.domain.model.Expense
import ch.no1hardy.orbit7.core.domain.model.ExpenseSource
import ch.no1hardy.orbit7.core.domain.model.Mission
import ch.no1hardy.orbit7.core.domain.model.MissionKind
import ch.no1hardy.orbit7.core.domain.model.MissionStatus
import ch.no1hardy.orbit7.core.domain.model.NoticePeriod
import ch.no1hardy.orbit7.core.domain.model.StationModuleKey
import ch.no1hardy.orbit7.core.domain.model.StationModuleState
import ch.no1hardy.orbit7.core.domain.model.WeeklySettlement
import ch.no1hardy.orbit7.core.domain.model.ZeroSpendMark
import ch.no1hardy.orbit7.core.domain.model.ZeroSpendSource
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.time.Week

/**
 * Entity ↔ domain mapping.
 *
 * Enums are stored as their names rather than as ordinals, so reordering an enum can never silently
 * reinterpret existing rows.
 */

fun CategoryEntity.toDomain(): Category =
    Category(
        id = id,
        key = key?.let { runCatching { CategoryKey.valueOf(it) }.getOrNull() },
        customName = customName,
        iconKey = iconKey,
        colorToken = colorToken,
        sortOrder = sortOrder,
        archivedAt = archivedAt,
    )

fun Category.toEntity(): CategoryEntity =
    CategoryEntity(
        id = id,
        key = key?.name,
        customName = customName,
        iconKey = iconKey,
        colorToken = colorToken,
        sortOrder = sortOrder,
        archivedAt = archivedAt,
    )

fun ExpenseEntity.toDomain(): Expense =
    Expense(
        id = id,
        amount = Money(amountMinor),
        currency = currency,
        categoryId = categoryId,
        occurredOn = occurredOn,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
        source = runCatching { ExpenseSource.valueOf(source) }.getOrDefault(ExpenseSource.MANUAL),
    )

fun Expense.toEntity(): ExpenseEntity =
    ExpenseEntity(
        id = id,
        amountMinor = amount.minor,
        currency = currency,
        categoryId = categoryId,
        occurredOn = occurredOn,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
        source = source.name,
    )

fun BudgetEntity.toDomain(): Budget =
    Budget(
        id = id,
        categoryId = categoryId,
        amountPerMonth = Money(amountMinorPerMonth),
        validFrom = validFrom,
        validTo = validTo,
    )

fun Budget.toEntity(): BudgetEntity =
    BudgetEntity(
        id = id,
        categoryId = categoryId,
        amountMinorPerMonth = amountPerMonth.minor,
        validFrom = validFrom,
        validTo = validTo,
    )

fun ContractEntity.toDomain(): Contract =
    Contract(
        id = id,
        name = name,
        categoryId = categoryId,
        amount = Money(amountMinor),
        cadence = Cadence.valueOf(cadence),
        nextChargeOn = nextChargeOn,
        noticePeriod = NoticePeriod(months = noticePeriodMonths, days = noticePeriodDays),
        earliestCancellationOn = earliestCancellationOn,
        status = ContractStatus.valueOf(status),
        previousAmount = previousAmountMinor?.let(::Money),
        statusChangedOn = statusChangedOn,
        createdAt = createdAt,
    )

fun Contract.toEntity(): ContractEntity =
    ContractEntity(
        id = id,
        name = name,
        categoryId = categoryId,
        amountMinor = amount.minor,
        cadence = cadence.name,
        nextChargeOn = nextChargeOn,
        noticePeriodMonths = noticePeriod.months,
        noticePeriodDays = noticePeriod.days,
        earliestCancellationOn = earliestCancellationOn,
        status = status.name,
        previousAmountMinor = previousAmount?.minor,
        statusChangedOn = statusChangedOn,
        createdAt = createdAt,
    )

fun ZeroSpendMarkEntity.toDomain(): ZeroSpendMark =
    ZeroSpendMark(
        date = date,
        markedAt = markedAt,
        source = ZeroSpendSource.valueOf(source),
    )

fun ZeroSpendMark.toEntity(): ZeroSpendMarkEntity =
    ZeroSpendMarkEntity(
        date = date,
        markedAt = markedAt,
        source = source.name,
    )

fun EnergyLedgerEntity.toDomain(): EnergyLedgerEntry =
    EnergyLedgerEntry(
        id = id,
        delta = delta,
        reason = EnergyReason.valueOf(reason),
        referenceId = referenceId,
        occurredAt = occurredAt,
    )

fun EnergyLedgerEntry.toEntity(): EnergyLedgerEntity =
    EnergyLedgerEntity(
        id = id,
        delta = delta,
        reason = reason.name,
        referenceId = referenceId,
        occurredAt = occurredAt,
    )

fun WeeklySettlementEntity.toDomain(): WeeklySettlement =
    WeeklySettlement(
        week = Week(weekStartDate),
        baseEp = baseEp,
        ventedEp = ventedEp,
        confidenceBasisPoints = confidenceBasisPoints,
        streakBasisPoints = streakBasisPoints,
        awardedEp = awardedEp,
        signalDays = signalDays,
        goodWeek = goodWeek,
        budgeted = Money(budgetedMinor),
        spent = Money(spentMinor),
        settledAt = settledAt,
        bestCategoryId = bestCategoryId,
        worstCategoryId = worstCategoryId,
    )

fun WeeklySettlement.toEntity(): WeeklySettlementEntity =
    WeeklySettlementEntity(
        weekStartDate = week.start,
        baseEp = baseEp,
        ventedEp = ventedEp,
        confidenceBasisPoints = confidenceBasisPoints,
        streakBasisPoints = streakBasisPoints,
        awardedEp = awardedEp,
        signalDays = signalDays,
        goodWeek = goodWeek,
        budgetedMinor = budgeted.minor,
        spentMinor = spent.minor,
        bestCategoryId = bestCategoryId,
        worstCategoryId = worstCategoryId,
        settledAt = settledAt,
    )

fun SettlementCategoryEntity.toDomain(): CategorySettlement =
    CategorySettlement(
        categoryId = categoryId,
        budgeted = Money(budgetedMinor),
        spent = Money(spentMinor),
    )

fun CategorySettlement.toEntity(week: Week): SettlementCategoryEntity =
    SettlementCategoryEntity(
        weekStartDate = week.start,
        categoryId = categoryId,
        budgetedMinor = budgeted.minor,
        spentMinor = spent.minor,
    )

fun StationModuleEntity.toDomain(): StationModuleState =
    StationModuleState(
        moduleKey = StationModuleKey.valueOf(moduleKey),
        level = level,
        unlockedAt = unlockedAt,
        lastUpgradedAt = lastUpgradedAt,
    )

fun StationModuleState.toEntity(): StationModuleEntity =
    StationModuleEntity(
        moduleKey = moduleKey.name,
        level = level,
        unlockedAt = unlockedAt,
        lastUpgradedAt = lastUpgradedAt,
    )

fun MissionEntity.toDomain(): Mission =
    Mission(
        id = id,
        week = Week(weekStartDate),
        kind = MissionKind.valueOf(kind),
        categoryId = categoryId,
        targetAmount = targetAmountMinor?.let(::Money),
        targetCount = targetCount,
        payoutEp = payoutEp,
        status = MissionStatus.valueOf(status),
    )

fun Mission.toEntity(): MissionEntity =
    MissionEntity(
        id = id,
        weekStartDate = week.start,
        kind = kind.name,
        categoryId = categoryId,
        targetAmountMinor = targetAmount?.minor,
        targetCount = targetCount,
        payoutEp = payoutEp,
        status = status.name,
    )
