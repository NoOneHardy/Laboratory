package ch.no1hardy.orbit7.core.domain.usecase

import ch.no1hardy.orbit7.core.domain.economy.StationCatalog
import ch.no1hardy.orbit7.core.domain.model.EnergyReason
import ch.no1hardy.orbit7.core.domain.model.StationModuleKey
import ch.no1hardy.orbit7.core.domain.test.FakeClock
import ch.no1hardy.orbit7.core.domain.test.FakeEnergyRepository
import ch.no1hardy.orbit7.core.domain.test.FakeStationRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class StationUseCasesTest {
    private val clock = FakeClock()
    private val station = FakeStationRepository()
    private val energy = FakeEnergyRepository()
    private val purchase = PurchaseModuleUpgradeUseCase(station, energy, StationCatalog(), clock)

    @Test
    @DisplayName("a purchase spends EP by appending to the ledger, exactly once")
    fun `purchasing lights a module`() =
        runTest {
            energy.append(100, EnergyReason.SETTLEMENT, "week", clock.instant())

            val result = purchase(StationModuleKey.REACTOR_CORE)

            result.shouldBeInstanceOf<PurchaseResult.Success>()
            result.newLevel shouldBe 1
            result.spentEp shouldBe 60
            energy.balance() shouldBe 40
            energy.entries.filter { it.reason == EnergyReason.MODULE_PURCHASE } shouldHaveSize 1
            station.level(StationModuleKey.REACTOR_CORE) shouldBe 1
        }

    @Test
    fun `too little energy is refused with the shortfall stated`() =
        runTest {
            energy.append(25, EnergyReason.SETTLEMENT, "week", clock.instant())

            val result = purchase(StationModuleKey.REACTOR_CORE)

            result shouldBe PurchaseResult.Rejected(PurchaseRejection.NOT_ENOUGH_ENERGY, 35)
            energy.balance() shouldBe 25
            station.level(StationModuleKey.REACTOR_CORE) shouldBe 0
        }

    @Test
    fun `a module whose prerequisite is dark cannot be bought at any price`() =
        runTest {
            energy.append(10_000, EnergyReason.SETTLEMENT, "week", clock.instant())

            val result = purchase(StationModuleKey.HANGAR)

            result shouldBe PurchaseResult.Rejected(PurchaseRejection.PREREQUISITE_MISSING)
            energy.balance() shouldBe 10_000
        }

    @Test
    fun `a fully upgraded module is refused`() =
        runTest {
            energy.append(10_000, EnergyReason.SETTLEMENT, "week", clock.instant())
            repeat(StationModuleKey.REACTOR_CORE.maxLevel) { purchase(StationModuleKey.REACTOR_CORE) }

            purchase(StationModuleKey.REACTOR_CORE) shouldBe
                PurchaseResult.Rejected(PurchaseRejection.ALREADY_MAX_LEVEL)
            station.level(StationModuleKey.REACTOR_CORE) shouldBe 5
        }

    @Test
    fun `levels only ever increase, and each one costs its published price`() =
        runTest {
            energy.append(10_000, EnergyReason.SETTLEMENT, "week", clock.instant())

            val spends = (1..5).map { (purchase(StationModuleKey.REACTOR_CORE) as PurchaseResult.Success).spentEp }

            spends shouldBe listOf(60L, 110L, 190L, 350L, 630L)
            energy.balance() shouldBe 10_000 - 1_340
        }
}
