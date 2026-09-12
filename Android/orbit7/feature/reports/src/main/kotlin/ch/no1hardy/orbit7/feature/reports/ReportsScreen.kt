package ch.no1hardy.orbit7.feature.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.categoryNameRes
import ch.no1hardy.orbit7.core.designsystem.component.Panel
import ch.no1hardy.orbit7.core.designsystem.format.MoneyFormatter
import ch.no1hardy.orbit7.core.designsystem.moduleNameRes
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme
import ch.no1hardy.orbit7.core.domain.model.StationModuleKey
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.usecase.CategoryTrend
import ch.no1hardy.orbit7.core.domain.usecase.ObserveReportsUseCase
import ch.no1hardy.orbit7.core.domain.usecase.ReportsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface ReportsUiState {
    data object Loading : ReportsUiState

    /** Gated behind the Observation Deck: the screen explains how to unlock rather than hiding. */
    data object Locked : ReportsUiState

    data class Ready(
        val reports: ReportsState,
    ) : ReportsUiState
}

@HiltViewModel
class ReportsViewModel
    @Inject
    constructor(
        observeReports: ObserveReportsUseCase,
    ) : ViewModel() {
        val state: StateFlow<ReportsUiState> =
            observeReports()
                .map { reports -> if (reports.unlocked) ReportsUiState.Ready(reports) else ReportsUiState.Locked }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), ReportsUiState.Loading)

        private companion object {
            const val STOP_TIMEOUT = 5_000L
        }
    }

@Composable
fun ReportsRoute(viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReportsScreen(state = state)
}

/**
 * Reports (`docs/03-screens.md` §8), including the success-criteria panel that answers the three
 * questions from `docs/01-concept.md` from the user's own data, with no telemetry.
 */
@Composable
fun ReportsScreen(
    state: ReportsUiState,
    formatter: MoneyFormatter = MoneyFormatter(),
) {
    Column(
        modifier =
            Modifier
                .testTag(TestTags.REPORTS)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Orbit7Theme.shapes.screenGutter),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (state) {
            ReportsUiState.Loading ->
                Text(
                    text = stringResource(R.string.reports_loading),
                    style = Orbit7Theme.typography.body,
                    color = Orbit7Theme.colors.onSurfaceMuted,
                )

            ReportsUiState.Locked ->
                Panel(
                    caption = stringResource(R.string.reports_locked_caption),
                    testTag = TestTags.REPORTS_LOCKED,
                ) {
                    Text(
                        text =
                            stringResource(
                                R.string.reports_locked_body,
                                stringResource(moduleNameRes(StationModuleKey.OBSERVATION_DECK)),
                            ),
                        style = Orbit7Theme.typography.body,
                        color = Orbit7Theme.colors.onSurfaceMuted,
                    )
                }

            is ReportsUiState.Ready -> ReadyReports(state.reports, formatter)
        }
    }
}

@Composable
private fun ReadyReports(
    reports: ReportsState,
    formatter: MoneyFormatter,
) {
    val locale = LocalConfiguration.current.locales[0]

    Panel(caption = stringResource(R.string.reports_lifetime)) {
        Text(
            text = formatter.formatEnergy(reports.lifetimeEp),
            style = Orbit7Theme.typography.readoutL,
            color = Orbit7Theme.colors.accent,
            softWrap = false,
        )
        Text(
            text = stringResource(R.string.reports_streaks, reports.currentStreak, reports.bestStreak),
            style = Orbit7Theme.typography.bodyS,
            color = Orbit7Theme.colors.onSurfaceMuted,
        )
    }

    // The three success criteria from docs/01-concept.md, measured inside the app.
    Panel(caption = stringResource(R.string.reports_success), testTag = TestTags.SUCCESS_CRITERIA) {
        Text(
            text = stringResource(R.string.reports_contracts_salvaged, reports.successCriteria.contractsSalvaged),
            style = Orbit7Theme.typography.body,
            color = Orbit7Theme.colors.onSurface,
        )
        Text(
            text =
                stringResource(
                    R.string.reports_fixed_cost,
                    formatter.format(reports.successCriteria.monthlyFixedCostAtStart),
                    formatter.format(reports.successCriteria.monthlyFixedCostNow),
                ),
            style = Orbit7Theme.typography.body,
            color = Orbit7Theme.colors.onSurface,
        )
        Text(
            text =
                stringResource(
                    R.string.reports_baselines_down,
                    reports.successCriteria.categoriesWithLowerBaseline,
                ),
            style = Orbit7Theme.typography.body,
            color = Orbit7Theme.colors.onSurface,
        )
        Text(
            text = stringResource(R.string.reports_weeks_settled, reports.successCriteria.weeksSettled),
            style = Orbit7Theme.typography.bodyS,
            color = Orbit7Theme.colors.onSurfaceMuted,
        )
    }

    Panel(caption = stringResource(R.string.reports_history), testTag = TestTags.SETTLEMENT_HISTORY) {
        reports.settlements.forEach { settlement ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatter.formatShortDate(settlement.week.start, locale),
                    style = Orbit7Theme.typography.bodyS,
                    color = Orbit7Theme.colors.onSurfaceMuted,
                )
                Text(
                    text =
                        stringResource(
                            R.string.reports_history_row,
                            settlement.awardedEp,
                            settlement.confidencePercent,
                            settlement.streakBonusPercent,
                        ),
                    style = Orbit7Theme.typography.readoutM,
                    color = if (settlement.goodWeek) Orbit7Theme.colors.energy else Orbit7Theme.colors.onSurfaceMuted,
                    softWrap = false,
                )
            }
        }
    }

    reports.trends.forEach { trend -> TrendPanel(trend = trend, formatter = formatter) }
}

@Composable
private fun TrendPanel(
    trend: CategoryTrend,
    formatter: MoneyFormatter,
) {
    val name =
        trend.category.customName
            ?: trend.category.key?.let { stringResource(categoryNameRes(it)) }
            ?: ""

    Panel(caption = name) {
        TrendChart(values = trend.weeklySpend, baseline = trend.baseline)
        trend.baseline?.let {
            Text(
                text = stringResource(R.string.reports_baseline, formatter.format(it)),
                style = Orbit7Theme.typography.bodyS,
                color = Orbit7Theme.colors.onSurfaceMuted,
            )
        }
    }
}

@Composable
private fun TrendChart(
    values: List<Money>,
    baseline: Money?,
) {
    if (values.isEmpty()) return
    val colors = Orbit7Theme.colors
    val maximum = maxOf(values.maxOf { it.minor }, baseline?.minor ?: 0L).coerceAtLeast(1L)

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT),
    ) {
        val step = size.width / values.size.coerceAtLeast(1)
        values.forEachIndexed { index, value ->
            val height = size.height * (value.minor.toFloat() / maximum.toFloat())
            drawRect(
                color = colors.accent,
                topLeft = Offset(index * step, size.height - height),
                size =
                    androidx.compose.ui.geometry
                        .Size(step * BAR_RATIO, height),
            )
        }
        // The baseline ghost, on every chart that has one.
        baseline?.let {
            val y = size.height - size.height * (it.minor.toFloat() / maximum.toFloat())
            drawLine(
                color = colors.onSurfaceMuted,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2f,
            )
        }
    }
}

private val CHART_HEIGHT = 72.dp
private const val BAR_RATIO = 0.6f
