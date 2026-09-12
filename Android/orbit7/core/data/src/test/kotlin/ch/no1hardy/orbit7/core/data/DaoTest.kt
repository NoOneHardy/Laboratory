package ch.no1hardy.orbit7.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import ch.no1hardy.orbit7.core.data.db.Orbit7Database
import ch.no1hardy.orbit7.core.data.db.entity.CategoryEntity
import ch.no1hardy.orbit7.core.data.db.entity.EnergyLedgerEntity
import ch.no1hardy.orbit7.core.data.db.entity.ExpenseEntity
import ch.no1hardy.orbit7.core.data.db.entity.StationModuleEntity
import ch.no1hardy.orbit7.core.data.db.entity.WeeklySettlementEntity
import ch.no1hardy.orbit7.core.data.db.entity.ZeroSpendMarkEntity
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate

/**
 * One suite per table, covering CRUD and the invariant each table carries
 * (`docs/06-test-strategy.md` §4).
 */
@RunWith(RobolectricTestRunner::class)
class DaoTest {
    private lateinit var database: Orbit7Database
    private val now: Instant = Instant.parse("2025-04-14T08:00:00Z")

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    Orbit7Database::class.java,
                ).allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `categories are archived, never deleted, and still resolve for old expenses`() =
        runTest {
            val id = database.categoryDao().upsert(category())
            database.expenseDao().insert(expense(categoryId = id))

            database.categoryDao().archive(id, now)

            database.categoryDao().all() shouldHaveSize 1
            database.categoryDao().byId(id)?.archivedAt shouldBe now
            // The expense still resolves to the archived category.
            database
                .expenseDao()
                .all()
                .single()
                .categoryId shouldBe id
        }

    @Test
    fun `a date range includes its first and last day exactly once`() =
        runTest {
            val categoryId = database.categoryDao().upsert(category())
            val monday = LocalDate.of(2025, 4, 14)
            listOf(monday.minusDays(1), monday, monday.plusDays(3), monday.plusDays(6), monday.plusDays(7))
                .forEach { database.expenseDao().insert(expense(categoryId = categoryId, on = it)) }

            val week = database.expenseDao().between(monday, monday.plusDays(6))

            week shouldHaveSize 3
            week.map { it.occurredOn } shouldBe listOf(monday, monday.plusDays(3), monday.plusDays(6))
        }

    @Test
    fun `budget versions never overlap and exactly one row stays open`() =
        runTest {
            val categoryId = database.categoryDao().upsert(category())
            val dao = database.budgetDao()

            dao.openNewVersion(categoryId, 40_000, LocalDate.of(2025, 1, 1))
            dao.openNewVersion(categoryId, 50_000, LocalDate.of(2025, 4, 1))
            dao.openNewVersion(categoryId, 45_000, LocalDate.of(2025, 6, 1))

            val versions = dao.all().sortedBy { it.validFrom }
            versions shouldHaveSize 3
            versions.count { it.validTo == null } shouldBe 1
            versions[0].validTo shouldBe LocalDate.of(2025, 3, 31)
            versions[1].validTo shouldBe LocalDate.of(2025, 5, 31)

            // The version in force on an arbitrary past date is the right one.
            val march = dao.versionsCovering(LocalDate.of(2025, 3, 15), LocalDate.of(2025, 3, 15))
            march.single().amountMinorPerMonth shouldBe 40_000
        }

    @Test
    fun `marking a day twice is idempotent and keeps the original source`() =
        runTest {
            val dao = database.zeroSpendDao()
            val date = LocalDate.of(2025, 4, 14)

            dao.insert(ZeroSpendMarkEntity(date, now, "NOTIFICATION"))
            dao.insert(ZeroSpendMarkEntity(date, now.plusSeconds(60), "APP"))

            val marks = dao.all()
            marks shouldHaveSize 1
            marks.single().source shouldBe "NOTIFICATION"
            dao.isMarked(date).shouldBeTrue()
        }

    @Test
    fun `the balance equals the sum of the ledger`() =
        runTest {
            val dao = database.energyLedgerDao()
            listOf(120, -60, 270, -30).forEach { delta ->
                dao.append(
                    EnergyLedgerEntity(delta = delta, reason = "SETTLEMENT", referenceId = null, occurredAt = now),
                )
            }

            dao.balance() shouldBe 300
            dao.balance() shouldBe dao.all().sumOf { it.delta }
            dao.lifetimeEarned() shouldBe 390
        }

    @Test
    fun `a salvage reference can be looked up, so a payout is never made twice`() =
        runTest {
            val dao = database.energyLedgerDao()
            dao.append(
                EnergyLedgerEntity(
                    delta = 270,
                    reason = "SALVAGE",
                    referenceId = "contract:1:CANCELLED",
                    occurredAt = now,
                ),
            )

            dao.hasEntryFor("SALVAGE", "contract:1:CANCELLED").shouldBeTrue()
            dao.hasEntryFor("SALVAGE", "contract:2:CANCELLED").shouldBeFalse()
        }

    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun `settling the same week twice fails rather than duplicating`() =
        runTest {
            val settlement = settlement(LocalDate.of(2025, 4, 7))
            database.settlementDao().insert(settlement)
            database.settlementDao().insert(settlement.copy(awardedEp = 999))
        }

    @Test
    fun `a module level only ever increases`() =
        runTest {
            val dao = database.stationDao()
            dao.upsert(StationModuleEntity("REACTOR_CORE", 1, now, now))
            dao.upsert(StationModuleEntity("REACTOR_CORE", 2, now, now))

            dao.all() shouldHaveSize 1
            dao.level("REACTOR_CORE") shouldBe 2
        }

    @Test
    fun `the earliest expense and mark dates anchor the settlement catch-up`() =
        runTest {
            val categoryId = database.categoryDao().upsert(category())
            database.expenseDao().insert(expense(categoryId = categoryId, on = LocalDate.of(2025, 3, 3)))
            database.expenseDao().insert(expense(categoryId = categoryId, on = LocalDate.of(2025, 4, 14)))
            database.zeroSpendDao().insert(ZeroSpendMarkEntity(LocalDate.of(2025, 2, 24), now, "APP"))

            database.expenseDao().earliestDate() shouldBe LocalDate.of(2025, 3, 3)
            database.zeroSpendDao().earliestDate() shouldBe LocalDate.of(2025, 2, 24)
        }

    @Test
    fun `settlement category detail is stored alongside the settlement`() =
        runTest {
            val week = LocalDate.of(2025, 4, 7)
            database.settlementDao().insert(settlement(week))
            database.settlementDao().insertCategories(
                listOf(
                    ch.no1hardy.orbit7.core.data.db.entity
                        .SettlementCategoryEntity(week, 1, 9_333, 7_820),
                ),
            )

            val detail = database.settlementDao().categoryDetail(week)
            detail shouldHaveSize 1
            detail.single().spentMinor shouldBe 7_820
            database.settlementDao().latest().shouldNotBeNull()
        }

    private fun category() =
        CategoryEntity(
            key = "GROCERIES",
            customName = null,
            iconKey = "cart",
            colorToken = "accent",
            sortOrder = 0,
        )

    private fun expense(
        categoryId: Long,
        on: LocalDate = LocalDate.of(2025, 4, 14),
    ) = ExpenseEntity(
        amountMinor = 1_250,
        currency = "CHF",
        categoryId = categoryId,
        occurredOn = on,
        note = null,
        createdAt = now,
        updatedAt = now,
        source = "MANUAL",
    )

    private fun settlement(week: LocalDate) =
        WeeklySettlementEntity(
            weekStartDate = week,
            baseEp = 26,
            ventedEp = 7,
            confidenceBasisPoints = 9_143,
            streakBasisPoints = 11_500,
            awardedEp = 20,
            signalDays = 6,
            goodWeek = true,
            budgetedMinor = 16_333,
            spentMinor = 15_160,
            bestCategoryId = 1,
            worstCategoryId = 2,
            settledAt = now,
        )
}
