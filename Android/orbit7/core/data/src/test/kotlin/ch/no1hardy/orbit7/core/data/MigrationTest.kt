package ch.no1hardy.orbit7.core.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import ch.no1hardy.orbit7.core.data.db.Migrations
import ch.no1hardy.orbit7.core.data.db.Orbit7Database
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Migration tests, from version 1 (`docs/05-architecture.md` §5, hard rule 4).
 *
 * The schema JSONs are exported and committed, and every future migration adds a case here that
 * opens the previous schema, writes representative rows, migrates and asserts the data survived.
 * There is no cloud backup to restore from, so a destructive migration is never acceptable.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            Orbit7Database::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun `version 1 opens and carries its data`() {
        helper.createDatabase(TEST_DB, 1).use { database ->
            database.execSQL(
                """
                INSERT INTO categories (id, key, customName, iconKey, colorToken, sortOrder, archivedAt)
                VALUES (1, 'GROCERIES', NULL, 'cart', 'accent', 0, NULL)
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO expenses
                    (id, amountMinor, currency, categoryId, occurredOn, note, createdAt, updatedAt, source)
                VALUES (1, 1250, 'CHF', 1, '2025-04-14', NULL, 0, 0, 'MANUAL')
                """.trimIndent(),
            )
        }

        // With no migrations yet, re-opening at the current version must be a no-op that keeps data.
        val migrated = helper.runMigrationsAndValidate(TEST_DB, Orbit7Database.VERSION, true, *Migrations.ALL)

        migrated.query("SELECT amountMinor FROM expenses WHERE id = 1").use { cursor ->
            cursor.moveToFirst() shouldBe true
            cursor.getLong(0) shouldBe 1_250L
        }
    }

    @Test
    fun `every declared migration is covered by a test`() {
        // A migration without a test is the failure mode this assertion exists to prevent: when
        // Migrations.ALL grows, this test fails until a case above is added for the new version.
        Migrations.ALL.size shouldBe COVERED_MIGRATIONS
    }

    private companion object {
        const val TEST_DB = "orbit7-migration-test"
        const val COVERED_MIGRATIONS = 0
    }
}
