package ch.no1hardy.orbit7.core.domain.test

import ch.no1hardy.orbit7.core.domain.model.Budget
import ch.no1hardy.orbit7.core.domain.model.Cadence
import ch.no1hardy.orbit7.core.domain.model.Category
import ch.no1hardy.orbit7.core.domain.model.CategoryKey
import ch.no1hardy.orbit7.core.domain.model.Contract
import ch.no1hardy.orbit7.core.domain.model.ContractStatus
import ch.no1hardy.orbit7.core.domain.model.Expense
import ch.no1hardy.orbit7.core.domain.model.NoticePeriod
import ch.no1hardy.orbit7.core.domain.model.StationModuleKey
import ch.no1hardy.orbit7.core.domain.model.StationModuleState
import ch.no1hardy.orbit7.core.domain.model.ZeroSpendMark
import ch.no1hardy.orbit7.core.domain.model.ZeroSpendSource
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.time.Week
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/**
 * Builders with sensible defaults (`docs/06-test-strategy.md` §9).
 *
 * A test names only what it is actually about; everything else comes from here.
 */
object Fixtures {
    val EPOCH: Instant = Instant.parse("2025-01-01T00:00:00Z")

    const val GROCERIES = 1L
    const val EATING_OUT = 2L
    const val TRANSPORT = 3L
}

/** `chf(12, 50)` is CHF 12.50 — money is integers all the way down, in tests too. */
fun chf(
    major: Long,
    rappen: Long = 0,
): Money = Money.ofMajor(major, rappen)

fun aCategory(
    id: Long = Fixtures.GROCERIES,
    key: CategoryKey? = CategoryKey.GROCERIES,
    customName: String? = null,
    sortOrder: Int = 0,
    archivedAt: Instant? = null,
): Category =
    Category(
        id = id,
        key = key,
        customName = customName,
        iconKey = "cart",
        colorToken = "accent",
        sortOrder = sortOrder,
        archivedAt = archivedAt,
    )

fun anExpense(
    amount: Money = chf(12, 50),
    on: LocalDate = LocalDate.of(2025, 4, 14),
    categoryId: Long = Fixtures.GROCERIES,
    id: Long = 0,
    note: String? = null,
    createdAt: Instant = Fixtures.EPOCH,
): Expense =
    Expense(
        id = id,
        amount = amount,
        currency = "CHF",
        categoryId = categoryId,
        occurredOn = on,
        note = note,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

fun aBudget(
    categoryId: Long = Fixtures.GROCERIES,
    monthly: Money = chf(400),
    validFrom: LocalDate = LocalDate.of(2020, 1, 1),
    validTo: LocalDate? = null,
    id: Long = 0,
): Budget =
    Budget(
        id = id,
        categoryId = categoryId,
        amountPerMonth = monthly,
        validFrom = validFrom,
        validTo = validTo,
    )

fun aContract(
    id: Long = 1,
    name: String = "Fitness Wankdorf",
    amount: Money = chf(45),
    cadence: Cadence = Cadence.MONTHLY,
    categoryId: Long = Fixtures.GROCERIES,
    nextChargeOn: LocalDate = LocalDate.of(2026, 1, 1),
    noticePeriod: NoticePeriod = NoticePeriod.THREE_MONTHS,
    status: ContractStatus = ContractStatus.ACTIVE,
    previousAmount: Money? = null,
    statusChangedOn: LocalDate? = null,
): Contract =
    Contract(
        id = id,
        name = name,
        categoryId = categoryId,
        amount = amount,
        cadence = cadence,
        nextChargeOn = nextChargeOn,
        noticePeriod = noticePeriod,
        earliestCancellationOn = noticePeriod.latestNoticeDateFor(nextChargeOn),
        status = status,
        previousAmount = previousAmount,
        statusChangedOn = statusChangedOn,
        createdAt = Fixtures.EPOCH,
    )

fun aMark(
    on: LocalDate,
    source: ZeroSpendSource = ZeroSpendSource.APP,
): ZeroSpendMark = ZeroSpendMark(date = on, markedAt = Fixtures.EPOCH, source = source)

fun aModule(
    key: StationModuleKey = StationModuleKey.REACTOR_CORE,
    level: Int = 1,
): StationModuleState =
    StationModuleState(
        moduleKey = key,
        level = level,
        unlockedAt = Fixtures.EPOCH,
        lastUpgradedAt = Fixtures.EPOCH,
    )

/** The Monday-based week containing [date]. */
fun aWeek(date: LocalDate = LocalDate.of(2025, 4, 14)): Week = Week.containing(date, DayOfWeek.MONDAY)

/** The Monday of the week containing [date]. */
fun mondayOf(date: LocalDate): LocalDate = aWeek(date).start
