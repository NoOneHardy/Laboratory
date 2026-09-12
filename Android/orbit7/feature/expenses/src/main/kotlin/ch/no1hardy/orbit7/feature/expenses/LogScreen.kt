package ch.no1hardy.orbit7.feature.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.component.CategoryChip
import ch.no1hardy.orbit7.core.designsystem.component.Panel
import ch.no1hardy.orbit7.core.designsystem.format.MoneyFormatter
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme
import ch.no1hardy.orbit7.core.domain.model.Expense
import ch.no1hardy.orbit7.core.designsystem.R as CoreR
import java.time.format.TextStyle as JavaTextStyle

@Composable
fun LogRoute(
    onEditExpense: (Long) -> Unit,
    viewModel: LogViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LogScreen(
        state = state,
        onEditExpense = onEditExpense,
        onDelete = viewModel::onDelete,
        onCategoryFilter = viewModel::onCategoryFilter,
        onRangeFilter = viewModel::onRangeFilter,
        onClearFilters = viewModel::onClearFilters,
    )
}

/**
 * The Log (`docs/03-screens.md` §3): entries grouped by day with day totals, a sticky month header
 * with the month total, filters, and delete-with-undo.
 */
@Composable
fun LogScreen(
    state: LogUiState,
    onEditExpense: (Long) -> Unit,
    onDelete: (Expense) -> Unit,
    onCategoryFilter: (Long?) -> Unit,
    onRangeFilter: (LogRange) -> Unit,
    onClearFilters: () -> Unit,
    formatter: MoneyFormatter = MoneyFormatter(),
) {
    Column(
        modifier =
            Modifier
                .testTag(TestTags.LOG)
                .fillMaxSize()
                .padding(horizontal = Orbit7Theme.shapes.screenGutter),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FilterRow(
            state = state,
            onCategoryFilter = onCategoryFilter,
            onRangeFilter = onRangeFilter,
        )

        when {
            state.isFilteredEmpty ->
                EmptyState(
                    title = stringResource(R.string.log_filtered_empty_title),
                    body = null,
                    actionLabel = stringResource(R.string.log_clear_filters),
                    onAction = onClearFilters,
                    actionTag = TestTags.LOG_CLEAR_FILTERS,
                )

            state.isEmpty ->
                EmptyState(
                    title = stringResource(R.string.log_empty_title),
                    body = stringResource(R.string.log_empty_body),
                    actionLabel = null,
                    onAction = {},
                    actionTag = null,
                )

            else ->
                LazyColumn(
                    modifier =
                        Modifier
                            .testTag(TestTags.LOG_LIST)
                            .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.months.forEach { month ->
                        item(key = "month-${month.month}") {
                            MonthHeader(month = month, formatter = formatter)
                        }
                        month.days.forEach { day ->
                            item(key = "day-${day.date}") {
                                DayHeader(day = day, formatter = formatter)
                            }
                            items(day.entries, key = { it.id }) { expense ->
                                LogEntryRow(
                                    expense = expense,
                                    categoryName =
                                        state.categories
                                            .firstOrNull { it.id == expense.categoryId }
                                            ?.let { categoryLabel(it) }
                                            .orEmpty(),
                                    formatter = formatter,
                                    onClick = { onEditExpense(expense.id) },
                                    onDelete = { onDelete(expense) },
                                )
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun FilterRow(
    state: LogUiState,
    onCategoryFilter: (Long?) -> Unit,
    onRangeFilter: (LogRange) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .testTag(TestTags.LOG_FILTERS)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CategoryChip(
            label = stringResource(R.string.log_filter_all),
            selected = state.filters.categoryId == null,
            onClick = { onCategoryFilter(null) },
            stateDescription =
                stringResource(
                    if (state.filters.categoryId ==
                        null
                    ) {
                        CoreR.string.state_selected
                    } else {
                        CoreR.string.state_not_selected
                    },
                ),
            testTag = TestTags.categoryChip(0),
        )
        state.categories.forEach { category ->
            val selected = state.filters.categoryId == category.id
            CategoryChip(
                label = categoryLabel(category),
                selected = selected,
                onClick = { onCategoryFilter(category.id) },
                stateDescription =
                    stringResource(
                        if (selected) CoreR.string.state_selected else CoreR.string.state_not_selected,
                    ),
                testTag = TestTags.categoryChip(category.id),
            )
        }
        LogRange.entries.forEach { range ->
            val selected = state.filters.range == range
            CategoryChip(
                label = stringResource(rangeLabel(range)),
                selected = selected,
                onClick = { onRangeFilter(range) },
                stateDescription =
                    stringResource(
                        if (selected) CoreR.string.state_selected else CoreR.string.state_not_selected,
                    ),
                testTag = "log_range_${range.name}",
            )
        }
    }
}

@Composable
private fun MonthHeader(
    month: LogMonth,
    formatter: MoneyFormatter,
) {
    val locale = LocalConfiguration.current.locales[0]
    Text(
        text =
            stringResource(
                R.string.log_month_total,
                formatter.format(month.total),
                month.month.month.getDisplayName(JavaTextStyle.FULL, locale),
            ),
        style = Orbit7Theme.typography.label,
        color = Orbit7Theme.colors.onSurfaceMuted,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun DayHeader(
    day: LogDay,
    formatter: MoneyFormatter,
) {
    val locale = LocalConfiguration.current.locales[0]
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = formatter.formatShortDate(day.date, locale),
            style = Orbit7Theme.typography.label,
            color = Orbit7Theme.colors.onSurfaceMuted,
        )
        Text(
            text = formatter.formatPlain(day.total),
            style = Orbit7Theme.typography.label,
            color = Orbit7Theme.colors.onSurface,
            softWrap = false,
        )
    }
}

@Composable
private fun LogEntryRow(
    expense: Expense,
    categoryName: String,
    formatter: MoneyFormatter,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val amount = formatter.format(expense.amount)
    Panel(testTag = TestTags.logEntry(expense.id)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .semantics {
                        contentDescription = "$categoryName, $amount"
                    },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(categoryName, style = Orbit7Theme.typography.body, color = Orbit7Theme.colors.onSurface)
                expense.note?.let {
                    Text(it, style = Orbit7Theme.typography.bodyS, color = Orbit7Theme.colors.onSurfaceMuted)
                }
            }
            Text(
                text = amount,
                style = Orbit7Theme.typography.readoutM,
                color = Orbit7Theme.colors.onSurface,
                softWrap = false,
            )
            TextButton(onClick = onDelete) {
                Text(stringResource(CoreR.string.action_delete), style = Orbit7Theme.typography.bodyS)
            }
        }
    }
}

@Composable
internal fun EmptyState(
    title: String,
    body: String?,
    actionLabel: String?,
    onAction: () -> Unit,
    actionTag: String?,
) {
    Panel(testTag = TestTags.EMPTY_STATE) {
        Text(title, style = Orbit7Theme.typography.titleM, color = Orbit7Theme.colors.onSurface)
        if (body != null) {
            Text(body, style = Orbit7Theme.typography.body, color = Orbit7Theme.colors.onSurfaceMuted)
        }
        if (actionLabel != null) {
            TextButton(
                onClick = onAction,
                modifier = if (actionTag != null) Modifier.testTag(actionTag) else Modifier,
            ) {
                Text(actionLabel)
            }
        }
    }
}

private fun rangeLabel(range: LogRange): Int =
    when (range) {
        LogRange.THIS_MONTH -> R.string.log_range_this_month
        LogRange.LAST_30_DAYS -> R.string.log_range_last_30
        LogRange.EVERYTHING -> R.string.log_range_all
    }
