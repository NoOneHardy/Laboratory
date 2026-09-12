package ch.no1hardy.orbit7.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import ch.no1hardy.orbit7.core.data.backup.BackupDocument
import ch.no1hardy.orbit7.core.data.backup.JsonBackupRepository
import ch.no1hardy.orbit7.core.data.db.Orbit7Database
import ch.no1hardy.orbit7.core.data.db.entity.CategoryEntity
import ch.no1hardy.orbit7.core.data.db.entity.EnergyLedgerEntity
import ch.no1hardy.orbit7.core.data.db.entity.ExpenseEntity
import ch.no1hardy.orbit7.core.domain.repository.ImportFailure
import ch.no1hardy.orbit7.core.domain.repository.ImportResult
import ch.no1hardy.orbit7.core.domain.test.FakeClock
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate

/**
 * Export → wipe → import must land on exactly the state it started from
 * (`docs/05-architecture.md` §7). This is the only backup ORBIT-7 has.
 */
@RunWith(RobolectricTestRunner::class)
class BackupRoundTripTest {
    private lateinit var database: Orbit7Database
    private lateinit var backup: JsonBackupRepository
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
        backup = JsonBackupRepository(database, FakeClock(), Dispatchers.Unconfined, "1.0.0-test")
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `a round trip restores every row`() =
        runTest {
            seed()
            val exported = backup.export()

            backup.wipeAllData()
            database.expenseDao().all() shouldHaveSize 0

            val result = backup.import(exported)

            result.shouldBeInstanceOf<ImportResult.Success>()
            result.expenses shouldBe 2
            database.categoryDao().all() shouldHaveSize 1
            database.expenseDao().all() shouldHaveSize 2
            database.energyLedgerDao().balance() shouldBe 120
        }

    @Test
    fun `a malformed file is refused with a readable reason and changes nothing`() =
        runTest {
            seed()

            val result = backup.import("{ this is not json")

            result shouldBe ImportResult.Failure(ImportFailure.MALFORMED_JSON)
            database.expenseDao().all() shouldHaveSize 2
        }

    @Test
    fun `a newer schema version is refused rather than half-imported`() =
        runTest {
            seed()
            val future =
                """
                {"schemaVersion": ${BackupDocument.CURRENT_SCHEMA_VERSION + 1},
                 "exportedAt": "2025-04-14T08:00:00Z", "appVersion": "9.9.9"}
                """.trimIndent()

            val result = backup.import(future)

            result shouldBe ImportResult.Failure(ImportFailure.UNSUPPORTED_SCHEMA_VERSION)
            database.expenseDao().all() shouldHaveSize 2
        }

    @Test
    fun `an expense referencing a missing category is refused`() =
        runTest {
            val orphaned =
                """
                {"schemaVersion": 1, "exportedAt": "2025-04-14T08:00:00Z", "appVersion": "1.0.0",
                 "expenses": [{"id":1,"amountMinor":1250,"currency":"CHF","categoryId":99,
                               "occurredOn":"2025-04-14","note":null,"createdAt":0,"updatedAt":0,
                               "source":"MANUAL"}]}
                """.trimIndent()

            backup.import(orphaned) shouldBe ImportResult.Failure(ImportFailure.INCONSISTENT_DATA)
        }

    private suspend fun seed() {
        val categoryId =
            database.categoryDao().upsert(
                CategoryEntity(
                    key = "GROCERIES",
                    customName = null,
                    iconKey = "cart",
                    colorToken = "accent",
                    sortOrder = 0,
                ),
            )
        repeat(2) { index ->
            database.expenseDao().insert(
                ExpenseEntity(
                    amountMinor = 1_250 + index,
                    currency = "CHF",
                    categoryId = categoryId,
                    occurredOn = LocalDate.of(2025, 4, 14).plusDays(index.toLong()),
                    note = null,
                    createdAt = now,
                    updatedAt = now,
                    source = "MANUAL",
                ),
            )
        }
        database.energyLedgerDao().append(
            EnergyLedgerEntity(delta = 120, reason = "SETTLEMENT", referenceId = "2025-04-07", occurredAt = now),
        )
    }
}
