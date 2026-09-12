package ch.no1hardy.orbit7.feature.station

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.component.BusLine
import ch.no1hardy.orbit7.core.designsystem.component.DigitRollText
import ch.no1hardy.orbit7.core.designsystem.component.Gauge
import ch.no1hardy.orbit7.core.designsystem.component.HazardBanner
import ch.no1hardy.orbit7.core.designsystem.component.Panel
import ch.no1hardy.orbit7.core.designsystem.component.ReadoutWindow
import ch.no1hardy.orbit7.core.designsystem.component.StationView
import ch.no1hardy.orbit7.core.designsystem.format.MoneyFormatter
import ch.no1hardy.orbit7.core.designsystem.moduleNameRes
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme
import ch.no1hardy.orbit7.core.domain.usecase.BridgeState
import java.time.temporal.ChronoUnit
import ch.no1hardy.orbit7.core.designsystem.R as CoreR

@Composable
fun BridgeRoute(
    onOpenStation: () -> Unit,
    onOpenSettlementSummary: (String) -> Unit,
    onOpenDrains: () -> Unit,
    onQuickAdd: () -> Unit,
    viewModel: BridgeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BridgeScreen(
        state = state,
        onOpenStation = onOpenStation,
        onOpenSettlementSummary = onOpenSettlementSummary,
        onOpenDrains = onOpenDrains,
        onQuickAdd = onQuickAdd,
        onMarkZeroSpendDay = viewModel::onMarkZeroSpendDay,
        onUndoZeroSpendDay = viewModel::onUndoZeroSpendDay,
    )
}

/**
 * The Bridge (`docs/03-screens.md` §1) — the only screen the user is guaranteed to see every day.
 *
 * Top to bottom: the station viewport, the power readout with this week's *provisional* EP, the
 * week gauge, the today card with its one-tap zero-spend mark, the streak chip and the nearest
 * contract deadline.
 */
@Composable
fun BridgeScreen(
    state: BridgeUiState,
    onOpenStation: () -> Unit,
    onOpenSettlementSummary: (String) -> Unit,
    onOpenDrains: () -> Unit,
    onQuickAdd: () -> Unit,
    onMarkZeroSpendDay: () -> Unit,
    onUndoZeroSpendDay: () -> Unit,
    formatter: MoneyFormatter = MoneyFormatter(),
) {
    Column(
        modifier =
            Modifier
                .testTag(TestTags.BRIDGE)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Orbit7Theme.shapes.screenGutter),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (state) {
            BridgeUiState.Loading ->
                Panel(caption = stringResource(R.string.bridge_booting)) {
                    Text(
                        text = stringResource(R.string.bridge_booting_body),
                        style = Orbit7Theme.typography.body,
                        color = Orbit7Theme.colors.onSurfaceMuted,
                    )
                }

            is BridgeUiState.Ready ->
                BridgeContent(
                    state = state,
                    onOpenStation = onOpenStation,
                    onOpenSettlementSummary = onOpenSettlementSummary,
                    onOpenDrains = onOpenDrains,
                    onQuickAdd = onQuickAdd,
                    onMarkZeroSpendDay = onMarkZeroSpendDay,
                    onUndoZeroSpendDay = onUndoZeroSpendDay,
                    formatter = formatter,
                )
        }
    }
}

@Composable
private fun BridgeContent(
    state: BridgeUiState.Ready,
    onOpenStation: () -> Unit,
    onOpenSettlementSummary: (String) -> Unit,
    onOpenDrains: () -> Unit,
    onQuickAdd: () -> Unit,
    onMarkZeroSpendDay: () -> Unit,
    onUndoZeroSpendDay: () -> Unit,
    formatter: MoneyFormatter,
) {
    val bridge = state.bridge

    state.bridge.pendingSettlementWeekId?.let { weekId ->
        Panel(
            caption = stringResource(R.string.bridge_settlement_ready),
            testTag = TestTags.SETTLEMENT_BANNER,
        ) {
            TextButton(onClick = { onOpenSettlementSummary(weekId) }) {
                Text(stringResource(R.string.bridge_settlement_open))
            }
        }
    }

    StationView(
        levels = bridge.stationLevels,
        contentDescription = stationDescription(bridge),
        modifier = Modifier.clickable(onClick = onOpenStation),
    )

    BusLine()

    Panel(caption = stringResource(R.string.bridge_power), testTag = TestTags.POWER_READOUT) {
        ReadoutWindow {
            DigitRollText(
                text = formatter.formatEnergy(bridge.balanceEp),
                contentDescription =
                    stringResource(
                        R.string.bridge_power_description,
                        bridge.balanceEp,
                    ),
                style = Orbit7Theme.typography.readoutXL,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // Provisional, and labelled as such: nothing is banked until the week is settled.
        Text(
            text = stringResource(R.string.bridge_provisional, bridge.provisionalEp),
            style = Orbit7Theme.typography.bodyS,
            color = Orbit7Theme.colors.onSurfaceMuted,
            modifier = Modifier.testTag(TestTags.PROVISIONAL_EP),
        )
    }

    if (state.firstRun) {
        Panel(caption = stringResource(R.string.bridge_first_run_caption), testTag = TestTags.EMPTY_STATE) {
            Text(
                text = stringResource(R.string.bridge_first_run_body),
                style = Orbit7Theme.typography.body,
                color = Orbit7Theme.colors.onSurface,
            )
            TextButton(onClick = onQuickAdd, modifier = Modifier.testTag(TestTags.FAB_QUICK_ADD)) {
                Text(stringResource(R.string.bridge_first_run_action))
            }
        }
        return
    }

    WeekGauge(bridge = bridge, formatter = formatter)
    TodayCard(
        bridge = bridge,
        formatter = formatter,
        onMarkZeroSpendDay = onMarkZeroSpendDay,
        onUndoZeroSpendDay = onUndoZeroSpendDay,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text =
                stringResource(
                    R.string.bridge_streak,
                    bridge.consecutiveGoodWeeks,
                    bridge.streakBonusPercent,
                ),
            style = Orbit7Theme.typography.label,
            color = Orbit7Theme.colors.energy,
            modifier = Modifier.testTag(TestTags.STREAK_CHIP),
        )
    }

    bridge.nextDeadline?.let { deadline ->
        val name = deadline.contract.name
        if (deadline.urgent) {
            HazardBanner(
                title = stringResource(R.string.bridge_deadline_urgent, deadline.daysRemaining),
                detail = name,
                testTag = TestTags.NEXT_DEADLINE,
                modifier = Modifier.clickable(onClick = onOpenDrains),
            )
        } else {
            Panel(caption = stringResource(R.string.bridge_deadline), testTag = TestTags.NEXT_DEADLINE) {
                Text(
                    text = stringResource(R.string.bridge_deadline_body, name, deadline.daysRemaining),
                    style = Orbit7Theme.typography.body,
                    color = Orbit7Theme.colors.onSurface,
                    modifier = Modifier.clickable(onClick = onOpenDrains),
                )
            }
        }
    }
}

@Composable
private fun WeekGauge(
    bridge: BridgeState,
    formatter: MoneyFormatter,
) {
    val remaining = bridge.remainingBudget
    val progress =
        if (bridge.weeklyBudget.minor <= 0) {
            0f
        } else {
            (remaining.minor.toFloat() / bridge.weeklyBudget.minor.toFloat()).coerceIn(0f, 1f)
        }
    val dayIndex = ChronoUnit.DAYS.between(bridge.week.start, bridge.today).toFloat()

    Panel(caption = stringResource(R.string.bridge_week_headroom), testTag = TestTags.WEEK_GAUGE) {
        Gauge(
            progress = progress,
            contentDescription =
                if (bridge.isOverdrawn) {
                    stringResource(R.string.bridge_gauge_overdrawn, formatter.format(-remaining))
                } else {
                    stringResource(R.string.bridge_gauge_remaining, formatter.format(remaining))
                },
            dayMarker = (dayIndex + 1f) / DAYS_IN_WEEK,
            overdrawn = bridge.isOverdrawn,
        )
        Text(
            text =
                if (bridge.isOverdrawn) {
                    stringResource(R.string.bridge_overdrawn_by, formatter.format(-remaining))
                } else {
                    stringResource(R.string.bridge_remaining, formatter.format(remaining))
                },
            style = Orbit7Theme.typography.readoutM,
            color = if (bridge.isOverdrawn) Orbit7Theme.colors.danger else Orbit7Theme.colors.onSurface,
            softWrap = false,
        )
    }
}

@Composable
private fun TodayCard(
    bridge: BridgeState,
    formatter: MoneyFormatter,
    onMarkZeroSpendDay: () -> Unit,
    onUndoZeroSpendDay: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    Panel(
        caption = formatter.formatShortDate(bridge.today, locale),
        testTag = TestTags.TODAY_CARD,
    ) {
        Text(
            text = formatter.format(bridge.todaySpend),
            style = Orbit7Theme.typography.readoutL,
            color = Orbit7Theme.colors.onSurface,
            softWrap = false,
        )
        bridge.todayEntries.forEach { entry ->
            Text(
                text = formatter.format(entry.amount),
                style = Orbit7Theme.typography.bodyS,
                color = Orbit7Theme.colors.onSurfaceMuted,
            )
        }

        if (bridge.zeroSpendMarkedToday) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.bridge_zero_spend_confirmed),
                    style = Orbit7Theme.typography.body,
                    color = Orbit7Theme.colors.energy,
                )
                TextButton(
                    onClick = onUndoZeroSpendDay,
                    modifier = Modifier.testTag(TestTags.ZERO_SPEND_UNDO),
                ) {
                    Text(stringResource(CoreR.string.action_undo))
                }
            }
        } else if (bridge.todayEntries.isEmpty()) {
            TextButton(
                onClick = onMarkZeroSpendDay,
                modifier = Modifier.testTag(TestTags.ZERO_SPEND_MARK),
            ) {
                Text(stringResource(R.string.bridge_zero_spend_mark))
            }
        }
    }
}

/** Values, not pictures: what a screen reader is told about the station graphic. */
@Composable
private fun stationDescription(bridge: BridgeState): String {
    val lit = bridge.modules.filter { it.level > 0 }
    if (lit.isEmpty()) return stringResource(R.string.station_description_dark)
    val parts =
        lit.joinToString(", ") { module ->
            stringResource(
                R.string.station_description_module,
                stringResource(moduleNameRes(module.moduleKey)),
                module.level,
                module.moduleKey.maxLevel,
            )
        }
    return stringResource(R.string.station_description, parts)
}

private const val DAYS_IN_WEEK = 7f
