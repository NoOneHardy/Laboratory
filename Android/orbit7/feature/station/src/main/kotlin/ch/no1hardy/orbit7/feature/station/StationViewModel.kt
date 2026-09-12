package ch.no1hardy.orbit7.feature.station

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.no1hardy.orbit7.core.domain.economy.ModuleOffer
import ch.no1hardy.orbit7.core.domain.economy.StationCatalog
import ch.no1hardy.orbit7.core.domain.model.StationModuleKey
import ch.no1hardy.orbit7.core.domain.repository.EnergyRepository
import ch.no1hardy.orbit7.core.domain.repository.StationRepository
import ch.no1hardy.orbit7.core.domain.usecase.PurchaseModuleUpgradeUseCase
import ch.no1hardy.orbit7.core.domain.usecase.PurchaseRejection
import ch.no1hardy.orbit7.core.domain.usecase.PurchaseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StationUiState(
    val balanceEp: Int = 0,
    val offers: List<ModuleOffer> = emptyList(),
    val levels: Map<StationModuleKey, Int> = emptyMap(),
    val nearestGoal: ModuleOffer? = null,
    val selectedModule: StationModuleKey? = null,
    val purchaseInProgress: StationModuleKey? = null,
    val loading: Boolean = true,
) {
    val selectedOffer: ModuleOffer? get() = offers.firstOrNull { it.moduleKey == selectedModule }
    val hasAnythingAffordable: Boolean get() = offers.any { it.canPurchase }
}

sealed interface StationEffect {
    /** The second payoff moment of the app: the energy-transfer animation runs on this. */
    data class PoweredUp(
        val moduleKey: StationModuleKey,
        val newLevel: Int,
        val spentEp: Long,
    ) : StationEffect

    data class Refused(
        val reason: PurchaseRejection,
        val shortfallEp: Long,
    ) : StationEffect
}

@HiltViewModel
class StationViewModel
    @Inject
    constructor(
        station: StationRepository,
        energy: EnergyRepository,
        private val catalog: StationCatalog,
        private val purchase: PurchaseModuleUpgradeUseCase,
    ) : ViewModel() {
        private val selected = MutableStateFlow<StationModuleKey?>(null)
        private val inProgress = MutableStateFlow<StationModuleKey?>(null)

        private val _effects = Channel<StationEffect>(Channel.BUFFERED)
        val effects = _effects.receiveAsFlow()

        val state: StateFlow<StationUiState> =
            combine(
                station.observeModules(),
                energy.observeBalance(),
                selected,
                inProgress,
            ) { modules, balance, selectedModule, purchasing ->
                val states = modules.associateBy { it.moduleKey }
                StationUiState(
                    balanceEp = balance,
                    offers = catalog.offers(states, balance.toLong()),
                    levels = modules.associate { it.moduleKey to it.level },
                    nearestGoal = catalog.nearestGoal(states, balance.toLong()),
                    selectedModule = selectedModule,
                    purchaseInProgress = purchasing,
                    loading = false,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), StationUiState())

        fun onModuleSelected(moduleKey: StationModuleKey?) {
            selected.value = moduleKey
        }

        fun onPowerUp(moduleKey: StationModuleKey) {
            if (inProgress.value != null) return // the long animation never runs twice for one event
            viewModelScope.launch {
                inProgress.value = moduleKey
                when (val result = purchase(moduleKey)) {
                    is PurchaseResult.Success ->
                        _effects.send(StationEffect.PoweredUp(result.moduleKey, result.newLevel, result.spentEp))

                    is PurchaseResult.Rejected ->
                        _effects.send(StationEffect.Refused(result.reason, result.shortfallEp))
                }
                inProgress.value = null
                selected.value = null
            }
        }

        private companion object {
            const val STOP_TIMEOUT = 5_000L
        }
    }
