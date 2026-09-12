package ch.no1hardy.orbit7.feature.settings

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.categoryNameRes
import ch.no1hardy.orbit7.core.designsystem.component.CategoryChip
import ch.no1hardy.orbit7.core.designsystem.component.Panel
import ch.no1hardy.orbit7.core.designsystem.component.StationView
import ch.no1hardy.orbit7.core.designsystem.format.MoneyFormatter
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme
import ch.no1hardy.orbit7.core.domain.model.CategoryKey
import ch.no1hardy.orbit7.core.domain.model.StationModuleKey
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.repository.BudgetRepository
import ch.no1hardy.orbit7.core.domain.repository.CategoryRepository
import ch.no1hardy.orbit7.core.domain.repository.SettingsRepository
import ch.no1hardy.orbit7.core.domain.repository.StationRepository
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
import java.time.LocalTime
import javax.inject.Inject
import ch.no1hardy.orbit7.core.designsystem.R as CoreR

/**
 * Suggested starting budgets for the Swiss default set, in CHF per month.
 *
 * These are starting points a single person in Switzerland can recognise, not prescriptions — every
 * one is editable on the same screen, and the app never pushes a target below a floor the user set
 * (`docs/02-game-design.md` §8).
 */
private val SUGGESTED_MONTHLY_BUDGETS: Map<CategoryKey, Long> =
    mapOf(
        CategoryKey.GROCERIES to 600,
        CategoryKey.EATING_OUT to 250,
        CategoryKey.TRANSPORT to 120,
        CategoryKey.HOUSEHOLD to 200,
        CategoryKey.HEALTH to 100,
        CategoryKey.LEISURE to 180,
        CategoryKey.SUBSCRIPTIONS to 80,
        CategoryKey.OTHER to 100,
    )

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.PITCH,
    val selectedCategories: Set<CategoryKey> = SUGGESTED_MONTHLY_BUDGETS.keys,
    val budgets: Map<CategoryKey, Long> = SUGGESTED_MONTHLY_BUDGETS,
    val reminderEnabled: Boolean = true,
    val reminderTime: LocalTime = LocalTime.of(20, 0),
    val finishing: Boolean = false,
)

enum class OnboardingStep {
    PITCH,
    CATEGORIES,
    REMINDER,
}

/**
 * Three steps, skippable at any point with sensible defaults applied (`docs/03-screens.md` §10).
 *
 * On completion the Reactor Core is lit at level 1, so the station is never empty on first run and
 * the loop has something to build on.
 */
@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val categories: CategoryRepository,
        private val budgets: BudgetRepository,
        private val station: StationRepository,
        private val settings: SettingsRepository,
        private val clock: Clock,
    ) : ViewModel() {
        private val _state = MutableStateFlow(OnboardingUiState())
        val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

        private val _completed = Channel<Unit>(Channel.BUFFERED)
        val completed = _completed.receiveAsFlow()

        fun onNext() {
            val current = _state.value
            when (current.step) {
                OnboardingStep.PITCH -> _state.update { it.copy(step = OnboardingStep.CATEGORIES) }
                OnboardingStep.CATEGORIES -> _state.update { it.copy(step = OnboardingStep.REMINDER) }
                OnboardingStep.REMINDER -> finish()
            }
        }

        fun onCategoryToggled(key: CategoryKey) =
            _state.update { current ->
                val selected =
                    if (key in current.selectedCategories) {
                        current.selectedCategories - key
                    } else {
                        current.selectedCategories + key
                    }
                current.copy(selectedCategories = selected)
            }

        fun onBudgetChanged(
            key: CategoryKey,
            monthly: Long,
        ) = _state.update { current ->
            current.copy(budgets = current.budgets + (key to monthly))
        }

        fun onReminderChanged(enabled: Boolean) = _state.update { it.copy(reminderEnabled = enabled) }

        fun onReminderTimeChanged(time: LocalTime) = _state.update { it.copy(reminderTime = time) }

        /** Skipping is a first-class path: the defaults are what a skipping user gets. */
        fun onSkip() = finish()

        private fun finish() {
            if (_state.value.finishing) return
            _state.update { it.copy(finishing = true) }

            viewModelScope.launch {
                val current = _state.value
                val today = LocalDate.now(clock)
                val keys = SUGGESTED_MONTHLY_BUDGETS.keys.filter { it in current.selectedCategories }
                val created = categories.seedDefaults(keys)

                created.forEach { category ->
                    val key = category.key ?: return@forEach
                    val monthly = current.budgets[key] ?: SUGGESTED_MONTHLY_BUDGETS.getValue(key)
                    budgets.setMonthlyBudget(category.id, Money.ofMajor(monthly), today)
                }

                // One module already lit, so the first Bridge is a station coming online rather than a
                // dark screen with zeros on it.
                station.setLevel(StationModuleKey.REACTOR_CORE, level = 1, at = clock.instant())

                settings.update {
                    it.copy(
                        onboardingCompleted = true,
                        dailyReminderEnabled = current.reminderEnabled,
                        dailyReminderTime = current.reminderTime,
                        lastAppUsageOn = today,
                    )
                }
                _completed.send(Unit)
            }
        }
    }

@Composable
fun OnboardingRoute(
    onCompleted: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.completed.collect { onCompleted() }
    }

    OnboardingScreen(
        state = state,
        onNext = viewModel::onNext,
        onSkip = viewModel::onSkip,
        onCategoryToggled = viewModel::onCategoryToggled,
        onBudgetChanged = viewModel::onBudgetChanged,
        onReminderChanged = viewModel::onReminderChanged,
        onReminderTimeChanged = viewModel::onReminderTimeChanged,
    )
}

@Composable
@Suppress("LongParameterList")
fun OnboardingScreen(
    state: OnboardingUiState,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onCategoryToggled: (CategoryKey) -> Unit,
    onBudgetChanged: (CategoryKey, Long) -> Unit,
    onReminderChanged: (Boolean) -> Unit,
    onReminderTimeChanged: (LocalTime) -> Unit,
    formatter: MoneyFormatter = MoneyFormatter(),
) {
    Column(
        modifier =
            Modifier
                .testTag(TestTags.ONBOARDING)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Orbit7Theme.shapes.screenGutter),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (state.step) {
            OnboardingStep.PITCH -> PitchStep()
            OnboardingStep.CATEGORIES ->
                CategoriesStep(
                    state = state,
                    onCategoryToggled = onCategoryToggled,
                    onBudgetChanged = onBudgetChanged,
                    formatter = formatter,
                )

            OnboardingStep.REMINDER ->
                ReminderStep(
                    state = state,
                    onReminderChanged = onReminderChanged,
                    onReminderTimeChanged = onReminderTimeChanged,
                )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                onClick = onSkip,
                modifier =
                    Modifier
                        .testTag(TestTags.ONBOARDING_SKIP)
                        .weight(1f),
            ) {
                Text(stringResource(CoreR.string.action_skip))
            }
            Button(
                onClick = onNext,
                enabled = !state.finishing,
                modifier =
                    Modifier
                        .testTag(TestTags.ONBOARDING_NEXT)
                        .weight(2f),
            ) {
                Text(
                    stringResource(
                        if (state.step == OnboardingStep.REMINDER) {
                            R.string.onboarding_start
                        } else {
                            CoreR.string.action_continue
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun PitchStep() {
    StationView(
        levels = emptyMap(),
        contentDescription = stringResource(R.string.onboarding_station_dark),
    )
    Panel(caption = stringResource(R.string.onboarding_pitch_caption)) {
        Text(
            text = stringResource(R.string.onboarding_pitch_body),
            style = Orbit7Theme.typography.body,
            color = Orbit7Theme.colors.onSurface,
        )
    }
}

@Composable
private fun CategoriesStep(
    state: OnboardingUiState,
    onCategoryToggled: (CategoryKey) -> Unit,
    onBudgetChanged: (CategoryKey, Long) -> Unit,
    formatter: MoneyFormatter,
) {
    Panel(caption = stringResource(R.string.onboarding_categories_caption)) {
        Text(
            text = stringResource(R.string.onboarding_categories_body),
            style = Orbit7Theme.typography.bodyS,
            color = Orbit7Theme.colors.onSurfaceMuted,
        )
        state.budgets.keys.sortedBy { it.ordinal }.forEach { key ->
            val selected = key in state.selectedCategories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CategoryChip(
                    label = stringResource(categoryNameRes(key)),
                    selected = selected,
                    onClick = { onCategoryToggled(key) },
                    stateDescription =
                        stringResource(
                            if (selected) CoreR.string.state_selected else CoreR.string.state_not_selected,
                        ),
                    testTag = TestTags.onboardingCategory(key.name),
                    modifier = Modifier.weight(1f),
                )
                BasicTextField(
                    value = (state.budgets[key] ?: 0L).toString(),
                    onValueChange = { input ->
                        onBudgetChanged(key, input.filter { it.isDigit() }.toLongOrNull() ?: 0L)
                    },
                    textStyle = Orbit7Theme.typography.readoutM.copy(color = Orbit7Theme.colors.onSurface),
                    keyboardOptions =
                        androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        ),
                    modifier =
                        Modifier
                            .testTag("onboarding_budget_${key.name}")
                            .weight(1f),
                )
            }
        }
        Text(
            text =
                stringResource(
                    R.string.onboarding_total,
                    formatter.format(
                        Money.ofMajor(
                            state.budgets
                                .filterKeys { it in state.selectedCategories }
                                .values
                                .sum(),
                        ),
                    ),
                ),
            style = Orbit7Theme.typography.readoutM,
            color = Orbit7Theme.colors.accent,
            softWrap = false,
        )
    }
}

@Composable
private fun ReminderStep(
    state: OnboardingUiState,
    onReminderChanged: (Boolean) -> Unit,
    onReminderTimeChanged: (LocalTime) -> Unit,
) {
    Panel(caption = stringResource(R.string.onboarding_reminder_caption)) {
        Text(
            text = stringResource(R.string.onboarding_reminder_body),
            style = Orbit7Theme.typography.body,
            color = Orbit7Theme.colors.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(18, 20, 21).forEach { hour ->
                val time = LocalTime.of(hour, 0)
                val selected = state.reminderEnabled && state.reminderTime == time
                CategoryChip(
                    label = stringResource(R.string.settings_reminder_hour, hour),
                    selected = selected,
                    onClick = {
                        onReminderChanged(true)
                        onReminderTimeChanged(time)
                    },
                    stateDescription =
                        stringResource(
                            if (selected) CoreR.string.state_selected else CoreR.string.state_not_selected,
                        ),
                    testTag = "onboarding_reminder_$hour",
                )
            }
        }
        TextButton(onClick = { onReminderChanged(false) }, modifier = Modifier.testTag("onboarding_no_reminder")) {
            Text(stringResource(R.string.onboarding_no_reminder))
        }
    }
}
