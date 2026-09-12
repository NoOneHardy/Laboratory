package ch.no1hardy.orbit7

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.no1hardy.orbit7.core.domain.repository.SettingsRepository
import ch.no1hardy.orbit7.core.domain.repository.SettlementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface MainUiState {
    data object Loading : MainUiState

    data class Ready(
        val onboardingCompleted: Boolean,
        val reduceEffects: Boolean,
        /** Non-null when a settlement has happened that the user has not seen yet. */
        val pendingSettlementWeekId: String?,
    ) : MainUiState
}

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        settings: SettingsRepository,
        settlements: SettlementRepository,
    ) : ViewModel() {
        val state: StateFlow<MainUiState> =
            combine(
                settings.observeSettings(),
                settlements.observeSettlements(),
            ) { appSettings, history ->
                MainUiState.Ready(
                    onboardingCompleted = appSettings.onboardingCompleted,
                    reduceEffects = appSettings.reduceEffects,
                    pendingSettlementWeekId =
                        history
                            .maxByOrNull { it.week }
                            ?.takeIf { it.week.id != appSettings.lastAcknowledgedSettlementWeekId }
                            ?.week
                            ?.id,
                )
            }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)
    }
