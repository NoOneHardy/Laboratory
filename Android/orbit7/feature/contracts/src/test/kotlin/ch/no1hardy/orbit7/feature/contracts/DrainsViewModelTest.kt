package ch.no1hardy.orbit7.feature.contracts

import app.cash.turbine.test
import ch.no1hardy.orbit7.core.domain.economy.ContractSchedule
import ch.no1hardy.orbit7.core.domain.economy.SalvageCalculator
import ch.no1hardy.orbit7.core.domain.model.Cadence
import ch.no1hardy.orbit7.core.domain.model.ContractStatus
import ch.no1hardy.orbit7.core.domain.test.FakeCategoryRepository
import ch.no1hardy.orbit7.core.domain.test.FakeClock
import ch.no1hardy.orbit7.core.domain.test.FakeContractRepository
import ch.no1hardy.orbit7.core.domain.test.FakeEnergyRepository
import ch.no1hardy.orbit7.core.domain.test.aCategory
import ch.no1hardy.orbit7.core.domain.test.aContract
import ch.no1hardy.orbit7.core.domain.test.chf
import ch.no1hardy.orbit7.core.domain.usecase.CancelContractUseCase
import ch.no1hardy.orbit7.core.domain.usecase.ObserveDrainsUseCase
import ch.no1hardy.orbit7.core.domain.usecase.RenegotiateContractUseCase
import ch.no1hardy.orbit7.core.testing.MainDispatcherRule
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class DrainsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = FakeClock().apply { setDate(LocalDate.of(2025, 9, 20)) }
    private val contracts =
        FakeContractRepository(
            listOf(
                aContract(id = 1, name = "Fitness", amount = chf(45)),
                aContract(id = 2, name = "Streaming", amount = chf(19, 90)),
                aContract(id = 3, name = "Versicherung", amount = chf(360), cadence = Cadence.YEARLY),
            ),
        )
    private val categories = FakeCategoryRepository(listOf(aCategory(id = 1)))
    private val energy = FakeEnergyRepository()

    @Test
    fun `drains are sorted by bleed rate, biggest target first`() =
        runTest {
            val viewModel = viewModel()

            viewModel.state.test {
                awaitItem()
                val loaded = awaitItem()

                loaded.drains.active.map { it.contract.name } shouldBe listOf("Versicherung", "Fitness", "Streaming")
                // 30.00 + 45.00 + 19.90 = 94.90 per month.
                loaded.drains.totalMonthlyBleed shouldBe chf(94, 90)
                loaded.drains.totalMonthlyBleedEp shouldBe 95
            }
        }

    @Test
    fun `cancelling is confirmed first, then pays salvage once`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onCancelRequested(1)
            advanceUntilIdle()
            viewModel.state.value.confirmingCancelId shouldBe 1L

            viewModel.effects.test {
                viewModel.onCancelConfirmed(1)
                advanceUntilIdle()

                val effect = awaitItem()
                effect.shouldBeInstanceOf<DrainsEffect.Salvaged>()
                effect.ep shouldBe 270

                // A second attempt on the same contract pays nothing.
                viewModel.onCancelConfirmed(1)
                advanceUntilIdle()
                awaitItem() shouldBe DrainsEffect.AlreadySalvaged
            }
            energy.balance() shouldBe 270
        }

    @Test
    fun `a cancelled contract moves to the salvaged section with its saving`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onCancelConfirmed(1)
            advanceUntilIdle()

            val state = viewModel.state.value
            state.drains.active shouldHaveSize 2
            state.drains.salvaged shouldHaveSize 1
            state.drains.salvaged
                .single()
                .contract.status shouldBe ContractStatus.CANCELLED
            state.drains.monthlySavingsAchieved shouldBe chf(45)
        }

    @Test
    fun `renegotiating upward is refused`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onRenegotiateConfirmed(1, chf(60))
                advanceUntilIdle()

                awaitItem() shouldBe DrainsEffect.NoSaving
            }
            energy.balance() shouldBe 0
        }

    private fun viewModel() =
        DrainsViewModel(
            observeDrains = ObserveDrainsUseCase(contracts, categories, ContractSchedule(), SalvageCalculator(), clock),
            cancelContract = CancelContractUseCase(contracts, energy, SalvageCalculator(), clock),
            renegotiateContract = RenegotiateContractUseCase(contracts, energy, SalvageCalculator(), clock),
        )
}
