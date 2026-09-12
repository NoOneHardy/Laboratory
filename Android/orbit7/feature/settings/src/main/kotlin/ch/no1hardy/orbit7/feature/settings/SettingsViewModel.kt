package ch.no1hardy.orbit7.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.no1hardy.orbit7.core.domain.model.AppLanguage
import ch.no1hardy.orbit7.core.domain.model.AppSettings
import ch.no1hardy.orbit7.core.domain.repository.BackupRepository
import ch.no1hardy.orbit7.core.domain.repository.ImportFailure
import ch.no1hardy.orbit7.core.domain.repository.ImportResult
import ch.no1hardy.orbit7.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val confirmingWipe: Boolean = false,
    val confirmingImport: String? = null,
    val busy: Boolean = false,
)

sealed interface SettingsEffect {
    data class ExportReady(
        val json: String,
    ) : SettingsEffect

    data class Imported(
        val expenses: Int,
        val contracts: Int,
    ) : SettingsEffect

    data class ImportFailed(
        val reason: ImportFailure,
    ) : SettingsEffect

    data object Wiped : SettingsEffect
}

/**
 * Settings (`docs/03-screens.md` §9).
 *
 * The two destructive paths — import (replace-all) and wipe — are the only places in the app with a
 * confirmation step, and wipe has two.
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settings: SettingsRepository,
        private val backup: BackupRepository,
    ) : ViewModel() {
        private val confirmingWipe = MutableStateFlow(false)
        private val pendingImport = MutableStateFlow<String?>(null)
        private val busy = MutableStateFlow(false)

        private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
        val effects = _effects.receiveAsFlow()

        val state: StateFlow<SettingsUiState> =
            combine(
                settings.observeSettings(),
                confirmingWipe,
                pendingImport,
                busy,
            ) { appSettings, wipe, import, isBusy ->
                SettingsUiState(appSettings, wipe, import, isBusy)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), SettingsUiState())

        fun onLanguageChanged(language: AppLanguage) = update { it.copy(language = language) }

        fun onFirstDayOfWeekChanged(day: DayOfWeek) = update { it.copy(firstDayOfWeek = day) }

        fun onRappenFirstChanged(enabled: Boolean) = update { it.copy(rappenFirstInput = enabled) }

        fun onDailyReminderChanged(enabled: Boolean) = update { it.copy(dailyReminderEnabled = enabled) }

        fun onReminderTimeChanged(time: LocalTime) = update { it.copy(dailyReminderTime = time) }

        fun onContractRemindersChanged(enabled: Boolean) = update { it.copy(contractRemindersEnabled = enabled) }

        /** The accessibility promise: every retro effect can be switched off, with the app still usable. */
        fun onReduceEffectsChanged(enabled: Boolean) = update { it.copy(reduceEffects = enabled) }

        fun onSoundChanged(enabled: Boolean) = update { it.copy(soundEnabled = enabled) }

        fun onExport() {
            viewModelScope.launch {
                busy.value = true
                _effects.send(SettingsEffect.ExportReady(backup.export()))
                busy.value = false
            }
        }

        fun onImportRequested(json: String) {
            pendingImport.value = json
        }

        fun onImportDismissed() {
            pendingImport.value = null
        }

        /** Import replaces everything on the device; the confirmation says so in those words. */
        fun onImportConfirmed() {
            val json = pendingImport.value ?: return
            viewModelScope.launch {
                busy.value = true
                pendingImport.value = null
                when (val result = backup.import(json)) {
                    is ImportResult.Success ->
                        _effects.send(SettingsEffect.Imported(result.expenses, result.contracts))

                    is ImportResult.Failure -> _effects.send(SettingsEffect.ImportFailed(result.reason))
                }
                busy.value = false
            }
        }

        fun onWipeRequested() {
            confirmingWipe.value = true
        }

        fun onWipeDismissed() {
            confirmingWipe.value = false
        }

        fun onWipeConfirmed() {
            viewModelScope.launch {
                busy.value = true
                confirmingWipe.value = false
                backup.wipeAllData()
                settings.update { AppSettings() }
                _effects.send(SettingsEffect.Wiped)
                busy.value = false
            }
        }

        private fun update(transform: (AppSettings) -> AppSettings) {
            viewModelScope.launch { settings.update(transform) }
        }

        private companion object {
            const val STOP_TIMEOUT = 5_000L
        }
    }
