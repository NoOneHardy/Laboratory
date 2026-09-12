package ch.no1hardy.orbit7.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Hardware, not app cards (`docs/04-design-system.md` §3).
 *
 * A 4dp radius, a 2dp bezel border and an inner highlight along the top edge read as a recessed
 * screen in a machined faceplate. Elevation is expressed through bezel and inset shadow — never
 * Material tonal elevation.
 */
@Immutable
data class Orbit7Shapes(
    val panel: Shape = RoundedCornerShape(4.dp),
    val readoutWindow: Shape = RoundedCornerShape(2.dp),
    val chip: Shape = RoundedCornerShape(2.dp),
    val key: Shape = RoundedCornerShape(3.dp),
    val bezelWidth: Dp = 2.dp,
    val panelPadding: Dp = 16.dp,
    val screenGutter: Dp = 16.dp,
    val gridUnit: Dp = 4.dp,
    /** Nothing interactive is ever smaller than this, anywhere, including numpad keys and chips. */
    val minimumTouchTarget: Dp = 48.dp,
)
