package ch.no1hardy.orbit7.feature.station

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.test.aModule
import ch.no1hardy.orbit7.core.domain.test.anExpense
import ch.no1hardy.orbit7.core.domain.test.chf
import ch.no1hardy.orbit7.core.domain.time.Week
import ch.no1hardy.orbit7.core.domain.usecase.BridgeState
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "de-rCH-w411dp-h891dp")
class BridgeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2025, 4, 16)

    @Test
    fun `the first run explains the loop instead of showing zeros`() {
        setContent(BridgeUiState.Ready(bridge(hasAnyData = false), firstRun = true))

        composeRule.onNodeWithTag(TestTags.BRIDGE).assertExists()
        composeRule.onNodeWithTag(TestTags.EMPTY_STATE).assertExists()
        composeRule.onNodeWithTag(TestTags.WEEK_GAUGE).assertDoesNotExist()
    }

    @Test
    fun `the loaded state shows the gauge, the readout and the today card`() {
        setContent(BridgeUiState.Ready(bridge(), firstRun = false))

        composeRule.onNodeWithTag(TestTags.POWER_READOUT).assertExists()
        composeRule.onNodeWithTag(TestTags.PROVISIONAL_EP).assertExists()
        composeRule.onNodeWithTag(TestTags.WEEK_GAUGE).assertExists()
        composeRule.onNodeWithTag(TestTags.TODAY_CARD).assertExists()
        composeRule.onNodeWithTag(TestTags.STREAK_CHIP).assertExists()
    }

    @Test
    fun `the zero-spend mark is offered on a quiet day and invokes the callback`() {
        var marked = 0
        setContent(
            state = BridgeUiState.Ready(bridge(todayEntries = emptyList()), firstRun = false),
            onMarkZeroSpendDay = { marked++ },
        )

        composeRule.onNodeWithTag(TestTags.ZERO_SPEND_MARK).performClick()

        marked shouldBe 1
    }

    @Test
    fun `a marked day flips to the confirmed state and can be undone`() {
        var undone = 0
        setContent(
            state =
                BridgeUiState.Ready(
                    bridge(todayEntries = emptyList(), zeroSpendMarked = true),
                    firstRun = false,
                ),
            onUndoZeroSpendDay = { undone++ },
        )

        composeRule.onNodeWithTag(TestTags.ZERO_SPEND_MARK).assertDoesNotExist()
        composeRule.onNodeWithTag(TestTags.ZERO_SPEND_UNDO).performClick()

        undone shouldBe 1
    }

    @Test
    fun `a settled week offers its summary`() {
        var opened: String? = null
        setContent(
            state = BridgeUiState.Ready(bridge(pendingWeekId = "2025-04-07"), firstRun = false),
            onOpenSettlementSummary = { opened = it },
        )

        composeRule.onNodeWithTag(TestTags.SETTLEMENT_BANNER).assertExists()
    }

    private fun bridge(
        hasAnyData: Boolean = true,
        todayEntries: List<ch.no1hardy.orbit7.core.domain.model.Expense> = listOf(anExpense(chf(12, 50), today)),
        zeroSpendMarked: Boolean = false,
        pendingWeekId: String? = null,
    ) = BridgeState(
        week = Week(LocalDate.of(2025, 4, 14)),
        today = today,
        balanceEp = 340,
        provisionalEp = 58,
        weeklyBudget = chf(163, 33),
        weeklySpend = chf(72, 10),
        todaySpend = todayEntries.fold(Money.ZERO) { acc, expense -> acc + expense.amount },
        todayEntries = todayEntries,
        zeroSpendMarkedToday = zeroSpendMarked,
        streakBonusPercent = 15,
        consecutiveGoodWeeks = 3,
        signalDays = 4,
        modules = listOf(aModule(level = 2)),
        nextDeadline = null,
        pendingSettlementWeekId = pendingWeekId,
        hasAnyData = hasAnyData,
    )

    private fun setContent(
        state: BridgeUiState,
        onMarkZeroSpendDay: () -> Unit = {},
        onUndoZeroSpendDay: () -> Unit = {},
        onOpenSettlementSummary: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            Orbit7Theme {
                BridgeScreen(
                    state = state,
                    onOpenStation = {},
                    onOpenSettlementSummary = onOpenSettlementSummary,
                    onOpenDrains = {},
                    onQuickAdd = {},
                    onMarkZeroSpendDay = onMarkZeroSpendDay,
                    onUndoZeroSpendDay = onUndoZeroSpendDay,
                )
            }
        }
    }
}
