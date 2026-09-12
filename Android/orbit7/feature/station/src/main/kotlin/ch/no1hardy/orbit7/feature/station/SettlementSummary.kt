package ch.no1hardy.orbit7.feature.station

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.component.DigitRollText
import ch.no1hardy.orbit7.core.designsystem.component.Panel
import ch.no1hardy.orbit7.core.designsystem.component.ReadoutWindow
import ch.no1hardy.orbit7.core.designsystem.format.MoneyFormatter
import ch.no1hardy.orbit7.core.designsystem.moduleNameRes
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme
import ch.no1hardy.orbit7.core.domain.economy.StationCatalog
import ch.no1hardy.orbit7.core.domain.model.Category
import ch.no1hardy.orbit7.core.domain.model.StationModuleKey
import ch.no1hardy.orbit7.core.domain.model.WeeklySettlement
import ch.no1hardy.orbit7.core.domain.repository.CategoryRepository
import ch.no1hardy.orbit7.core.domain.repository.EnergyRepository
import ch.no1hardy.orbit7.core.domain.repository.SettingsRepository
import ch.no1hardy.orbit7.core.domain.repository.SettlementRepository
import ch.no1hardy.orbit7.core.domain.repository.StationRepository
import ch.no1hardy.orbit7.core.domain.time.weekOfId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import ch.no1hardy.orbit7.core.designsystem.R as CoreR

data class SettlementSummaryUiState(
    val settlement: WeeklySettlement? = null,
    val bestCategory: Category? = null,
    val worstCategory: Category? = null,
    val nowAffordable: StationModuleKey? = null,
    val loading: Boolean = true,
)

/**
 * The weekly payoff moment (`docs/03-screens.md` §11).
 *
 * Shown once, on the first open after a week has been settled, and re-readable from Reports
 * afterwards. Acknowledging it is what stops it from appearing again.
 */
@HiltViewModel
class SettlementSummaryViewModel
    @Inject
    constructor(
        private val settlements: SettlementRepository,
        private val categories: CategoryRepository,
        private val station: StationRepository,
        private val energy: EnergyRepository,
        private val settings: SettingsRepository,
        private val catalog: StationCatalog,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val weekId: String? = savedStateHandle[ARG_WEEK_ID]

        private val _state = MutableStateFlow(SettlementSummaryUiState())
        val state: StateFlow<SettlementSummaryUiState> = _state.asStateFlow()

        init {
            viewModelScope.launch {
                val settlement = weekId?.let { settlements.settlement(weekOfId(it)) } ?: settlements.latest()
                val states = station.modules().associateBy { it.moduleKey }
                val balance = energy.balance().toLong()

                _state.value =
                    SettlementSummaryUiState(
                        settlement = settlement,
                        bestCategory = settlement?.bestCategoryId?.let { categories.category(it) },
                        worstCategory = settlement?.worstCategoryId?.let { categories.category(it) },
                        nowAffordable =
                            catalog
                                .offers(states, balance)
                                .filter { it.canPurchase }
                                .minByOrNull { it.nextLevelCostEp ?: Long.MAX_VALUE }
                                ?.moduleKey,
                        loading = false,
                    )
            }
        }

        fun onAcknowledge() {
            val week = _state.value.settlement?.week ?: return
            viewModelScope.launch {
                settings.update { it.copy(lastAcknowledgedSettlementWeekId = week.id) }
            }
        }

        companion object {
            const val ARG_WEEK_ID = "weekId"
        }
    }

@Composable
fun SettlementSummaryRoute(
    onDismiss: () -> Unit,
    onOpenStation: () -> Unit,
    viewModel: SettlementSummaryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettlementSummaryScreen(
        state = state,
        onDismiss = {
            viewModel.onAcknowledge()
            onDismiss()
        },
        onOpenStation = {
            viewModel.onAcknowledge()
            onOpenStation()
        },
    )
}

/**
 * Power generated, the multipliers as they actually applied, the best and worst category, and the
 * streak — reported as telemetry, never as a verdict. A bad week is never scolded
 * (`docs/02-game-design.md` §8).
 */
@Composable
fun SettlementSummaryScreen(
    state: SettlementSummaryUiState,
    onDismiss: () -> Unit,
    onOpenStation: () -> Unit,
    formatter: MoneyFormatter = MoneyFormatter(),
) {
    val settlement = state.settlement
    Column(
        modifier =
            Modifier
                .testTag(TestTags.SETTLEMENT_SUMMARY)
                .fillMaxSize()
                .padding(Orbit7Theme.shapes.screenGutter),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (settlement == null) {
            Text(
                text = stringResource(R.string.settlement_none),
                style = Orbit7Theme.typography.body,
                color = Orbit7Theme.colors.onSurfaceMuted,
            )
            TextButton(onClick = onDismiss) { Text(stringResource(CoreR.string.action_dismiss)) }
            return@Column
        }

        Panel(caption = stringResource(R.string.settlement_title)) {
            ReadoutWindow {
                DigitRollText(
                    text = formatter.formatEnergy(settlement.awardedEp, withSign = true),
                    contentDescription =
                        stringResource(
                            R.string.settlement_generated_description,
                            settlement.awardedEp,
                        ),
                    style = Orbit7Theme.typography.readoutXL,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text =
                    stringResource(
                        R.string.settlement_breakdown,
                        settlement.baseEp,
                        settlement.confidencePercent,
                        settlement.streakBonusPercent,
                        settlement.ventedEp,
                    ),
                style = Orbit7Theme.typography.bodyS,
                color = Orbit7Theme.colors.onSurfaceMuted,
            )
        }

        Panel(caption = stringResource(R.string.settlement_week)) {
            Text(
                text =
                    stringResource(
                        R.string.settlement_budget_vs_spend,
                        formatter.format(settlement.budgeted),
                        formatter.format(settlement.spent),
                    ),
                style = Orbit7Theme.typography.body,
                color = Orbit7Theme.colors.onSurface,
            )
            Text(
                text = stringResource(R.string.settlement_signal_days, settlement.signalDays),
                style = Orbit7Theme.typography.bodyS,
                color = Orbit7Theme.colors.onSurfaceMuted,
            )
            Text(
                text =
                    if (settlement.goodWeek) {
                        stringResource(R.string.settlement_streak_advanced, settlement.streakBonusPercent)
                    } else {
                        // Neutral, never a failure message.
                        stringResource(R.string.settlement_streak_reset)
                    },
                style = Orbit7Theme.typography.body,
                color = if (settlement.goodWeek) Orbit7Theme.colors.energy else Orbit7Theme.colors.onSurfaceMuted,
            )
        }

        state.nowAffordable?.let { module ->
            Panel(caption = stringResource(R.string.settlement_now_affordable)) {
                Text(
                    text = stringResource(moduleNameRes(module)),
                    style = Orbit7Theme.typography.titleM,
                    color = Orbit7Theme.colors.energy,
                )
                Button(onClick = onOpenStation, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settlement_open_station))
                }
            }
        }

        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(CoreR.string.action_dismiss))
        }
    }
}
