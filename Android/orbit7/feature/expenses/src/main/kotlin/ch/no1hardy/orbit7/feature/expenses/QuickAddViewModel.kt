package ch.no1hardy.orbit7.feature.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.no1hardy.orbit7.core.domain.model.Category
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.repository.CategoryRepository
import ch.no1hardy.orbit7.core.domain.repository.ExpenseRepository
import ch.no1hardy.orbit7.core.domain.repository.SettingsRepository
import ch.no1hardy.orbit7.core.domain.usecase.LogExpenseUseCase
import ch.no1hardy.orbit7.core.domain.usecase.UpdateExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Quick Add's state (`docs/03-screens.md` §2). Loading and validation are state, not branches. */
data class QuickAddUiState(
    val amountMinor: Long = 0,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val date: LocalDate = LocalDate.EPOCH,
    val note: String = "",
    val noteExpanded: Boolean = false,
    val editingExpenseId: Long? = null,
    val rappenFirstInput: Boolean = true,
    val loading: Boolean = true,
) {
    val amount: Money get() = Money(amountMinor)
    val isEditing: Boolean get() = editingExpenseId != null

    /** Save is disabled at zero, and the reason is shown rather than left to be guessed. */
    val canSave: Boolean get() = amountMinor > 0 && selectedCategoryId != null
    val showsAmountRequired: Boolean get() = amountMinor <= 0
}

/** One-shot effects go through a channel, never through state, so they cannot replay on rotation. */
sealed interface QuickAddEffect {
    data class Saved(
        val expenseId: Long,
        val isNew: Boolean,
    ) : QuickAddEffect

    data object Dismissed : QuickAddEffect
}

@HiltViewModel
class QuickAddViewModel
    @Inject
    constructor(
        private val categories: CategoryRepository,
        private val expenses: ExpenseRepository,
        private val settings: SettingsRepository,
        private val logExpense: LogExpenseUseCase,
        private val updateExpense: UpdateExpenseUseCase,
        private val clock: Clock,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val editingId: Long? = savedStateHandle.get<Long>(ARG_EXPENSE_ID)?.takeIf { it > 0 }

        private val _state = MutableStateFlow(QuickAddUiState(date = LocalDate.now(clock)))
        val state: StateFlow<QuickAddUiState> = _state.asStateFlow()

        private val _effects = Channel<QuickAddEffect>(Channel.BUFFERED)
        val effects = _effects.receiveAsFlow()

        init {
            viewModelScope.launch {
                val available = categories.categories()
                val appSettings = settings.settings()
                val editing = editingId?.let { expenses.expense(it) }

                _state.value =
                    QuickAddUiState(
                        amountMinor = editing?.amount?.minor ?: 0,
                        categories = available.sortedBy { it.sortOrder },
                        selectedCategoryId = editing?.categoryId ?: recentCategoryId(available),
                        date = editing?.occurredOn ?: LocalDate.now(clock),
                        note = editing?.note.orEmpty(),
                        noteExpanded = !editing?.note.isNullOrBlank(),
                        editingExpenseId = editing?.id,
                        rappenFirstInput = appSettings.rappenFirstInput,
                        loading = false,
                    )
            }
        }

        /**
         * Rappen-first input: `12` is CHF 0.12 and `1250` is CHF 12.50, the way a till works. With the
         * setting off, digits fill the major units instead.
         */
        fun onDigit(digit: Int) {
            _state.update { current ->
                val next =
                    if (current.rappenFirstInput) {
                        current.amountMinor * 10 + digit
                    } else {
                        // Major-first: digits fill the francs and the Rappen stay at zero.
                        (current.amountMinor / MINOR * 10 + digit) * MINOR
                    }
                current.copy(amountMinor = next.coerceAtMost(MAX_AMOUNT_MINOR))
            }
        }

        fun onBackspace() {
            _state.update { current -> current.copy(amountMinor = current.amountMinor / 10) }
        }

        fun onClear() {
            _state.update { it.copy(amountMinor = 0) }
        }

        fun onCategorySelected(categoryId: Long) {
            _state.update { it.copy(selectedCategoryId = categoryId) }
        }

        fun onDateStep(days: Long) {
            _state.update { current ->
                val stepped = current.date.plusDays(days)
                // A spend cannot be logged in the future: it has not happened yet.
                current.copy(date = minOf(stepped, LocalDate.now(clock)))
            }
        }

        fun onNoteChanged(note: String) {
            _state.update { it.copy(note = note) }
        }

        fun onNoteExpanded() {
            _state.update { it.copy(noteExpanded = true) }
        }

        fun onSave() {
            val current = _state.value
            val categoryId = current.selectedCategoryId ?: return
            if (!current.canSave) return

            viewModelScope.launch {
                val editing = current.editingExpenseId
                if (editing != null) {
                    updateExpense(editing, current.amount, categoryId, current.date, current.note)
                    _effects.send(QuickAddEffect.Saved(editing, isNew = false))
                } else {
                    val id = logExpense(current.amount, categoryId, current.date, current.note)
                    _effects.send(QuickAddEffect.Saved(id, isNew = true))
                }
            }
        }

        fun onDismiss() {
            viewModelScope.launch { _effects.send(QuickAddEffect.Dismissed) }
        }

        /** Chips are ordered by the user's own recency and frequency; the first one is preselected. */
        private suspend fun recentCategoryId(available: List<Category>): Long? {
            val recent = expenses.observeRecent(RECENT_WINDOW).first()
            return recent
                .groupingBy { it.categoryId }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
                ?: available.firstOrNull()?.id
        }

        companion object {
            const val ARG_EXPENSE_ID = "expenseId"
            private const val MINOR = 100L
            private const val MAX_AMOUNT_MINOR = 99_999_999L
            private const val RECENT_WINDOW = 30
        }
    }
