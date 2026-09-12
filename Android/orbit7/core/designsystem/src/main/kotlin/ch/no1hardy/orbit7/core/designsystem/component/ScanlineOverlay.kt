package ch.no1hardy.orbit7.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme

/**
 * The slow scanline drift and the barely-visible vignette flicker.
 *
 * Constraints from `docs/04-design-system.md` §4 and §6: ambient motion runs only while the screen
 * is in the foreground, it is purely decorative (and therefore hidden from accessibility services),
 * and `reduceEffects` removes it entirely — a flickering overlay is a genuine photosensitivity risk,
 * which is why the system's own reduce-motion setting switches it off too.
 */
@Composable
fun ScanlineOverlay(modifier: Modifier = Modifier) {
    val motion = Orbit7Theme.motion
    if (!motion.ambientEnabled) return

    val colors = Orbit7Theme.colors
    val transition = rememberInfiniteTransition(label = "scanline")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(motion.scanlineLoop, easing = motion.linear),
                repeatMode = RepeatMode.Restart,
            ),
        label = "drift",
    )

    Canvas(
        modifier =
            modifier
                .testTag(TestTags.SCANLINE_OVERLAY)
                .fillMaxSize()
                .clearAndSetSemantics { },
    ) {
        val spacing = SCANLINE_SPACING
        var y = (offset * spacing) - spacing
        while (y < size.height) {
            drawLine(
                color = colors.scanline,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            y += spacing
        }
    }
}

private const val SCANLINE_SPACING = 4f
