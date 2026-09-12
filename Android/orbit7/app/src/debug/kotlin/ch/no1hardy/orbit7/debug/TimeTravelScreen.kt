package ch.no1hardy.orbit7.debug

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import ch.no1hardy.orbit7.R
import ch.no1hardy.orbit7.core.data.di.TimeTravel
import ch.no1hardy.orbit7.core.designsystem.component.Panel
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme
import ch.no1hardy.orbit7.core.domain.model.Expense
import ch.no1hardy.orbit7.core.domain.model.ExpenseSource
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.repository.CategoryRepository
import ch.no1hardy.orbit7.core.domain.repository.EnergyRepository
import ch.no1hardy.orbit7.core.domain.repository.ExpenseRepository
import ch.no1hardy.orbit7.core.domain.repository.SettlementRepository
import ch.no1hardy.orbit7.core.domain.usecase.SettleDueWeeksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

data class TimeTravelUiState(
    val today: LocalDate = LocalDate.EPOCH,
    val balanceEp: Int = 0,
    val settledWeeks: Int = 0,
    val running: Boolean = false,
)

/**
 * The balancing tool from `docs/02-game-design.md` §8.
 *
 * It advances the injected clock, generates synthetic history and runs settlements in a loop, so a
 * full year of play can be simulated in seconds before any change to the numbers in `GameBalance`
 * is shipped. Debug builds only — the screen does not exist in a release APK.
 */
@HiltViewModel
class TimeTravelViewModel
    @Inject
    constructor(
        private val timeTravel: TimeTravel,
        private val expenses: ExpenseRepository,
        private val categories: CategoryRepository,
        private val settlements: SettlementRepository,
        private val energy: EnergyRepository,
        private val settleDueWeeks: SettleDueWeeksUseCase,
        private val clock: Clock,
    ) : ViewModel() {
        private val _state = MutableStateFlow(TimeTravelUiState())
        val state: StateFlow<TimeTravelUiState> = _state.asStateFlow()

        init {
            refresh()
        }

        fun onAdvance(days: Long) {
            timeTravel.advance(Duration.ofDays(days))
            viewModelScope.launch {
                settleDueWeeks()
                refresh()
            }
        }

        fun onSettleNow() {
            viewModelScope.launch {
                settleDueWeeks()
                refresh()
            }
        }

        /** A year of plausible spending: a weekly rhythm with noise, not a flat line. */
        fun onGenerateYear() {
            viewModelScope.launch {
                _state.update { it.copy(running = true) }
                val available = categories.categories()
                val random = Random(SEED)
                val start = LocalDate.now(clock).minusWeeks(WEEKS_IN_YEAR)

                repeat(WEEKS_IN_YEAR.toInt()) { week ->
                    available.forEachIndexed { index, category ->
                        repeat(random.nextInt(1, 4)) { entry ->
                            val day = start.plusWeeks(week.toLong()).plusDays(random.nextInt(0, 7).toLong())
                            val amount = BASE_MINOR + random.nextInt(0, VARIANCE_MINOR) - index * DRIFT_MINOR
                            expenses.add(
                                Expense(
                                    id = 0,
                                    amount = Money(amount.coerceAtLeast(MIN_MINOR)),
                                    currency = "CHF",
                                    categoryId = category.id,
                                    occurredOn = day,
                                    note = null,
                                    createdAt = clock.instant(),
                                    updatedAt = clock.instant(),
                                    source = ExpenseSource.MANUAL,
                                ),
                            )
                        }
                    }
                }
                settleDueWeeks()
                refresh()
                _state.update { it.copy(running = false) }
            }
        }

        private fun refresh() {
            viewModelScope.launch {
                _state.update {
                    it.copy(
                        today = LocalDate.now(clock),
                        balanceEp = energy.balance(),
                        settledWeeks = settlements.settlements().size,
                    )
                }
            }
        }

        private companion object {
            const val SEED = 7
            const val WEEKS_IN_YEAR = 52L
            const val BASE_MINOR = 2_200L
            const val VARIANCE_MINOR = 4_000
            const val DRIFT_MINOR = 150L
            const val MIN_MINOR = 300L
        }
    }

@Composable
fun TimeTravelScreen(viewModel: TimeTravelViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier =
            Modifier
                .testTag("debug_time_travel")
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Orbit7Theme.shapes.screenGutter),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Panel(caption = stringResource(R.string.debug_title)) {
            Text(
                text = stringResource(R.string.debug_warning),
                style = Orbit7Theme.typography.bodyS,
                color = Orbit7Theme.colors.danger,
            )
            Text(
                text = stringResource(R.string.debug_clock, state.today.toString()),
                style = Orbit7Theme.typography.readoutM,
                color = Orbit7Theme.colors.onSurface,
                softWrap = false,
            )
            Text(
                text = stringResource(R.string.debug_result, state.balanceEp, state.settledWeeks),
                style = Orbit7Theme.typography.body,
                color = Orbit7Theme.colors.energy,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = { viewModel.onAdvance(1) }) { Text(stringResource(R.string.debug_advance_day)) }
            TextButton(onClick = { viewModel.onAdvance(DAYS_PER_WEEK) }) {
                Text(stringResource(R.string.debug_advance_week))
            }
            TextButton(onClick = { viewModel.onAdvance(DAYS_PER_WEEK * 4) }) {
                Text(stringResource(R.string.debug_advance_month))
            }
        }

        TextButton(onClick = viewModel::onGenerateYear, enabled = !state.running) {
            Text(stringResource(R.string.debug_generate))
        }
        TextButton(onClick = viewModel::onSettleNow) { Text(stringResource(R.string.debug_settle)) }
    }
}

private const val DAYS_PER_WEEK = 7L
