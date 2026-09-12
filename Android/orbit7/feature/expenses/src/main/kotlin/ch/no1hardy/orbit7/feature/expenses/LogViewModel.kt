package ch.no1hardy.orbit7.feature.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.no1hardy.orbit7.core.domain.model.Category
import ch.no1hardy.orbit7.core.domain.model.Expense
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.money.sum
import ch.no1hardy.orbit7.core.domain.repository.CategoryRepository
import ch.no1hardy.orbit7.core.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** A day's worth of entries, with its total — the Log is grouped by day, newest first. */
data class LogDay(
    val date: LocalDate,
    val entries: List<Expense>,
    val total: Money,
)

/** A month header with its total, sticky above the days it contains. */
data class LogMonth(
    val month: YearMonth,
    val total: Money,
    val days: List<LogDay>,
)

data class LogFilters(
    val categoryId: Long? = null,
    val range: LogRange = LogRange.LAST_30_DAYS,
) {
    val isActive: Boolean get() = categoryId != null || range != LogRange.LAST_30_DAYS
}

enum class LogRange {
    THIS_MONTH,
    LAST_30_DAYS,
    EVERYTHING,
}

data class LogUiState(
    val months: List<LogMonth> = emptyList(),
    val categories: List<Category> = emptyList(),
    val filters: LogFilters = LogFilters(),
    val loading: Boolean = true,
) {
    val isEmpty: Boolean get() = !loading && months.isEmpty()

    /** An empty list with filters applied says something different, and offers a way out. */
    val isFilteredEmpty: Boolean get() = isEmpty && filters.isActive
}

sealed interface LogEffect {
    /** A deleted entry is held for undo; the snackbar is the only thing standing between the two. */
    data class Deleted(
        val expense: Expense,
    ) : LogEffect
}

@HiltViewModel
class LogViewModel
    @Inject
    constructor(
        private val expenses: ExpenseRepository,
        categories: CategoryRepository,
        private val clock: Clock,
    ) : ViewModel() {
        private val filters = MutableStateFlow(LogFilters())

        private val _effects = Channel<LogEffect>(Channel.BUFFERED)
        val effects = _effects.receiveAsFlow()

        val state: StateFlow<LogUiState> =
            combine(
                expenses.observeAll(),
                categories.observeCategories(includeArchived = true),
                filters,
            ) { allExpenses, allCategories, activeFilters ->
                LogUiState(
                    months = group(filter(allExpenses, activeFilters)),
                    categories = allCategories,
                    filters = activeFilters,
                    loading = false,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), LogUiState())

        fun onCategoryFilter(categoryId: Long?) = filters.update { it.copy(categoryId = categoryId) }

        fun onRangeFilter(range: LogRange) = filters.update { it.copy(range = range) }

        fun onClearFilters() {
            filters.value = LogFilters()
        }

        fun onDelete(expense: Expense) {
            viewModelScope.launch {
                expenses.delete(expense.id)
                _effects.send(LogEffect.Deleted(expense))
            }
        }

        /** Undo restores the row, keeping its original dates — it is a restore, not a new entry. */
        fun onUndoDelete(expense: Expense) {
            viewModelScope.launch { expenses.add(expense.copy(id = 0)) }
        }

        private fun filter(
            expenses: List<Expense>,
            filters: LogFilters,
        ): List<Expense> {
            val today = LocalDate.now(clock)
            val from =
                when (filters.range) {
                    LogRange.THIS_MONTH -> today.withDayOfMonth(1)
                    LogRange.LAST_30_DAYS -> today.minusDays(DAYS_IN_RANGE)
                    LogRange.EVERYTHING -> LocalDate.MIN
                }
            return expenses
                .filter { filters.categoryId == null || it.categoryId == filters.categoryId }
                .filter { !it.occurredOn.isBefore(from) }
        }

        private fun group(expenses: List<Expense>): List<LogMonth> =
            expenses
                .groupBy { YearMonth.from(it.occurredOn) }
                .toSortedMap(compareByDescending { it })
                .map { (month, monthExpenses) ->
                    LogMonth(
                        month = month,
                        total = monthExpenses.map { it.amount }.sum(),
                        days =
                            monthExpenses
                                .groupBy { it.occurredOn }
                                .toSortedMap(compareByDescending { it })
                                .map { (date, dayExpenses) ->
                                    LogDay(
                                        date = date,
                                        entries = dayExpenses.sortedByDescending { it.createdAt },
                                        total = dayExpenses.map { it.amount }.sum(),
                                    )
                                },
                    )
                }

        private companion object {
            const val STOP_TIMEOUT = 5_000L
            const val DAYS_IN_RANGE = 30L
        }
    }
