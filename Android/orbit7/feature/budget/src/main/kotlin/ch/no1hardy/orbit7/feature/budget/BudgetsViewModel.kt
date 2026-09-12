package ch.no1hardy.orbit7.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.repository.BudgetRepository
import ch.no1hardy.orbit7.core.domain.usecase.AcceptBaselineSuggestionUseCase
import ch.no1hardy.orbit7.core.domain.usecase.CategoryBudget
import ch.no1hardy.orbit7.core.domain.usecase.DismissBaselineSuggestionUseCase
import ch.no1hardy.orbit7.core.domain.usecase.ObserveBudgetsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** The Budgets screen can show everything monthly or prorated to the week. */
enum class BudgetPeriod {
    MONTH,
    WEEK,
}

data class BudgetsUiState(
    val rows: List<CategoryBudget> = emptyList(),
    val period: BudgetPeriod = BudgetPeriod.MONTH,
    val editingCategoryId: Long? = null,
    val loading: Boolean = true,
) {
    val isEmpty: Boolean get() = !loading && rows.isEmpty()

    /** Before 28 days of data there is no baseline, and the screen says why rather than hiding it. */
    val anyBaselines: Boolean get() = rows.any { it.hasBaseline }
}

@HiltViewModel
class BudgetsViewModel
    @Inject
    constructor(
        observeBudgets: ObserveBudgetsUseCase,
        private val budgets: BudgetRepository,
        private val acceptSuggestion: AcceptBaselineSuggestionUseCase,
        private val dismissSuggestion: DismissBaselineSuggestionUseCase,
        private val clock: Clock,
    ) : ViewModel() {
        private val period = MutableStateFlow(BudgetPeriod.MONTH)
        private val editing = MutableStateFlow<Long?>(null)

        val state: StateFlow<BudgetsUiState> =
            combine(
                observeBudgets(),
                period,
                editing,
            ) { rows, selectedPeriod, editingId ->
                BudgetsUiState(rows = rows, period = selectedPeriod, editingCategoryId = editingId, loading = false)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), BudgetsUiState())

        fun onPeriodChanged(newPeriod: BudgetPeriod) {
            period.value = newPeriod
        }

        fun onEdit(categoryId: Long?) {
            editing.value = categoryId
        }

        /** A budget change opens a new version; March's settlement still evaluates against March. */
        fun onBudgetChanged(
            categoryId: Long,
            monthlyAmount: Money,
        ) {
            viewModelScope.launch {
                budgets.setMonthlyBudget(categoryId, monthlyAmount, LocalDate.now(clock))
                editing.value = null
            }
        }

        fun onAcceptSuggestion(
            categoryId: Long,
            monthlyAmount: Money,
        ) {
            viewModelScope.launch { acceptSuggestion(categoryId, monthlyAmount) }
        }

        fun onDismissSuggestion(categoryId: Long) {
            viewModelScope.launch { dismissSuggestion(categoryId) }
        }

        private companion object {
            const val STOP_TIMEOUT = 5_000L
        }
    }
