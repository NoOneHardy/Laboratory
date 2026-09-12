package ch.no1hardy.orbit7

import ch.no1hardy.orbit7.core.data.db.dao.ContractDao
import ch.no1hardy.orbit7.core.domain.economy.ContractSchedule
import ch.no1hardy.orbit7.core.domain.model.Cadence
import ch.no1hardy.orbit7.core.domain.model.Contract
import ch.no1hardy.orbit7.core.domain.model.ContractStatus
import ch.no1hardy.orbit7.core.domain.model.NoticePeriod
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.repository.ContractRepository
import ch.no1hardy.orbit7.core.domain.repository.EnergyRepository
import ch.no1hardy.orbit7.core.domain.test.FakeClock
import ch.no1hardy.orbit7.core.domain.usecase.CancelContractUseCase
import ch.no1hardy.orbit7.core.domain.usecase.SaveContractUseCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import javax.inject.Inject

/**
 * Journey 2: add a contract with a notice period → the deadline reminder fires at the 3-day
 * threshold → cancel → salvage EP appears in the ledger → the monthly fixed-cost total drops.
 */
@HiltAndroidTest
class ContractHunterEndToEndTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var clock: FakeClock

    @Inject lateinit var contracts: ContractRepository

    @Inject lateinit var contractDao: ContractDao

    @Inject lateinit var energy: EnergyRepository

    @Inject lateinit var schedule: ContractSchedule

    @Inject lateinit var saveContract: SaveContractUseCase

    @Inject lateinit var cancelContract: CancelContractUseCase

    @Before
    fun setUp() = hiltRule.inject()

    @Test
    fun contractHunter() =
        runBlocking {
            clock.setDate(LocalDate.of(2025, 8, 1))

            val id =
                saveContract(
                    Contract(
                        id = 0,
                        name = "Fitness Wankdorf",
                        categoryId = 1,
                        amount = Money.ofMajor(45),
                        cadence = Cadence.MONTHLY,
                        nextChargeOn = LocalDate.of(2026, 1, 1),
                        noticePeriod = NoticePeriod.THREE_MONTHS,
                        earliestCancellationOn = LocalDate.of(2026, 1, 1),
                        createdAt = clock.instant(),
                    ),
                )

            // The derived deadline is 1 October 2025: three calendar months before renewal.
            val contract = contracts.contract(id)!!
            contract.earliestCancellationOn shouldBe LocalDate.of(2025, 10, 1)

            // The 3-day threshold is the last one, and it fires exactly once.
            clock.setDate(LocalDate.of(2025, 9, 29))
            schedule.dueReminderThreshold(contract, clock.today(), emptySet()) shouldBe 3
            contractDao.recordNotification(
                ch.no1hardy.orbit7.core.data.db.entity
                    .ContractNotificationEntity(id, 3, clock.today()),
            )
            schedule.dueReminderThreshold(contract, clock.today(), contractDao.notifiedThresholds(id).toSet()) shouldBe
                null

            // Cancelling pays six months of the monthly cost, once, and the drain stops bleeding.
            val result = cancelContract(id)
            result.shouldBeInstanceOf<ch.no1hardy.orbit7.core.domain.usecase.SalvageResult.Paid>()
            result.ep shouldBe 270
            energy.balance() shouldBe 270
            contracts.contract(id)?.status shouldBe ContractStatus.CANCELLED
            contracts.contracts().filter { it.isActive }.sumOf { it.monthlyEquivalent.minor } shouldBe 0L
        }
}
