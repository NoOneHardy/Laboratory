package ch.no1hardy.orbit7

import ch.no1hardy.orbit7.core.domain.model.CategoryKey
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.repository.BackupRepository
import ch.no1hardy.orbit7.core.domain.repository.BudgetRepository
import ch.no1hardy.orbit7.core.domain.repository.CategoryRepository
import ch.no1hardy.orbit7.core.domain.repository.EnergyRepository
import ch.no1hardy.orbit7.core.domain.repository.ExpenseRepository
import ch.no1hardy.orbit7.core.domain.repository.ImportResult
import ch.no1hardy.orbit7.core.domain.test.FakeClock
import ch.no1hardy.orbit7.core.domain.usecase.LogExpenseUseCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import javax.inject.Inject

/**
 * Journey 3: export → wipe all data → import → the original state is back.
 *
 * This is the only backup the app has, so it is tested end to end rather than only at the
 * repository level.
 */
@HiltAndroidTest
class BackupEndToEndTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var clock: FakeClock

    @Inject lateinit var categories: CategoryRepository

    @Inject lateinit var budgets: BudgetRepository

    @Inject lateinit var expenses: ExpenseRepository

    @Inject lateinit var energy: EnergyRepository

    @Inject lateinit var backup: BackupRepository

    @Inject lateinit var logExpense: LogExpenseUseCase

    @Before
    fun setUp() = hiltRule.inject()

    @Test
    fun exportWipeImport() =
        runBlocking {
            clock.setDate(LocalDate.of(2025, 4, 14))
            val seeded = categories.seedDefaults(listOf(CategoryKey.GROCERIES, CategoryKey.EATING_OUT))
            budgets.setMonthlyBudget(seeded.first().id, Money.ofMajor(600), LocalDate.of(2025, 4, 1))
            logExpense(Money.ofMajor(12, 50), seeded.first().id)
            logExpense(Money.ofMajor(31, 80), seeded.last().id)
            energy.append(
                delta = 120,
                reason = ch.no1hardy.orbit7.core.domain.model.EnergyReason.SETTLEMENT,
                referenceId = "2025-04-07",
                at = clock.instant(),
            )

            val exported = backup.export()

            backup.wipeAllData()
            expenses.observeAll() // no-op read; the assertion below is what matters
            categories.categories() shouldHaveSize 0

            val result = backup.import(exported)

            result.shouldBeInstanceOf<ImportResult.Success>()
            result.expenses shouldBe 2
            categories.categories() shouldHaveSize 2
            budgets.allVersions() shouldHaveSize 1
            energy.balance() shouldBe 120
        }
}
