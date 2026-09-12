package ch.no1hardy.orbit7.feature.contracts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.categoryNameRes
import ch.no1hardy.orbit7.core.designsystem.component.CategoryChip
import ch.no1hardy.orbit7.core.designsystem.component.Panel
import ch.no1hardy.orbit7.core.designsystem.format.MoneyFormatter
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme
import ch.no1hardy.orbit7.core.domain.economy.ContractSchedule
import ch.no1hardy.orbit7.core.domain.model.Cadence
import ch.no1hardy.orbit7.core.domain.model.Category
import ch.no1hardy.orbit7.core.domain.model.Contract
import ch.no1hardy.orbit7.core.domain.model.NoticePeriod
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.repository.CategoryRepository
import ch.no1hardy.orbit7.core.domain.repository.ContractRepository
import ch.no1hardy.orbit7.core.domain.usecase.SaveContractUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import ch.no1hardy.orbit7.core.designsystem.R as CoreR

data class ContractEditorUiState(
    val id: Long = 0,
    val name: String = "",
    val amountMinor: Long = 0,
    val cadence: Cadence = Cadence.MONTHLY,
    val categoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val nextChargeOn: LocalDate = LocalDate.EPOCH,
    val noticePeriod: NoticePeriod = NoticePeriod.THREE_MONTHS,
    val overriddenCancellationDate: LocalDate? = null,
    val derivedCancellationDate: LocalDate = LocalDate.EPOCH,
    val loading: Boolean = true,
) {
    val amount: Money get() = Money(amountMinor)
    val canSave: Boolean get() = name.isNotBlank() && amountMinor > 0 && categoryId != null
    val isEditing: Boolean get() = id != 0L
}

@HiltViewModel
class ContractEditorViewModel
    @Inject
    constructor(
        private val contracts: ContractRepository,
        private val categories: CategoryRepository,
        private val saveContract: SaveContractUseCase,
        private val schedule: ContractSchedule,
        private val clock: Clock,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val contractId: Long = savedStateHandle.get<Long>(ARG_CONTRACT_ID) ?: 0L

        private val _state = MutableStateFlow(ContractEditorUiState())
        val state: StateFlow<ContractEditorUiState> = _state.asStateFlow()

        private val _effects = Channel<Unit>(Channel.BUFFERED)
        val saved = _effects.receiveAsFlow()

        init {
            viewModelScope.launch {
                val available = categories.categories()
                val existing = contractId.takeIf { it > 0 }?.let { contracts.contract(it) }
                val nextCharge = existing?.nextChargeOn ?: LocalDate.now(clock).plusMonths(1)
                val notice = existing?.noticePeriod ?: NoticePeriod.THREE_MONTHS

                _state.value =
                    ContractEditorUiState(
                        id = existing?.id ?: 0,
                        name = existing?.name.orEmpty(),
                        amountMinor = existing?.amount?.minor ?: 0,
                        cadence = existing?.cadence ?: Cadence.MONTHLY,
                        categoryId = existing?.categoryId ?: available.firstOrNull()?.id,
                        categories = available,
                        nextChargeOn = nextCharge,
                        noticePeriod = notice,
                        derivedCancellationDate = schedule.earliestCancellationOn(nextCharge, notice),
                        overriddenCancellationDate =
                            existing
                                ?.earliestCancellationOn
                                ?.takeIf { it != schedule.earliestCancellationOn(nextCharge, notice) },
                        loading = false,
                    )
            }
        }

        fun onNameChanged(name: String) = _state.update { it.copy(name = name) }

        fun onAmountChanged(minor: Long) = _state.update { it.copy(amountMinor = minor) }

        fun onCadenceChanged(cadence: Cadence) = _state.update { it.copy(cadence = cadence) }

        fun onCategoryChanged(categoryId: Long) = _state.update { it.copy(categoryId = categoryId) }

        fun onNextChargeChanged(date: LocalDate) =
            _state.update { current ->
                current.copy(
                    nextChargeOn = date,
                    derivedCancellationDate = schedule.earliestCancellationOn(date, current.noticePeriod),
                )
            }

        fun onNoticePeriodChanged(period: NoticePeriod) =
            _state.update { current ->
                current.copy(
                    noticePeriod = period,
                    derivedCancellationDate = schedule.earliestCancellationOn(current.nextChargeOn, period),
                )
            }

        /** Contracts with odd terms can override the derived date; everything else derives it. */
        fun onCancellationOverride(date: LocalDate?) = _state.update { it.copy(overriddenCancellationDate = date) }

        fun onSave() {
            val current = _state.value
            val categoryId = current.categoryId ?: return
            if (!current.canSave) return

            viewModelScope.launch {
                saveContract(
                    Contract(
                        id = current.id,
                        name = current.name.trim(),
                        categoryId = categoryId,
                        amount = current.amount,
                        cadence = current.cadence,
                        nextChargeOn = current.nextChargeOn,
                        noticePeriod = current.noticePeriod,
                        earliestCancellationOn = current.derivedCancellationDate,
                        createdAt = clock.instant(),
                    ),
                    overrideCancellationDate = current.overriddenCancellationDate,
                )
                _effects.send(Unit)
            }
        }

        companion object {
            const val ARG_CONTRACT_ID = "contractId"
        }
    }

@Composable
fun ContractEditorRoute(
    onSaved: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: ContractEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.saved.collect { onSaved() }
    }

    ContractEditorScreen(
        state = state,
        onNameChanged = viewModel::onNameChanged,
        onAmountChanged = viewModel::onAmountChanged,
        onCadenceChanged = viewModel::onCadenceChanged,
        onCategoryChanged = viewModel::onCategoryChanged,
        onNextChargeChanged = viewModel::onNextChargeChanged,
        onNoticePeriodChanged = viewModel::onNoticePeriodChanged,
        onSave = viewModel::onSave,
        onDismiss = onDismiss,
    )
}

/**
 * The contract editor (`docs/03-screens.md` §7).
 *
 * The derived cancellation date is always shown **with its calculation spelled out**, so a wrong
 * renewal date is obvious before it costs money.
 */
@Composable
@Suppress("LongParameterList")
fun ContractEditorScreen(
    state: ContractEditorUiState,
    onNameChanged: (String) -> Unit,
    onAmountChanged: (Long) -> Unit,
    onCadenceChanged: (Cadence) -> Unit,
    onCategoryChanged: (Long) -> Unit,
    onNextChargeChanged: (LocalDate) -> Unit,
    onNoticePeriodChanged: (NoticePeriod) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    formatter: MoneyFormatter = MoneyFormatter(),
) {
    val locale = LocalConfiguration.current.locales[0]

    Column(
        modifier =
            Modifier
                .testTag(TestTags.CONTRACT_EDITOR)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Orbit7Theme.shapes.screenGutter),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Panel(caption = stringResource(R.string.contract_name)) {
            BasicTextField(
                value = state.name,
                onValueChange = onNameChanged,
                textStyle = Orbit7Theme.typography.body.copy(color = Orbit7Theme.colors.onSurface),
                modifier =
                    Modifier
                        .testTag("contract_name_field")
                        .fillMaxWidth(),
            )
        }

        Panel(caption = stringResource(R.string.contract_amount)) {
            BasicTextField(
                value = if (state.amountMinor == 0L) "" else (state.amountMinor / 100).toString(),
                onValueChange = { input ->
                    onAmountChanged((input.filter { it.isDigit() }.toLongOrNull() ?: 0L) * 100)
                },
                textStyle = Orbit7Theme.typography.readoutM.copy(color = Orbit7Theme.colors.onSurface),
                keyboardOptions =
                    androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    ),
                modifier =
                    Modifier
                        .testTag("contract_amount_field")
                        .fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Cadence.entries.forEach { cadence ->
                    val selected = state.cadence == cadence
                    CategoryChip(
                        label = stringResource(cadenceLabel(cadence)),
                        selected = selected,
                        onClick = { onCadenceChanged(cadence) },
                        stateDescription =
                            stringResource(
                                if (selected) CoreR.string.state_selected else CoreR.string.state_not_selected,
                            ),
                        testTag = "contract_cadence_${cadence.name}",
                    )
                }
            }
            Text(
                text =
                    stringResource(
                        R.string.contract_monthly_equivalent,
                        formatter.format(Money(state.amountMinor / state.cadence.months)),
                    ),
                style = Orbit7Theme.typography.bodyS,
                color = Orbit7Theme.colors.onSurfaceMuted,
            )
        }

        Panel(caption = stringResource(R.string.contract_category)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.categories.forEach { category ->
                    val selected = state.categoryId == category.id
                    CategoryChip(
                        label =
                            category.customName
                                ?: category.key?.let { stringResource(categoryNameRes(it)) }
                                ?: "",
                        selected = selected,
                        onClick = { onCategoryChanged(category.id) },
                        stateDescription =
                            stringResource(
                                if (selected) CoreR.string.state_selected else CoreR.string.state_not_selected,
                            ),
                        testTag = TestTags.categoryChip(category.id),
                    )
                }
            }
        }

        Panel(caption = stringResource(R.string.contract_next_charge)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = { onNextChargeChanged(state.nextChargeOn.minusMonths(1)) }) {
                    Text(stringResource(R.string.contract_month_earlier))
                }
                Text(
                    text = formatter.formatLongDate(state.nextChargeOn, locale),
                    style = Orbit7Theme.typography.readoutM,
                    color = Orbit7Theme.colors.onSurface,
                    softWrap = false,
                )
                TextButton(onClick = { onNextChargeChanged(state.nextChargeOn.plusMonths(1)) }) {
                    Text(stringResource(R.string.contract_month_later))
                }
            }
        }

        Panel(caption = stringResource(R.string.contract_notice_period)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NoticePeriod.PRESETS.forEach { preset ->
                    val selected = state.noticePeriod == preset
                    CategoryChip(
                        label = stringResource(noticeLabel(preset)),
                        selected = selected,
                        onClick = { onNoticePeriodChanged(preset) },
                        stateDescription =
                            stringResource(
                                if (selected) CoreR.string.state_selected else CoreR.string.state_not_selected,
                            ),
                        testTag = "contract_notice_${preset.months}_${preset.days}",
                    )
                }
            }
        }

        // The calculation, spelled out. This is the line that stops a contract renewing by accident.
        Panel(
            caption = stringResource(R.string.contract_deadline_caption),
            testTag = TestTags.DERIVED_DEADLINE,
        ) {
            Text(
                text =
                    formatter.formatLongDate(
                        state.overriddenCancellationDate ?: state.derivedCancellationDate,
                        locale,
                    ),
                style = Orbit7Theme.typography.readoutM,
                color = Orbit7Theme.colors.accent,
                softWrap = false,
            )
            Text(
                text =
                    stringResource(
                        R.string.contract_deadline_explained,
                        formatter.formatLongDate(state.nextChargeOn, locale),
                        stringResource(noticeLabel(state.noticePeriod)),
                    ),
                style = Orbit7Theme.typography.bodyS,
                color = Orbit7Theme.colors.onSurfaceMuted,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text(stringResource(CoreR.string.action_cancel))
            }
            Button(
                onClick = onSave,
                enabled = state.canSave,
                modifier =
                    Modifier
                        .testTag(TestTags.CONTRACT_SAVE)
                        .weight(2f),
            ) {
                Text(stringResource(CoreR.string.action_save))
            }
        }
    }
}

private fun cadenceLabel(cadence: Cadence): Int =
    when (cadence) {
        Cadence.MONTHLY -> R.string.cadence_monthly
        Cadence.QUARTERLY -> R.string.cadence_quarterly
        Cadence.YEARLY -> R.string.cadence_yearly
    }

private fun noticeLabel(period: NoticePeriod): Int =
    when {
        period.isNone -> R.string.notice_none
        period.months == 1 -> R.string.notice_one_month
        period.months == 3 -> R.string.notice_three_months
        else -> R.string.notice_custom
    }
