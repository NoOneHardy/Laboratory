package ch.no1hardy.orbit7.core.domain.usecase

import ch.no1hardy.orbit7.core.domain.economy.ContractSchedule
import ch.no1hardy.orbit7.core.domain.economy.SalvageCalculator
import ch.no1hardy.orbit7.core.domain.model.Cadence
import ch.no1hardy.orbit7.core.domain.model.ContractStatus
import ch.no1hardy.orbit7.core.domain.model.EnergyReason
import ch.no1hardy.orbit7.core.domain.model.NoticePeriod
import ch.no1hardy.orbit7.core.domain.test.FakeClock
import ch.no1hardy.orbit7.core.domain.test.FakeContractRepository
import ch.no1hardy.orbit7.core.domain.test.FakeEnergyRepository
import ch.no1hardy.orbit7.core.domain.test.aContract
import ch.no1hardy.orbit7.core.domain.test.chf
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ContractUseCasesTest {
    private val clock = FakeClock().apply { setDate(LocalDate.of(2025, 4, 14)) }
    private val contracts = FakeContractRepository(listOf(aContract(id = 1, amount = chf(45))))
    private val energy = FakeEnergyRepository()

    private val cancel = CancelContractUseCase(contracts, energy, SalvageCalculator(), clock)
    private val renegotiate = RenegotiateContractUseCase(contracts, energy, SalvageCalculator(), clock)

    @Test
    fun `cancelling pays six months of the monthly equivalent, once`() =
        runTest {
            val result = cancel(1)

            result.shouldBeInstanceOf<SalvageResult.Paid>()
            result.ep shouldBe 270
            energy.balance() shouldBe 270
            contracts.contract(1)?.status shouldBe ContractStatus.CANCELLED
            contracts.contract(1)?.statusChangedOn shouldBe LocalDate.of(2025, 4, 14)
        }

    @Test
    @DisplayName("a status cannot be toggled to farm payouts: the second cancel pays nothing")
    fun `salvage is paid once per transition`() =
        runTest {
            cancel(1)
            val second = cancel(1)

            second shouldBe SalvageResult.AlreadySalvaged
            energy.entries.filter { it.reason == EnergyReason.SALVAGE } shouldHaveSize 1
            energy.balance() shouldBe 270
        }

    @Test
    @DisplayName("a cancelled contract that comes back is a new contract, and pays nothing")
    fun `a re-entered contract pays nothing`() =
        runTest {
            cancel(1)
            val balanceAfterCancel = energy.balance()

            // The gym is re-joined: a brand new row, exactly as the rules require.
            val newId = contracts.upsert(aContract(id = 0, name = "Fitness Wankdorf", amount = chf(45)))

            newId shouldBe 2L
            energy.balance() shouldBe balanceAfterCancel
            energy.entries.filter { it.reason == EnergyReason.SALVAGE } shouldHaveSize 1
        }

    @Test
    fun `renegotiating pays on the delta and remembers the previous amount`() =
        runTest {
            contracts.upsert(aContract(id = 2, name = "Mobile", amount = chf(89), cadence = Cadence.MONTHLY))

            val result = renegotiate(2, chf(39))

            result.shouldBeInstanceOf<SalvageResult.Paid>()
            result.ep shouldBe 300 // 6 × (89 − 39)
            val updated = contracts.contract(2)
            updated?.status shouldBe ContractStatus.RENEGOTIATED
            updated?.amount shouldBe chf(39)
            updated?.previousAmount shouldBe chf(89)
            updated?.monthlySaving shouldBe chf(50)
        }

    @Test
    fun `a renegotiation upward is refused`() =
        runTest {
            renegotiate(1, chf(60)) shouldBe SalvageResult.NoSaving
            energy.balance() shouldBe 0
        }

    @Test
    fun `an unknown contract is reported, not crashed on`() =
        runTest {
            cancel(404) shouldBe SalvageResult.NotFound
        }

    @Test
    fun `saving a contract always derives the cancellation deadline from the notice period`() =
        runTest {
            val save = SaveContractUseCase(contracts, ContractSchedule(), clock)

            val id =
                save(
                    aContract(
                        id = 0,
                        name = "Krankenkasse",
                        nextChargeOn = LocalDate.of(2026, 1, 1),
                        noticePeriod = NoticePeriod.THREE_MONTHS,
                    ).copy(earliestCancellationOn = LocalDate.of(1970, 1, 1)),
                )

            contracts.contract(id)?.earliestCancellationOn shouldBe LocalDate.of(2025, 10, 1)
        }

    @Test
    fun `an explicit override wins over the derived date, for contracts with odd terms`() =
        runTest {
            val save = SaveContractUseCase(contracts, ContractSchedule(), clock)
            val odd = LocalDate.of(2025, 11, 15)

            val id = save(aContract(id = 0, nextChargeOn = LocalDate.of(2026, 1, 1)), overrideCancellationDate = odd)

            contracts.contract(id)?.earliestCancellationOn shouldBe odd
        }
}
