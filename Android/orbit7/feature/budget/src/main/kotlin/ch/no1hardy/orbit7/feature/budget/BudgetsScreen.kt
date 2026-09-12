package ch.no1hardy.orbit7.feature.budget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.categoryNameRes
import ch.no1hardy.orbit7.core.designsystem.component.CategoryChip
import ch.no1hardy.orbit7.core.designsystem.component.Panel
import ch.no1hardy.orbit7.core.designsystem.format.MoneyFormatter
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.usecase.CategoryBudget
import ch.no1hardy.orbit7.core.designsystem.R as CoreR

@Composable
fun BudgetsRoute(viewModel: BudgetsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BudgetsScreen(
        state = state,
        onPeriodChanged = viewModel::onPeriodChanged,
        onAcceptSuggestion = viewModel::onAcceptSuggestion,
        onDismissSuggestion = viewModel::onDismissSuggestion,
        onEdit = viewModel::onEdit,
        onBudgetChanged = viewModel::onBudgetChanged,
    )
}

/**
 * Budgets (`docs/03-screens.md` §4): one row per category with its budget, spend so far, the
 * baseline ghost marker and a 12-week sparkline, plus the baseline suggestion card.
 */
@Composable
fun BudgetsScreen(
    state: BudgetsUiState,
    onPeriodChanged: (BudgetPeriod) -> Unit,
    onAcceptSuggestion: (Long, Money) -> Unit,
    onDismissSuggestion: (Long) -> Unit,
    onEdit: (Long?) -> Unit,
    onBudgetChanged: (Long, Money) -> Unit,
    formatter: MoneyFormatter = MoneyFormatter(),
) {
    Column(
        modifier =
            Modifier
                .testTag(TestTags.BUDGETS)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Orbit7Theme.shapes.screenGutter),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .testTag(TestTags.BUDGET_PERIOD_TOGGLE)
                    .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BudgetPeriod.entries.forEach { period ->
                val selected = state.period == period
                CategoryChip(
                    label =
                        stringResource(
                            if (period ==
                                BudgetPeriod.MONTH
                            ) {
                                R.string.budget_period_month
                            } else {
                                R.string.budget_period_week
                            },
                        ),
                    selected = selected,
                    onClick = { onPeriodChanged(period) },
                    stateDescription =
                        stringResource(
                            if (selected) CoreR.string.state_selected else CoreR.string.state_not_selected,
                        ),
                    testTag = "budget_period_${period.name}",
                )
            }
        }

        if (!state.anyBaselines && !state.loading) {
            Panel(caption = stringResource(R.string.budget_baseline_caption)) {
                Text(
                    text = stringResource(R.string.budget_baseline_pending),
                    style = Orbit7Theme.typography.bodyS,
                    color = Orbit7Theme.colors.onSurfaceMuted,
                )
            }
        }

        state.rows.forEach { row ->
            BudgetRow(
                row = row,
                period = state.period,
                formatter = formatter,
                onEdit = { onEdit(row.category.id) },
                onAcceptSuggestion = onAcceptSuggestion,
                onDismissSuggestion = onDismissSuggestion,
            )
        }
    }
}

@Composable
private fun BudgetRow(
    row: CategoryBudget,
    period: BudgetPeriod,
    formatter: MoneyFormatter,
    onEdit: () -> Unit,
    onAcceptSuggestion: (Long, Money) -> Unit,
    onDismissSuggestion: (Long) -> Unit,
) {
    val budget = if (period == BudgetPeriod.MONTH) row.monthlyBudget else row.weeklyBudget
    val spent = if (period == BudgetPeriod.MONTH) row.spentThisMonth else row.spentThisWeek
    val name =
        row.category.customName
            ?: row.category.key?.let { stringResource(categoryNameRes(it)) }
            ?: ""

    Panel(caption = name, testTag = TestTags.budgetRow(row.category.id)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEdit),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatter.format(spent),
                style = Orbit7Theme.typography.readoutM,
                color = Orbit7Theme.colors.onSurface,
                softWrap = false,
            )
            Text(
                text = formatter.format(budget),
                style = Orbit7Theme.typography.readoutM,
                color = Orbit7Theme.colors.onSurfaceMuted,
                softWrap = false,
            )
        }

        Sparkline(values = row.sparkline, baseline = row.weeklyBaseline)

        row.weeklyBaseline?.let { baseline ->
            Text(
                text = stringResource(R.string.budget_baseline_value, formatter.format(baseline)),
                style = Orbit7Theme.typography.bodyS,
                color = Orbit7Theme.colors.onSurfaceMuted,
            )
        }

        row.suggestion?.let { suggestion ->
            Panel(
                caption = stringResource(R.string.budget_suggestion_caption),
                testTag = TestTags.baselineSuggestion(row.category.id),
            ) {
                Text(
                    text =
                        stringResource(
                            R.string.budget_suggestion_body,
                            formatter.format(suggestion.weeklyBaseline),
                            formatter.format(suggestion.weeklyTarget),
                        ),
                    style = Orbit7Theme.typography.body,
                    color = Orbit7Theme.colors.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { onAcceptSuggestion(row.category.id, suggestion.suggestedMonthlyBudget) },
                        modifier = Modifier.testTag(TestTags.suggestionAccept(row.category.id)),
                    ) {
                        Text(stringResource(R.string.budget_suggestion_accept))
                    }
                    TextButton(
                        onClick = { onDismissSuggestion(row.category.id) },
                        modifier = Modifier.testTag(TestTags.suggestionDismiss(row.category.id)),
                    ) {
                        Text(stringResource(CoreR.string.action_dismiss))
                    }
                }
            }
        }
    }
}

/** Twelve weeks of spend, with the baseline drawn as a ghost line across it. */
@Composable
private fun Sparkline(
    values: List<Money>,
    baseline: Money?,
) {
    if (values.isEmpty()) return
    val colors = Orbit7Theme.colors
    val maximum = maxOf(values.maxOf { it.minor }, baseline?.minor ?: 0L).coerceAtLeast(1L)

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(SPARKLINE_HEIGHT),
    ) {
        val step = size.width / values.size.coerceAtLeast(1)
        values.forEachIndexed { index, value ->
            val height = size.height * (value.minor.toFloat() / maximum.toFloat())
            drawRect(
                color = colors.accent,
                topLeft = Offset(index * step, size.height - height),
                size =
                    androidx.compose.ui.geometry
                        .Size(step * BAR_WIDTH_RATIO, height),
            )
        }
        baseline?.let {
            val y = size.height - size.height * (it.minor.toFloat() / maximum.toFloat())
            drawLine(
                color = colors.onSurfaceMuted,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2f,
            )
        }
    }
}

private val SPARKLINE_HEIGHT = 40.dp
private const val BAR_WIDTH_RATIO = 0.6f
