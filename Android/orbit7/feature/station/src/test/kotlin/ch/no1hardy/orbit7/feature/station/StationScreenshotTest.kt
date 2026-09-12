package ch.no1hardy.orbit7.feature.station

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme
import ch.no1hardy.orbit7.core.domain.economy.StationCatalog
import ch.no1hardy.orbit7.core.domain.model.StationModuleKey
import ch.no1hardy.orbit7.core.domain.test.aModule
import ch.no1hardy.orbit7.core.domain.test.anExpense
import ch.no1hardy.orbit7.core.domain.test.chf
import ch.no1hardy.orbit7.core.domain.time.Week
import ch.no1hardy.orbit7.core.domain.usecase.BridgeState
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Screens rendered from fixed `UiState` fixtures, in the three configurations that catch the
 * regressions that actually happen (`docs/06-test-strategy.md` §7).
 */
class StationScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6.copy(locale = "de-CH"))

    private val today = LocalDate.of(2025, 4, 16)

    private val bridgeState =
        BridgeUiState.Ready(
            BridgeState(
                week = Week(LocalDate.of(2025, 4, 14)),
                today = today,
                balanceEp = 1_340,
                provisionalEp = 58,
                weeklyBudget = chf(163, 33),
                weeklySpend = chf(72, 10),
                todaySpend = chf(12, 50),
                todayEntries = listOf(anExpense(chf(12, 50), today)),
                zeroSpendMarkedToday = false,
                streakBonusPercent = 15,
                consecutiveGoodWeeks = 3,
                signalDays = 4,
                modules = listOf(aModule(level = 2), aModule(StationModuleKey.LIFE_SUPPORT, level = 1)),
                nextDeadline = null,
                pendingSettlementWeekId = null,
                hasAnyData = true,
            ),
            firstRun = false,
        )

    private val stationState =
        StationUiState(
            balanceEp = 1_340,
            offers =
                StationCatalog().offers(
                    mapOf(
                        StationModuleKey.REACTOR_CORE to aModule(level = 2),
                        StationModuleKey.LIFE_SUPPORT to aModule(StationModuleKey.LIFE_SUPPORT, level = 1),
                    ),
                    balanceEp = 1_340,
                ),
            levels = mapOf(StationModuleKey.REACTOR_CORE to 2, StationModuleKey.LIFE_SUPPORT to 1),
            loading = false,
        )

    @Test
    fun bridge() = snapshotAll("bridge") { BridgeScreenFixture() }

    @Test
    fun station() = snapshotAll("station") { StationScreenFixture() }

    @Test
    fun `bridge on first run`() {
        paparazzi.snapshot(name = "bridge_first_run") {
            Orbit7Theme {
                BridgeScreen(
                    state =
                        BridgeUiState.Ready(
                            bridgeState.bridge.copy(hasAnyData = false, balanceEp = 0, todayEntries = emptyList()),
                            firstRun = true,
                        ),
                    onOpenStation = {},
                    onOpenSettlementSummary = {},
                    onOpenDrains = {},
                    onQuickAdd = {},
                    onMarkZeroSpendDay = {},
                    onUndoZeroSpendDay = {},
                )
            }
        }
    }

    @Test
    fun `bridge when the week is overdrawn`() {
        paparazzi.snapshot(name = "bridge_overdrawn") {
            Orbit7Theme {
                BridgeScreen(
                    state =
                        BridgeUiState.Ready(
                            bridgeState.bridge.copy(weeklySpend = chf(210), provisionalEp = 0),
                            firstRun = false,
                        ),
                    onOpenStation = {},
                    onOpenSettlementSummary = {},
                    onOpenDrains = {},
                    onQuickAdd = {},
                    onMarkZeroSpendDay = {},
                    onUndoZeroSpendDay = {},
                )
            }
        }
    }

    @Test
    fun `settlement summary`() {
        paparazzi.snapshot(name = "settlement_summary") {
            Orbit7Theme {
                SettlementSummaryScreen(
                    state =
                        SettlementSummaryUiState(
                            settlement =
                                ch.no1hardy.orbit7.core.domain.model.WeeklySettlement(
                                    week = Week(LocalDate.of(2025, 4, 7)),
                                    baseEp = 26,
                                    ventedEp = 7,
                                    confidenceBasisPoints = 9_143,
                                    streakBasisPoints = 11_500,
                                    awardedEp = 20,
                                    signalDays = 6,
                                    goodWeek = true,
                                    budgeted = chf(163, 33),
                                    spent = chf(151, 60),
                                    settledAt = java.time.Instant.parse("2025-04-14T03:00:00Z"),
                                    bestCategoryId = 1,
                                    worstCategoryId = 2,
                                ),
                            bestCategory = null,
                            worstCategory = null,
                            nowAffordable = StationModuleKey.REACTOR_CORE,
                            loading = false,
                        ),
                    onDismiss = {},
                    onOpenStation = {},
                )
            }
        }
    }

    /** The reference look, the largest font scale, and every effect disabled. */
    private fun snapshotAll(
        name: String,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        paparazzi.snapshot(name = "${name}_default") { Orbit7Theme(reduceEffects = false) { content() } }

        paparazzi.unsafeUpdateConfig(
            deviceConfig = DeviceConfig.PIXEL_6.copy(locale = "de-CH", fontScale = 2.0f),
        )
        paparazzi.snapshot(name = "${name}_font_scale_200") { Orbit7Theme(reduceEffects = false) { content() } }

        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_6.copy(locale = "de-CH"))
        paparazzi.snapshot(name = "${name}_reduce_effects") { Orbit7Theme(reduceEffects = true) { content() } }
    }

    @androidx.compose.runtime.Composable
    private fun BridgeScreenFixture() =
        BridgeScreen(
            state = bridgeState,
            onOpenStation = {},
            onOpenSettlementSummary = {},
            onOpenDrains = {},
            onQuickAdd = {},
            onMarkZeroSpendDay = {},
            onUndoZeroSpendDay = {},
        )

    @androidx.compose.runtime.Composable
    private fun StationScreenFixture() =
        StationScreen(
            state = stationState,
            onModuleSelected = {},
            onPowerUp = {},
        )
}
