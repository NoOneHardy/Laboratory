package ch.no1hardy.orbit7.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import ch.no1hardy.orbit7.core.data.notification.Orbit7Notifier
import ch.no1hardy.orbit7.core.data.work.ContractDeadlineWorker
import ch.no1hardy.orbit7.core.data.work.DailyReminderWorker
import ch.no1hardy.orbit7.core.domain.economy.ContractSchedule
import ch.no1hardy.orbit7.core.domain.model.Contract
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.test.FakeBudgetRepository
import ch.no1hardy.orbit7.core.domain.test.FakeClock
import ch.no1hardy.orbit7.core.domain.test.FakeContractRepository
import ch.no1hardy.orbit7.core.domain.test.FakeExpenseRepository
import ch.no1hardy.orbit7.core.domain.test.FakeSettingsRepository
import ch.no1hardy.orbit7.core.domain.test.aBudget
import ch.no1hardy.orbit7.core.domain.test.aContract
import ch.no1hardy.orbit7.core.domain.test.anExpense
import ch.no1hardy.orbit7.core.domain.test.chf
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * Worker tests (`docs/06-test-strategy.md` §4).
 *
 * The settlement catch-up itself is covered by the domain tests; what matters here is the
 * notification behaviour: the daily reminder stays quiet on days the app was used, and each contract
 * deadline threshold fires exactly once.
 */
@RunWith(RobolectricTestRunner::class)
class WorkerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = FakeClock().apply { setDate(LocalDate.of(2025, 4, 16)) }
    private val notifier = RecordingNotifier()

    @Test
    fun `the daily reminder is skipped when the app was already used today`() =
        runTest {
            val settings = FakeSettingsRepository()
            settings.update { it.copy(lastAppUsageOn = LocalDate.of(2025, 4, 16)) }

            val worker = dailyReminderWorker(settings)

            worker.doWork() shouldBe ListenableWorker.Result.success()
            notifier.dailyReminders shouldHaveSize 0
        }

    @Test
    fun `the daily reminder reports the week's headroom and today's spend`() =
        runTest {
            val expenses = FakeExpenseRepository()
            expenses.add(anExpense(chf(20), LocalDate.of(2025, 4, 16)))
            val budgets =
                FakeBudgetRepository(listOf(aBudget(monthly = chf(300), validFrom = LocalDate.of(2025, 4, 1))))

            val worker = dailyReminderWorker(FakeSettingsRepository(), expenses, budgets)

            worker.doWork() shouldBe ListenableWorker.Result.success()
            notifier.dailyReminders shouldHaveSize 1
            // 300 × 7 / 30 = 70.00 budgeted for the week, 20.00 spent.
            notifier.dailyReminders.single().first shouldBe chf(50)
            notifier.dailyReminders.single().second shouldBe chf(20)
        }

    @Test
    fun `the daily reminder is cancelled when the setting is off`() =
        runTest {
            val settings = FakeSettingsRepository()
            settings.update { it.copy(dailyReminderEnabled = false) }

            dailyReminderWorker(settings).doWork() shouldBe ListenableWorker.Result.success()

            notifier.dailyReminders shouldHaveSize 0
            notifier.cancellations shouldBe 1
        }

    @Test
    fun `each deadline threshold notifies once, and the record is what stops a repeat`() =
        runTest {
            val contract = aContract(id = 1, nextChargeOn = LocalDate.of(2026, 1, 1)) // deadline 1 Oct 2025
            val contracts = FakeContractRepository(listOf(contract))
            val database = inMemoryDatabase()

            // 30 days before the deadline.
            clock.setDate(LocalDate.of(2025, 9, 1))
            deadlineWorker(contracts, database).doWork() shouldBe ListenableWorker.Result.success()
            notifier.deadlines shouldHaveSize 1

            // Four days later the 30-day threshold must not fire again.
            clock.setDate(LocalDate.of(2025, 9, 5))
            deadlineWorker(contracts, database).doWork()
            notifier.deadlines shouldHaveSize 1

            // 14 days out is a new threshold, and does fire.
            clock.setDate(LocalDate.of(2025, 9, 17))
            deadlineWorker(contracts, database).doWork()
            notifier.deadlines shouldHaveSize 2
        }

    private fun dailyReminderWorker(
        settings: FakeSettingsRepository,
        expenses: FakeExpenseRepository = FakeExpenseRepository(),
        budgets: FakeBudgetRepository = FakeBudgetRepository(),
    ): DailyReminderWorker =
        TestListenableWorkerBuilder<DailyReminderWorker>(context)
            .setWorkerFactory(
                workerFactory { _, parameters ->
                    DailyReminderWorker(context, parameters, expenses, budgets, settings, notifier, clock)
                },
            ).build()

    private fun deadlineWorker(
        contracts: FakeContractRepository,
        database: ch.no1hardy.orbit7.core.data.db.Orbit7Database,
    ): ContractDeadlineWorker =
        TestListenableWorkerBuilder<ContractDeadlineWorker>(context)
            .setWorkerFactory(
                workerFactory { _, parameters ->
                    ContractDeadlineWorker(
                        context,
                        parameters,
                        contracts,
                        database.contractDao(),
                        FakeSettingsRepository(),
                        ContractSchedule(),
                        notifier,
                        clock,
                    )
                },
            ).build()

    private fun inMemoryDatabase() =
        androidx.room.Room
            .inMemoryDatabaseBuilder(
                context,
                ch.no1hardy.orbit7.core.data.db.Orbit7Database::class.java,
            ).allowMainThreadQueries()
            .build()

    private fun workerFactory(
        create: (Context, androidx.work.WorkerParameters) -> ListenableWorker,
    ): androidx.work.WorkerFactory =
        object : androidx.work.WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: androidx.work.WorkerParameters,
            ): ListenableWorker = create(appContext, workerParameters)
        }

    /** Records what would have been posted, so the assertions are about behaviour, not about pixels. */
    private class RecordingNotifier : Orbit7Notifier {
        val dailyReminders = mutableListOf<Pair<Money, Money>>()
        val deadlines = mutableListOf<Pair<Contract, Int>>()
        var settlements = 0
        var cancellations = 0

        override fun postDailyReminder(
            remainingThisWeek: Money,
            spentToday: Money,
        ) {
            dailyReminders += remainingThisWeek to spentToday
        }

        override fun postContractDeadline(
            contract: Contract,
            daysRemaining: Int,
        ) {
            deadlines += contract to daysRemaining
        }

        override fun postSettlementReady(awardedEp: Int) {
            settlements++
        }

        override fun cancelDailyReminder() {
            cancellations++
        }
    }
}
