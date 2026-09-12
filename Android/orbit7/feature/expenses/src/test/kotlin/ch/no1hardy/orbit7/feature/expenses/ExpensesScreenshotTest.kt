package ch.no1hardy.orbit7.feature.expenses

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme
import ch.no1hardy.orbit7.core.domain.money.sum
import ch.no1hardy.orbit7.core.domain.test.aCategory
import ch.no1hardy.orbit7.core.domain.test.anExpense
import ch.no1hardy.orbit7.core.domain.test.chf
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ExpensesScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6.copy(locale = "de-CH"))

    private val today = LocalDate.of(2025, 4, 16)
    private val categories = listOf(aCategory(id = 1), aCategory(id = 2, sortOrder = 1))

    private val quickAddState =
        QuickAddUiState(
            amountMinor = 1_250,
            categories = categories,
            selectedCategoryId = 1,
            date = today,
            loading = false,
        )

    private val logState =
        LogUiState(
            months =
                listOf(
                    LogMonth(
                        month = YearMonth.of(2025, 4),
                        total = chf(42, 60),
                        days =
                            listOf(
                                LogDay(
                                    date = today,
                                    entries =
                                        listOf(
                                            anExpense(chf(12, 50), today),
                                            anExpense(chf(8, 10), today, id = 2),
                                        ),
                                    total = listOf(chf(12, 50), chf(8, 10)).sum(),
                                ),
                                LogDay(
                                    date = today.minusDays(1),
                                    entries =
                                        listOf(
                                            anExpense(chf(22), today.minusDays(1), id = 3, note = "Wocheneinkauf"),
                                        ),
                                    total = chf(22),
                                ),
                            ),
                    ),
                ),
            categories = categories,
            loading = false,
        )

    @Test
    fun `quick add`() =
        snapshotAll("quick_add") {
            QuickAddScreen(
                state = quickAddState,
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

    @Test
    fun `quick add with nothing entered`() {
        paparazzi.snapshot(name = "quick_add_empty") {
            Orbit7Theme {
                QuickAddScreen(
                    state = quickAddState.copy(amountMinor = 0),
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
    }

    @Test
    fun log() =
        snapshotAll("log") {
            LogScreen(
                state = logState,
                onEditExpense = {},
                onDelete = {},
                onCategoryFilter = {},
                onRangeFilter = {},
                onClearFilters = {},
            )
        }

    @Test
    fun `log when empty`() {
        paparazzi.snapshot(name = "log_empty") {
            Orbit7Theme {
                LogScreen(
                    state = LogUiState(loading = false),
                    onEditExpense = {},
                    onDelete = {},
                    onCategoryFilter = {},
                    onRangeFilter = {},
                    onClearFilters = {},
                )
            }
        }
    }

    private fun snapshotAll(
        name: String,
        content: @Composable () -> Unit,
    ) {
        paparazzi.snapshot(name = "${name}_default") { Orbit7Theme(reduceEffects = false) { content() } }

        paparazzi.unsafeUpdateConfig(
            deviceConfig = DeviceConfig.PIXEL_6.copy(locale = "de-CH", fontScale = 2.0f),
        )
        paparazzi.snapshot(name = "${name}_font_scale_200") { Orbit7Theme(reduceEffects = false) { content() } }

        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_6.copy(locale = "de-CH"))
        paparazzi.snapshot(name = "${name}_reduce_effects") { Orbit7Theme(reduceEffects = true) { content() } }
    }
}
