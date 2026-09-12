package ch.no1hardy.orbit7.feature.station

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.component.DigitRollText
import ch.no1hardy.orbit7.core.designsystem.component.ModuleTile
import ch.no1hardy.orbit7.core.designsystem.component.Panel
import ch.no1hardy.orbit7.core.designsystem.component.ReadoutWindow
import ch.no1hardy.orbit7.core.designsystem.component.StationView
import ch.no1hardy.orbit7.core.designsystem.format.MoneyFormatter
import ch.no1hardy.orbit7.core.designsystem.moduleDescriptionRes
import ch.no1hardy.orbit7.core.designsystem.moduleNameRes
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme
import ch.no1hardy.orbit7.core.domain.economy.ModuleOffer
import ch.no1hardy.orbit7.core.domain.model.StationModuleKey
import ch.no1hardy.orbit7.core.designsystem.R as CoreR

@Composable
fun StationRoute(viewModel: StationViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    StationScreen(
        state = state,
        onModuleSelected = viewModel::onModuleSelected,
        onPowerUp = viewModel::onPowerUp,
    )
}

/**
 * The Station (`docs/03-screens.md` §5): the graphic, the balance, and the modules with their
 * affordability states. Powering one up is the app's second payoff moment.
 */
@Composable
fun StationScreen(
    state: StationUiState,
    onModuleSelected: (StationModuleKey?) -> Unit,
    onPowerUp: (StationModuleKey) -> Unit,
    formatter: MoneyFormatter = MoneyFormatter(),
) {
    Column(
        modifier =
            Modifier
                .testTag(TestTags.STATION)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Orbit7Theme.shapes.screenGutter),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StationView(
            levels = state.levels,
            contentDescription = stringResource(R.string.station_title),
        )

        Panel(caption = stringResource(R.string.bridge_power)) {
            ReadoutWindow {
                DigitRollText(
                    text = formatter.formatEnergy(state.balanceEp),
                    contentDescription = stringResource(R.string.bridge_power_description, state.balanceEp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Nothing affordable: the nearest goal is highlighted rather than a wall of disabled tiles.
        if (!state.hasAnythingAffordable) {
            state.nearestGoal?.let { goal ->
                Panel(caption = stringResource(R.string.station_next_goal), testTag = TestTags.NEAREST_GOAL) {
                    Text(
                        text =
                            stringResource(
                                R.string.station_ep_to_go,
                                stringResource(moduleNameRes(goal.moduleKey)),
                                goal.epShortfall,
                            ),
                        style = Orbit7Theme.typography.body,
                        color = Orbit7Theme.colors.onSurface,
                    )
                }
            }
        }

        state.offers.forEach { offer ->
            ModuleTile(
                name = stringResource(moduleNameRes(offer.moduleKey)),
                levelLabel = stringResource(R.string.station_level_short, offer.level),
                statusLabel = statusLabel(offer, formatter),
                contentDescription = moduleDescription(offer),
                affordable = offer.canPurchase,
                locked = !offer.prerequisiteMet,
                lit = offer.level > 0,
                onClick = { onModuleSelected(offer.moduleKey) },
                testTag = TestTags.moduleTile(offer.moduleKey.name),
            )
        }

        state.selectedOffer?.let { offer ->
            ModuleDetail(
                offer = offer,
                purchasing = state.purchaseInProgress == offer.moduleKey,
                onPowerUp = { onPowerUp(offer.moduleKey) },
                onDismiss = { onModuleSelected(null) },
                formatter = formatter,
            )
        }
    }
}

@Composable
private fun ModuleDetail(
    offer: ModuleOffer,
    purchasing: Boolean,
    onPowerUp: () -> Unit,
    onDismiss: () -> Unit,
    formatter: MoneyFormatter,
) {
    Panel(
        caption = stringResource(moduleNameRes(offer.moduleKey)),
        testTag = TestTags.MODULE_DETAIL,
    ) {
        Text(
            text = stringResource(moduleDescriptionRes(offer.moduleKey)),
            style = Orbit7Theme.typography.body,
            color = Orbit7Theme.colors.onSurfaceMuted,
        )
        Text(
            text = statusLabel(offer, formatter),
            style = Orbit7Theme.typography.readoutM,
            color = Orbit7Theme.colors.onSurface,
            softWrap = false,
        )
        // Disabled buttons say why they are disabled; a dead control that explains nothing is a bug.
        if (!offer.prerequisiteMet) {
            Text(
                text =
                    stringResource(
                        R.string.station_requires,
                        stringResource(moduleNameRes(StationModuleKey.REACTOR_CORE)),
                    ),
                style = Orbit7Theme.typography.bodyS,
                color = Orbit7Theme.colors.warning,
            )
        } else if (!offer.isMaxed && !offer.affordable) {
            Text(
                text = stringResource(R.string.station_shortfall, offer.epShortfall),
                style = Orbit7Theme.typography.bodyS,
                color = Orbit7Theme.colors.warning,
            )
        }
        Button(
            onClick = onPowerUp,
            enabled = offer.canPurchase && !purchasing,
            modifier =
                Modifier
                    .testTag(TestTags.POWER_UP)
                    .fillMaxWidth(),
        ) {
            Text(stringResource(R.string.station_power_up))
        }
        TextButton(onClick = onDismiss) {
            Text(stringResource(CoreR.string.action_dismiss))
        }
    }
}

@Composable
private fun statusLabel(
    offer: ModuleOffer,
    formatter: MoneyFormatter,
): String =
    when {
        offer.isMaxed -> stringResource(R.string.station_maxed)
        !offer.prerequisiteMet ->
            stringResource(
                R.string.station_locked,
                stringResource(moduleNameRes(StationModuleKey.REACTOR_CORE)),
            )
        else -> formatter.formatEnergy(offer.nextLevelCostEp?.toInt() ?: 0)
    }

@Composable
private fun moduleDescription(offer: ModuleOffer): String =
    stringResource(
        R.string.station_module_description,
        stringResource(moduleNameRes(offer.moduleKey)),
        offer.level,
        offer.moduleKey.maxLevel,
        offer.nextLevelCostEp ?: 0,
    )
