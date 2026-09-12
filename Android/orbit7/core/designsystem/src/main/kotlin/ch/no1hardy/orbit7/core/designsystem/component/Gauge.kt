package ch.no1hardy.orbit7.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme

/**
 * The reactor-headroom bar: how much of the week's budget is left, with a marker for the day of the
 * week (`docs/03-screens.md` §1).
 *
 * Colour is never the only signal — an overdrawn gauge is also hazard-striped, and the caller always
 * supplies a spoken [contentDescription] phrased as values rather than as a picture.
 */
@Composable
fun Gauge(
    progress: Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
    dayMarker: Float? = null,
    overdrawn: Boolean = false,
) {
    val colors = Orbit7Theme.colors
    val motion = Orbit7Theme.motion
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(motion.savePulse, easing = motion.standardEasing),
        label = "gauge",
    )

    Box(
        modifier =
            modifier
                .testTag(TestTags.GAUGE)
                .fillMaxWidth()
                .height(GAUGE_HEIGHT)
                .semantics { this.contentDescription = contentDescription },
    ) {
        Canvas(Modifier.fillMaxWidth().height(GAUGE_HEIGHT)) {
            drawRect(color = colors.gaugeTrack, size = size)

            val fillWidth = size.width * animated
            if (overdrawn) {
                drawHazardStripes(colors.hazardStripe, colors.surface)
            } else {
                drawRect(color = colors.gaugeFill, size = Size(fillWidth, size.height))
            }

            dayMarker?.let { marker ->
                val x = size.width * marker.coerceIn(0f, 1f)
                drawLine(
                    color = colors.onSurface,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = MARKER_WIDTH,
                )
            }
        }
    }
}

/**
 * 45° amber/void diagonals, reserved for genuine urgency: an overdrawn week, a deadline inside 14
 * days, a destructive confirmation. Scarcity is what makes them work.
 */
internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHazardStripes(
    stripe: Color,
    background: Color,
) {
    drawRect(color = background, size = size)
    val step = STRIPE_WIDTH * 2
    var x = -size.height
    while (x < size.width + size.height) {
        drawLine(
            color = stripe,
            start = Offset(x, size.height),
            end = Offset(x + size.height, 0f),
            strokeWidth = STRIPE_WIDTH,
        )
        x += step
    }
}

private val GAUGE_HEIGHT = 12.dp
private const val MARKER_WIDTH = 2f
private const val STRIPE_WIDTH = 8f
