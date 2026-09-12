package ch.no1hardy.orbit7.core.domain.economy

import ch.no1hardy.orbit7.core.domain.model.ModuleUnlock
import ch.no1hardy.orbit7.core.domain.model.StationModuleKey
import ch.no1hardy.orbit7.core.domain.test.aModule
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class StationCatalogTest {
    private val catalog = StationCatalog()

    @Test
    @DisplayName("the generated cost curve matches the table published in docs 02 §5.2, exactly")
    fun `published cost table`() {
        catalog.costs(StationModuleKey.REACTOR_CORE) shouldBe listOf(60L, 110L, 190L, 350L, 630L)
        catalog.costs(StationModuleKey.LIFE_SUPPORT) shouldBe listOf(80L, 140L, 260L, 470L)
        catalog.costs(StationModuleKey.HYDROPONICS) shouldBe listOf(90L, 160L, 290L, 520L)
        catalog.costs(StationModuleKey.COMMS_ARRAY) shouldBe listOf(140L, 250L, 450L)
        catalog.costs(StationModuleKey.OBSERVATION_DECK) shouldBe listOf(160L, 290L, 520L)
        catalog.costs(StationModuleKey.HANGAR) shouldBe listOf(200L, 360L, 650L, 1170L)
        catalog.costs(StationModuleKey.CRYO_LAB) shouldBe listOf(400L, 720L, 1300L)
    }

    @Test
    fun `every cost is a whole multiple of ten EP`() {
        StationModuleKey.entries.flatMap { catalog.costs(it) }.forEach { cost ->
            (cost % 10) shouldBe 0L
        }
    }

    @Test
    fun `everything needs the reactor lit first`() {
        val empty = emptyMap<StationModuleKey, ch.no1hardy.orbit7.core.domain.model.StationModuleState>()

        catalog.offer(StationModuleKey.REACTOR_CORE, empty, balanceEp = 1_000).prerequisiteMet.shouldBeTrue()
        catalog.offer(StationModuleKey.HANGAR, empty, balanceEp = 1_000).prerequisiteMet.shouldBeFalse()

        val reactorLit = mapOf(StationModuleKey.REACTOR_CORE to aModule(level = 1))
        catalog.offer(StationModuleKey.HANGAR, reactorLit, balanceEp = 1_000).prerequisiteMet.shouldBeTrue()
    }

    @Test
    fun `an offer reports the shortfall so the screen can say how far away the goal is`() {
        val offer = catalog.offer(StationModuleKey.REACTOR_CORE, emptyMap(), balanceEp = 25)

        offer.affordable.shouldBeFalse()
        offer.epShortfall shouldBe 35L
        offer.nextLevelCostEp shouldBe 60L
    }

    @Test
    fun `a fully upgraded module offers nothing further`() {
        val maxed =
            mapOf(
                StationModuleKey.CRYO_LAB to aModule(StationModuleKey.CRYO_LAB, level = 3),
                StationModuleKey.REACTOR_CORE to aModule(level = 1),
            )

        val offer = catalog.offer(StationModuleKey.CRYO_LAB, maxed, balanceEp = 10_000)

        offer.isMaxed.shouldBeTrue()
        offer.canPurchase.shouldBeFalse()
    }

    @Test
    fun `the nearest goal is the cheapest reachable upgrade`() {
        val states = mapOf(StationModuleKey.REACTOR_CORE to aModule(level = 1))

        val goal = catalog.nearestGoal(states, balanceEp = 0)

        goal?.moduleKey shouldBe StationModuleKey.LIFE_SUPPORT // 80 EP, against Reactor L2 at 110
    }

    @Test
    fun `feature gates open when the gating module reaches level one`() {
        val states =
            mapOf(
                StationModuleKey.REACTOR_CORE to aModule(level = 2),
                StationModuleKey.OBSERVATION_DECK to aModule(StationModuleKey.OBSERVATION_DECK, level = 1),
            )

        catalog.isUnlocked(ModuleUnlock.REPORTS, states).shouldBeTrue()
        catalog.isUnlocked(ModuleUnlock.MISSIONS, states).shouldBeFalse()
    }

    @Test
    fun `invested EP sums every level actually bought`() {
        val states =
            mapOf(
                StationModuleKey.REACTOR_CORE to aModule(level = 3), // 60 + 110 + 190
                StationModuleKey.LIFE_SUPPORT to aModule(StationModuleKey.LIFE_SUPPORT, level = 1), // 80
            )

        catalog.investedEp(states) shouldBe 440L
    }
}
