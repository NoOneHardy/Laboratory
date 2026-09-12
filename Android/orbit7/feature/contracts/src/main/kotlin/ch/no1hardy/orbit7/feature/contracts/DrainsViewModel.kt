package ch.no1hardy.orbit7.feature.contracts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.usecase.CancelContractUseCase
import ch.no1hardy.orbit7.core.domain.usecase.DrainsState
import ch.no1hardy.orbit7.core.domain.usecase.ObserveDrainsUseCase
import ch.no1hardy.orbit7.core.domain.usecase.RenegotiateContractUseCase
import ch.no1hardy.orbit7.core.domain.usecase.SalvageResult
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

data class DrainsUiState(
    val drains: DrainsState = DrainsState(emptyList(), emptyList(), Money.ZERO, 0, Money.ZERO),
    val confirmingCancelId: Long? = null,
    val renegotiatingId: Long? = null,
    val loading: Boolean = true,
) {
    val isEmpty: Boolean get() = !loading && drains.active.isEmpty() && drains.salvaged.isEmpty()
}

sealed interface DrainsEffect {
    /** The salvage payout animation runs on this, once per transition. */
    data class Salvaged(
        val ep: Int,
        val monthlySaving: Money,
    ) : DrainsEffect

    data object AlreadySalvaged : DrainsEffect

    data object NoSaving : DrainsEffect
}

@HiltViewModel
class DrainsViewModel
    @Inject
    constructor(
        observeDrains: ObserveDrainsUseCase,
        private val cancelContract: CancelContractUseCase,
        private val renegotiateContract: RenegotiateContractUseCase,
    ) : ViewModel() {
        private val confirmingCancel = MutableStateFlow<Long?>(null)
        private val renegotiating = MutableStateFlow<Long?>(null)

        private val _effects = Channel<DrainsEffect>(Channel.BUFFERED)
        val effects = _effects.receiveAsFlow()

        val state: StateFlow<DrainsUiState> =
            combine(
                observeDrains(),
                confirmingCancel,
                renegotiating,
            ) { drains, confirming, renegotiatingId ->
                DrainsUiState(drains, confirming, renegotiatingId, loading = false)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), DrainsUiState())

        /** Cutting a drain is irreversible and pays real EP, so it is always confirmed first. */
        fun onCancelRequested(contractId: Long) {
            confirmingCancel.value = contractId
        }

        fun onCancelDismissed() {
            confirmingCancel.value = null
        }

        fun onCancelConfirmed(contractId: Long) {
            viewModelScope.launch {
                confirmingCancel.value = null
                emit(cancelContract(contractId))
            }
        }

        fun onRenegotiateRequested(contractId: Long) {
            renegotiating.value = contractId
        }

        fun onRenegotiateDismissed() {
            renegotiating.value = null
        }

        fun onRenegotiateConfirmed(
            contractId: Long,
            newAmount: Money,
        ) {
            viewModelScope.launch {
                renegotiating.value = null
                emit(renegotiateContract(contractId, newAmount))
            }
        }

        private suspend fun emit(result: SalvageResult) {
            val effect =
                when (result) {
                    is SalvageResult.Paid -> DrainsEffect.Salvaged(result.ep, result.monthlySaving)
                    SalvageResult.AlreadySalvaged -> DrainsEffect.AlreadySalvaged
                    SalvageResult.NoSaving -> DrainsEffect.NoSaving
                    SalvageResult.NotFound -> DrainsEffect.AlreadySalvaged
                }
            _effects.send(effect)
        }

        private companion object {
            const val STOP_TIMEOUT = 5_000L
        }
    }
