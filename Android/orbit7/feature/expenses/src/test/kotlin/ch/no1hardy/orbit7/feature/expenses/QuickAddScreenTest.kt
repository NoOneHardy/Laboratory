package ch.no1hardy.orbit7.feature.expenses

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme
import ch.no1hardy.orbit7.core.domain.test.aCategory
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose behaviour tests, driven against the stateless screen
 * (`docs/06-test-strategy.md` §6).
 *
 * Assertions use the `testTag` constants — never user-visible text, which is localized and would
 * make the suite break on a translation change.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "de-rCH-w411dp-h891dp")
class QuickAddScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val state =
        QuickAddUiState(
            categories = listOf(aCategory(id = 1), aCategory(id = 2, sortOrder = 1)),
            selectedCategoryId = 1,
            loading = false,
        )

    @Test
    fun `the empty state disables save and shows why`() {
        setContent(state)

        composeRule.onNodeWithTag(TestTags.QUICK_ADD).assertExists()
        composeRule.onNodeWithTag(TestTags.SAVE).assertIsNotEnabled()
        composeRule.onNodeWithTag(TestTags.VALIDATION_MESSAGE).assertExists()
    }

    @Test
    fun `save really is three interactions away`() {
        var savedCalls = 0
        val digits = mutableListOf<Int>()
        var selectedCategory: Long? = null

        setContent(
            state = state.copy(amountMinor = 1_250),
            onDigit = { digits += it },
            onCategorySelected = { selectedCategory = it },
            onSave = { savedCalls++ },
        )

        composeRule.onNodeWithTag(TestTags.numpadKey("5")).performClick()
        composeRule.onNodeWithTag(TestTags.categoryChip(2)).performClick()
        composeRule.onNodeWithTag(TestTags.SAVE).assertIsEnabled().performClick()

        digits shouldBe listOf(5)
        selectedCategory shouldBe 2L
        savedCalls shouldBe 1
    }

    @Test
    fun `the numpad exposes every key with a content description`() {
        setContent(state)

        (0..9).forEach { digit ->
            composeRule.onNodeWithTag(TestTags.numpadKey(digit.toString())).assertExists()
        }
        composeRule.onNodeWithTag(TestTags.numpadKey("clear")).assertExists()
        composeRule.onNodeWithTag(TestTags.numpadKey("backspace")).assertExists()
    }

    private fun setContent(
        state: QuickAddUiState,
        onDigit: (Int) -> Unit = {},
        onCategorySelected: (Long) -> Unit = {},
        onSave: () -> Unit = {},
    ) {
        composeRule.setContent {
            Orbit7Theme {
                QuickAddScreen(
                    state = state,
                    onDigit = onDigit,
                    onBackspace = {},
                    onClear = {},
                    onCategorySelected = onCategorySelected,
                    onDateStep = {},
                    onNoteChanged = {},
                    onNoteExpanded = {},
                    onSave = onSave,
                    onDismiss = {},
                )
            }
        }
    }
}
