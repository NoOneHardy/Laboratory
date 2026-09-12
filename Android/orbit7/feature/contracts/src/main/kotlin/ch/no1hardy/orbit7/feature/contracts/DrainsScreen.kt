package ch.no1hardy.orbit7.feature.contracts

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.component.HazardBanner
import ch.no1hardy.orbit7.core.designsystem.component.Panel
import ch.no1hardy.orbit7.core.designsystem.format.MoneyFormatter
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.usecase.Drain
import ch.no1hardy.orbit7.core.designsystem.R as CoreR

@Composable
fun DrainsRoute(
    onEditContract: (Long) -> Unit,
    onAddContract: () -> Unit,
    viewModel: DrainsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DrainsScreen(
        state = state,
        onEditContract = onEditContract,
        onAddContract = onAddContract,
        onCancelRequested = viewModel::onCancelRequested,
        onCancelConfirmed = viewModel::onCancelConfirmed,
        onCancelDismissed = viewModel::onCancelDismissed,
        onRenegotiateRequested = viewModel::onRenegotiateRequested,
        onRenegotiateConfirmed = viewModel::onRenegotiateConfirmed,
        onRenegotiateDismissed = viewModel::onRenegotiateDismissed,
    )
}

/**
 * The drains view (`docs/03-screens.md` §6): contracts as taps on the power bus, sorted by bleed
 * rate descending — the biggest target first, because that is where the real money is.
 */
@Composable
@Suppress("LongParameterList")
fun DrainsScreen(
    state: DrainsUiState,
    onEditContract: (Long) -> Unit,
    onAddContract: () -> Unit,
    onCancelRequested: (Long) -> Unit,
    onCancelConfirmed: (Long) -> Unit,
    onCancelDismissed: () -> Unit,
    onRenegotiateRequested: (Long) -> Unit,
    onRenegotiateConfirmed: (Long, Money) -> Unit,
    onRenegotiateDismissed: () -> Unit,
    formatter: MoneyFormatter = MoneyFormatter(),
) {
    Column(
        modifier =
            Modifier
                .testTag(TestTags.DRAINS)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Orbit7Theme.shapes.screenGutter),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Panel(caption = stringResource(R.string.drains_title), testTag = TestTags.DRAINS_HEADER) {
            Text(
                text =
                    stringResource(
                        R.string.drains_header,
                        state.drains.totalMonthlyBleedEp,
                        state.drains.active.size,
                    ),
                style = Orbit7Theme.typography.titleM,
                color = Orbit7Theme.colors.onSurface,
            )
            Text(
                text =
                    stringResource(
                        R.string.drains_header_money,
                        formatter.format(state.drains.totalMonthlyBleed),
                    ),
                style = Orbit7Theme.typography.bodyS,
                color = Orbit7Theme.colors.onSurfaceMuted,
            )
        }

        if (state.isEmpty) {
            Panel(caption = stringResource(R.string.drains_empty_caption), testTag = TestTags.EMPTY_STATE) {
                Text(
                    text = stringResource(R.string.drains_empty_body),
                    style = Orbit7Theme.typography.body,
                    color = Orbit7Theme.colors.onSurfaceMuted,
                )
                TextButton(onClick = onAddContract) {
                    Text(stringResource(R.string.drains_add))
                }
            }
        }

        state.drains.active.forEach { drain ->
            DrainRow(
                drain = drain,
                formatter = formatter,
                confirming = state.confirmingCancelId == drain.contract.id,
                renegotiating = state.renegotiatingId == drain.contract.id,
                onEdit = { onEditContract(drain.contract.id) },
                onCancelRequested = { onCancelRequested(drain.contract.id) },
                onCancelConfirmed = { onCancelConfirmed(drain.contract.id) },
                onCancelDismissed = onCancelDismissed,
                onRenegotiateRequested = { onRenegotiateRequested(drain.contract.id) },
                onRenegotiateConfirmed = { amount -> onRenegotiateConfirmed(drain.contract.id, amount) },
                onRenegotiateDismissed = onRenegotiateDismissed,
            )
        }

        if (state.drains.salvaged.isNotEmpty()) {
            Panel(
                caption = stringResource(R.string.drains_salvaged),
                testTag = TestTags.DRAINS_SALVAGED_SECTION,
            ) {
                Text(
                    text =
                        stringResource(
                            R.string.drains_salvaged_total,
                            formatter.format(state.drains.monthlySavingsAchieved),
                        ),
                    style = Orbit7Theme.typography.readoutM,
                    color = Orbit7Theme.colors.energy,
                    softWrap = false,
                )
                state.drains.salvaged.forEach { drain ->
                    Text(
                        text =
                            stringResource(
                                R.string.drains_salvaged_row,
                                drain.contract.name,
                                formatter.format(drain.contract.monthlySaving),
                            ),
                        style = Orbit7Theme.typography.bodyS,
                        color = Orbit7Theme.colors.onSurfaceMuted,
                    )
                }
            }
        }

        TextButton(onClick = onAddContract, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.drains_add))
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun DrainRow(
    drain: Drain,
    formatter: MoneyFormatter,
    confirming: Boolean,
    renegotiating: Boolean,
    onEdit: () -> Unit,
    onCancelRequested: () -> Unit,
    onCancelConfirmed: () -> Unit,
    onCancelDismissed: () -> Unit,
    onRenegotiateRequested: () -> Unit,
    onRenegotiateConfirmed: (Money) -> Unit,
    onRenegotiateDismissed: () -> Unit,
) {
    Panel(caption = drain.contract.name, testTag = TestTags.drainRow(drain.contract.id)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text =
                    stringResource(
                        R.string.drains_bleed,
                        formatter.format(drain.monthlyBleed),
                        drain.monthlyBleedEp,
                    ),
                style = Orbit7Theme.typography.readoutM,
                color = Orbit7Theme.colors.onSurface,
                softWrap = false,
            )
        }

        // Inside 14 days the row is allowed to shout, and says so in words as well as in stripes.
        if (drain.urgent) {
            HazardBanner(
                title = stringResource(R.string.drains_deadline_urgent, drain.daysUntilDeadline),
                detail = stringResource(R.string.drains_potential, drain.potentialSalvageEp),
                testTag = "drain_hazard_${drain.contract.id}",
            )
        } else {
            Text(
                text = stringResource(R.string.drains_deadline, drain.daysUntilDeadline),
                style = Orbit7Theme.typography.bodyS,
                color = Orbit7Theme.colors.onSurfaceMuted,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onEdit) { Text(stringResource(CoreR.string.action_edit)) }
            TextButton(
                onClick = onCancelRequested,
                modifier = Modifier.testTag(TestTags.CONTRACT_CANCEL),
            ) {
                Text(stringResource(R.string.drains_cancel))
            }
            TextButton(
                onClick = onRenegotiateRequested,
                modifier = Modifier.testTag(TestTags.CONTRACT_RENEGOTIATE),
            ) {
                Text(stringResource(R.string.drains_renegotiate))
            }
        }

        if (confirming) {
            HazardBanner(
                title = stringResource(R.string.drains_cancel_confirm_title),
                detail = stringResource(R.string.drains_cancel_confirm_body, drain.potentialSalvageEp),
                testTag = "drain_confirm_${drain.contract.id}",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCancelConfirmed, modifier = Modifier.testTag("drain_confirm_yes")) {
                    Text(stringResource(R.string.drains_cancel_confirm_action))
                }
                TextButton(onClick = onCancelDismissed) { Text(stringResource(CoreR.string.action_cancel)) }
            }
        }

        if (renegotiating) {
            RenegotiateForm(
                current = drain.contract.amount,
                onConfirm = onRenegotiateConfirmed,
                onDismiss = onRenegotiateDismissed,
                formatter = formatter,
            )
        }
    }
}

@Composable
private fun RenegotiateForm(
    current: Money,
    onConfirm: (Money) -> Unit,
    onDismiss: () -> Unit,
    formatter: MoneyFormatter,
) {
    var amountText by androidx.compose.runtime.saveable.rememberSaveable {
        androidx.compose.runtime.mutableStateOf((current.minor / 100).toString())
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.drains_renegotiate_body, formatter.format(current)),
            style = Orbit7Theme.typography.body,
            color = Orbit7Theme.colors.onSurface,
        )
        androidx.compose.foundation.text.BasicTextField(
            value = amountText,
            onValueChange = { input -> amountText = input.filter { it.isDigit() } },
            textStyle = Orbit7Theme.typography.readoutM.copy(color = Orbit7Theme.colors.onSurface),
            keyboardOptions =
                androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                ),
            modifier =
                Modifier
                    .testTag("drain_renegotiate_amount")
                    .fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = { onConfirm(Money((amountText.toLongOrNull() ?: 0L) * 100)) },
                modifier = Modifier.testTag("drain_renegotiate_confirm"),
            ) {
                Text(stringResource(CoreR.string.action_save))
            }
            TextButton(onClick = onDismiss) { Text(stringResource(CoreR.string.action_cancel)) }
        }
    }
}
