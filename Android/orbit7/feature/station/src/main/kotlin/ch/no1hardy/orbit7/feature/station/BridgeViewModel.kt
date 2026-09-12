package ch.no1hardy.orbit7.feature.station

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.no1hardy.orbit7.core.domain.usecase.BridgeState
import ch.no1hardy.orbit7.core.domain.usecase.MarkZeroSpendDayUseCase
import ch.no1hardy.orbit7.core.domain.usecase.ObserveBridgeStateUseCase
import ch.no1hardy.orbit7.core.domain.usecase.UnmarkZeroSpendDayUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The Bridge's state: loading and first-run are states, not separate code paths. */
sealed interface BridgeUiState {
    data object Loading : BridgeUiState

    /**
     * @param firstRun no data at all yet: the station is fully dark and the copy explains the loop
     * instead of showing zeros.
     */
    data class Ready(
        val bridge: BridgeState,
        val firstRun: Boolean,
    ) : BridgeUiState
}

@HiltViewModel
class BridgeViewModel
    @Inject
    constructor(
        observeBridgeState: ObserveBridgeStateUseCase,
        private val markZeroSpend: MarkZeroSpendDayUseCase,
        private val unmarkZeroSpend: UnmarkZeroSpendDayUseCase,
    ) : ViewModel() {
        val state: StateFlow<BridgeUiState> =
            observeBridgeState()
                .map { bridge -> BridgeUiState.Ready(bridge, firstRun = !bridge.hasAnyData) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), BridgeUiState.Loading)

        /** One tap, from the today card. Offered, never required. */
        fun onMarkZeroSpendDay() {
            viewModelScope.launch { markZeroSpend() }
        }

        fun onUndoZeroSpendDay() {
            val current = state.value
            if (current is BridgeUiState.Ready) {
                viewModelScope.launch { unmarkZeroSpend(current.bridge.today) }
            }
        }

        private companion object {
            const val STOP_TIMEOUT = 5_000L
        }
    }
