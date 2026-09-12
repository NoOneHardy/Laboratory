package ch.no1hardy.orbit7

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.domain.model.StationModuleKey
import ch.no1hardy.orbit7.core.domain.repository.EnergyRepository
import ch.no1hardy.orbit7.core.domain.repository.SettlementRepository
import ch.no1hardy.orbit7.core.domain.repository.StationRepository
import ch.no1hardy.orbit7.core.domain.test.FakeClock
import ch.no1hardy.orbit7.core.domain.usecase.SettleDueWeeksUseCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * Journey 1 from `docs/06-test-strategy.md` §8: onboarding → seed categories and budgets → log
 * expenses across a week → advance the clock → settlement runs → summary is shown → unlock a
 * station module → the station graphic changes.
 *
 * Few, slow, high-value: this is the test that proves the whole loop actually closes.
 */
@HiltAndroidTest
class FullLoopEndToEndTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var clock: FakeClock

    @Inject lateinit var settleDueWeeks: SettleDueWeeksUseCase

    @Inject lateinit var settlements: SettlementRepository

    @Inject lateinit var energy: EnergyRepository

    @Inject lateinit var station: StationRepository

    @Before
    fun setUp() = hiltRule.inject()

    @Test
    fun theFullLoop() {
        // 1. Onboarding, taking the defaults.
        composeRule.onNodeWithTag(TestTags.ONBOARDING).assertIsDisplayed()
        composeRule.onNodeWithTag(TestTags.ONBOARDING_SKIP).performClick()
        composeRule.waitUntil {
            composeRule.onAllNodes(hasTestTag(TestTags.BRIDGE)).fetchSemanticsNodes().isNotEmpty()
        }

        // 2. A week of spending, entered the way a user would.
        repeat(3) {
            composeRule.onNodeWithTag(TestTags.FAB_QUICK_ADD).performClick()
            composeRule.onNodeWithTag(TestTags.numpadKey("1")).performClick()
            composeRule.onNodeWithTag(TestTags.numpadKey("0")).performClick()
            composeRule.onNodeWithTag(TestTags.numpadKey("0")).performClick()
            composeRule.onNodeWithTag(TestTags.numpadKey("0")).performClick()
            composeRule.onNodeWithTag(TestTags.SAVE).performClick()
            clock.advanceDays(1)
        }

        // 3. The week closes and settlement runs.
        clock.advanceWeeks(1)
        runBlocking { settleDueWeeks() }

        runBlocking { settlements.settlements() } shouldHaveSize 1
        runBlocking { energy.balance() } shouldBeGreaterThan 0

        // 4. The station changes when a module is powered up.
        val balance = runBlocking { energy.balance() }
        composeRule.onNodeWithTag("nav_station").performClick()
        composeRule
            .onNodeWithTag(
                TestTags.moduleTile(StationModuleKey.REACTOR_CORE.name),
            ).performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(TestTags.POWER_UP).performScrollTo().performClick()
        composeRule.waitForIdle()

        val levelAfter = runBlocking { station.level(StationModuleKey.REACTOR_CORE) }
        (levelAfter >= 2).shouldBeTrue()
        (runBlocking { energy.balance() } < balance).shouldBeTrue()
    }
}
