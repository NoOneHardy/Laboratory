package ch.no1hardy.orbit7.feature.station

import app.cash.turbine.test
import ch.no1hardy.orbit7.core.domain.economy.StationCatalog
import ch.no1hardy.orbit7.core.domain.model.EnergyReason
import ch.no1hardy.orbit7.core.domain.model.StationModuleKey
import ch.no1hardy.orbit7.core.domain.test.FakeClock
import ch.no1hardy.orbit7.core.domain.test.FakeEnergyRepository
import ch.no1hardy.orbit7.core.domain.test.FakeStationRepository
import ch.no1hardy.orbit7.core.domain.test.aModule
import ch.no1hardy.orbit7.core.domain.usecase.PurchaseModuleUpgradeUseCase
import ch.no1hardy.orbit7.core.domain.usecase.PurchaseRejection
import ch.no1hardy.orbit7.core.testing.MainDispatcherRule
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class StationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = FakeClock()
    private val station = FakeStationRepository(listOf(aModule(level = 1)))
    private val energy = FakeEnergyRepository()
    private val catalog = StationCatalog()

    @Test
    fun `nothing affordable still names the nearest goal`() =
        runTest {
            val viewModel = viewModel()

            viewModel.state.test {
                awaitItem()
                val loaded = awaitItem()

                loaded.hasAnythingAffordable.shouldBeFalse()
                loaded.nearestGoal?.moduleKey shouldBe StationModuleKey.LIFE_SUPPORT
                loaded.nearestGoal?.epShortfall shouldBe 80L
            }
        }

    @Test
    fun `powering up spends EP and raises the level`() =
        runTest {
            energy.append(200, EnergyReason.SETTLEMENT, "week", clock.instant())
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onPowerUp(StationModuleKey.LIFE_SUPPORT)
                advanceUntilIdle()

                val effect = awaitItem()
                effect.shouldBeInstanceOf<StationEffect.PoweredUp>()
                effect.newLevel shouldBe 1
                effect.spentEp shouldBe 80L
            }
            energy.balance() shouldBe 120
            station.level(StationModuleKey.LIFE_SUPPORT) shouldBe 1
        }

    @Test
    fun `an unaffordable power-up is refused with the shortfall, and spends nothing`() =
        runTest {
            energy.append(10, EnergyReason.SETTLEMENT, "week", clock.instant())
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onPowerUp(StationModuleKey.LIFE_SUPPORT)
                advanceUntilIdle()

                val effect = awaitItem()
                effect.shouldBeInstanceOf<StationEffect.Refused>()
                effect.reason shouldBe PurchaseRejection.NOT_ENOUGH_ENERGY
                effect.shortfallEp shouldBe 70L
            }
            energy.balance() shouldBe 10
        }

    @Test
    fun `a locked module is refused even with plenty of energy`() =
        runTest {
            val darkStation = FakeStationRepository()
            energy.append(10_000, EnergyReason.SETTLEMENT, "week", clock.instant())
            val viewModel =
                StationViewModel(
                    darkStation,
                    energy,
                    catalog,
                    PurchaseModuleUpgradeUseCase(darkStation, energy, catalog, clock),
                )
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onPowerUp(StationModuleKey.HANGAR)
                advanceUntilIdle()

                val effect = awaitItem()
                effect.shouldBeInstanceOf<StationEffect.Refused>()
                effect.reason shouldBe PurchaseRejection.PREREQUISITE_MISSING
            }
            energy.balance() shouldBe 10_000
        }

    @Test
    fun `selecting a module opens its detail and clearing closes it`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onModuleSelected(StationModuleKey.REACTOR_CORE)
            advanceUntilIdle()
            viewModel.state.value.selectedOffer
                ?.moduleKey shouldBe StationModuleKey.REACTOR_CORE

            viewModel.onModuleSelected(null)
            advanceUntilIdle()
            (viewModel.state.value.selectedOffer == null).shouldBeTrue()
        }

    private fun viewModel() =
        StationViewModel(
            station = station,
            energy = energy,
            catalog = catalog,
            purchase = PurchaseModuleUpgradeUseCase(station, energy, catalog, clock),
        )
}
