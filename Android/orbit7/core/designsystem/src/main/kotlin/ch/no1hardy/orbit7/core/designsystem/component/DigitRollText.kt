package ch.no1hardy.orbit7.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme

/**
 * A split-flap readout: each character flips into place, staggered 30ms left to right
 * (`docs/04-design-system.md` §4).
 *
 * With `reduceEffects` on, the duration collapses to zero and the value simply appears — the number
 * is never withheld for the sake of an animation.
 *
 * The whole readout carries one content description (`contentDescription`), because a screen reader
 * announcing sixteen individual digits is useless. **Numbers never truncate**: `softWrap` is off and
 * overflow is visible, so a layout that cannot fit an amount is a visible bug rather than a silent
 * lie.
 */
@Composable
fun DigitRollText(
    text: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    style: TextStyle = Orbit7Theme.typography.readoutL,
    color: Color = Orbit7Theme.colors.accent,
) {
    val motion = Orbit7Theme.motion

    Row(
        modifier =
            modifier
                .testTag(TestTags.DIGIT_ROLL)
                .semantics { this.contentDescription = contentDescription },
    ) {
        text.forEachIndexed { index, character ->
            AnimatedContent(
                targetState = character,
                transitionSpec = {
                    val duration = motion.digitRoll
                    val delay = motion.digitStagger * index
                    (
                        slideInVertically(
                            tween(duration, delay, motion.mechanicalEasing),
                        ) { height -> -height } togetherWith
                            slideOutVertically(tween(duration, delay, motion.mechanicalEasing)) { height -> height }
                    )
                },
                label = "digit",
            ) { value ->
                Text(
                    text = value.toString(),
                    style = style,
                    color = color,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }
    }
}
