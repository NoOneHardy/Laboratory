package ch.no1hardy.orbit7.feature.expenses

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.categoryNameRes
import ch.no1hardy.orbit7.core.designsystem.component.CategoryChip
import ch.no1hardy.orbit7.core.designsystem.component.DigitRollText
import ch.no1hardy.orbit7.core.designsystem.component.NumpadKey
import ch.no1hardy.orbit7.core.designsystem.component.Panel
import ch.no1hardy.orbit7.core.designsystem.component.ReadoutWindow
import ch.no1hardy.orbit7.core.designsystem.format.MoneyFormatter
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme
import ch.no1hardy.orbit7.core.domain.model.Category
import kotlinx.coroutines.flow.collectLatest
import ch.no1hardy.orbit7.core.designsystem.R as CoreR

/**
 * The stateful route: it collects the ViewModel and forwards one-shot effects
 * (`docs/05-architecture.md` §3). Screenshot and behaviour tests drive [QuickAddScreen] instead,
 * which is what makes UI testing here cheap.
 */
@Composable
fun QuickAddRoute(
    onSaved: (expenseId: Long, isNew: Boolean) -> Unit,
    onDismiss: () -> Unit,
    viewModel: QuickAddViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is QuickAddEffect.Saved -> onSaved(effect.expenseId, effect.isNew)
                QuickAddEffect.Dismissed -> onDismiss()
            }
        }
    }

    QuickAddScreen(
        state = state,
        onDigit = viewModel::onDigit,
        onBackspace = viewModel::onBackspace,
        onClear = viewModel::onClear,
        onCategorySelected = viewModel::onCategorySelected,
        onDateStep = viewModel::onDateStep,
        onNoteChanged = viewModel::onNoteChanged,
        onNoteExpanded = viewModel::onNoteExpanded,
        onSave = viewModel::onSave,
        onDismiss = viewModel::onDismiss,
    )
}

/**
 * Three taps from FAB to saved: amount, category, save.
 *
 * The numpad is custom rather than the system keyboard — it is faster, it keeps Save under the
 * thumb, and the keycaps are a design showpiece.
 */
@Composable
fun QuickAddScreen(
    state: QuickAddUiState,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onCategorySelected: (Long) -> Unit,
    onDateStep: (Long) -> Unit,
    onNoteChanged: (String) -> Unit,
    onNoteExpanded: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    formatter: MoneyFormatter = MoneyFormatter(),
) {
    Column(
        modifier =
            Modifier
                .testTag(TestTags.QUICK_ADD)
                .fillMaxSize()
                .padding(Orbit7Theme.shapes.screenGutter),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Panel(caption = stringResource(if (state.isEditing) R.string.quick_add_edit else R.string.quick_add_title)) {
            ReadoutWindow {
                DigitRollText(
                    text = formatter.format(state.amount),
                    contentDescription =
                        stringResource(
                            R.string.quick_add_amount_description,
                            formatter.format(state.amount),
                        ),
                    style = Orbit7Theme.typography.readoutXL,
                    modifier =
                        Modifier
                            .testTag(TestTags.AMOUNT_READOUT)
                            .fillMaxWidth(),
                )
            }
            if (state.showsAmountRequired) {
                Text(
                    text = stringResource(R.string.quick_add_amount_required),
                    style = Orbit7Theme.typography.bodyS,
                    color = Orbit7Theme.colors.onSurfaceMuted,
                    modifier = Modifier.testTag(TestTags.VALIDATION_MESSAGE),
                )
            }
        }

        CategoryChips(
            categories = state.categories,
            selectedId = state.selectedCategoryId,
            onSelected = onCategorySelected,
        )

        DateStepper(state = state, onDateStep = onDateStep, formatter = formatter)

        if (state.noteExpanded) {
            BasicTextField(
                value = state.note,
                onValueChange = onNoteChanged,
                textStyle = Orbit7Theme.typography.body.copy(color = Orbit7Theme.colors.onSurface),
                modifier =
                    Modifier
                        .testTag(TestTags.NOTE_FIELD)
                        .fillMaxWidth(),
            )
        } else {
            TextButton(onClick = onNoteExpanded) {
                Text(stringResource(R.string.quick_add_add_note), style = Orbit7Theme.typography.bodyS)
            }
        }

        Spacer(Modifier.weight(1f))

        Numpad(onDigit = onDigit, onBackspace = onBackspace, onClear = onClear)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text(stringResource(CoreR.string.action_cancel))
            }
            Button(
                onClick = onSave,
                enabled = state.canSave,
                modifier =
                    Modifier
                        .testTag(TestTags.SAVE)
                        .weight(2f),
            ) {
                Text(
                    stringResource(if (state.isEditing) CoreR.string.action_save_changes else CoreR.string.action_save),
                )
            }
        }
    }
}

@Composable
private fun CategoryChips(
    categories: List<Category>,
    selectedId: Long?,
    onSelected: (Long) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .testTag(TestTags.CATEGORY_CHIPS)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            val selected = category.id == selectedId
            CategoryChip(
                label = categoryLabel(category),
                selected = selected,
                onClick = { onSelected(category.id) },
                stateDescription =
                    stringResource(
                        if (selected) CoreR.string.state_selected else CoreR.string.state_not_selected,
                    ),
                testTag = TestTags.categoryChip(category.id),
            )
        }
    }
}

@Composable
private fun DateStepper(
    state: QuickAddUiState,
    onDateStep: (Long) -> Unit,
    formatter: MoneyFormatter,
) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    Row(
        modifier =
            Modifier
                .testTag(TestTags.DATE_STEPPER)
                .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { onDateStep(-1) }) {
            Text(stringResource(R.string.quick_add_previous_day))
        }
        Text(
            text = formatter.formatShortDate(state.date, locale),
            style = Orbit7Theme.typography.readoutM,
            color = Orbit7Theme.colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { onDateStep(1) }) {
            Text(stringResource(R.string.quick_add_next_day))
        }
    }
}

@Composable
private fun Numpad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
) {
    val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))
    Column(
        modifier =
            Modifier
                .testTag(TestTags.NUMPAD)
                .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { digit ->
                    NumpadKey(
                        label = digit.toString(),
                        onClick = { onDigit(digit) },
                        modifier = Modifier.weight(1f),
                        contentDescription = stringResource(R.string.numpad_digit, digit),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NumpadKey(
                label = stringResource(R.string.numpad_clear),
                onClick = onClear,
                modifier = Modifier.weight(1f),
                testTag = TestTags.numpadKey("clear"),
                contentDescription = stringResource(R.string.numpad_clear_description),
            )
            NumpadKey(
                label = "0",
                onClick = { onDigit(0) },
                modifier = Modifier.weight(1f),
                contentDescription = stringResource(R.string.numpad_digit, 0),
            )
            NumpadKey(
                label = stringResource(R.string.numpad_backspace),
                onClick = onBackspace,
                modifier = Modifier.weight(1f),
                testTag = TestTags.numpadKey("backspace"),
                contentDescription = stringResource(R.string.numpad_backspace_description),
            )
        }
    }
}

/** Seeded categories are localized by key; user-created ones keep the name the user typed. */
@Composable
internal fun categoryLabel(category: Category): String =
    category.customName ?: category.key?.let { stringResource(categoryNameRes(it)) } ?: ""

@Preview
@Composable
private fun QuickAddPreview() {
    Orbit7Theme {
        QuickAddScreen(
            state = QuickAddUiState(amountMinor = 1_250, loading = false),
            onDigit = {},
            onBackspace = {},
            onClear = {},
            onCategorySelected = {},
            onDateStep = {},
            onNoteChanged = {},
            onNoteExpanded = {},
            onSave = {},
            onDismiss = {},
        )
    }
}
