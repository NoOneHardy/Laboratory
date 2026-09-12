package ch.no1hardy.orbit7.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme

/**
 * The base object of the design system: a panel-filled rectangle with a bezel border, a 1dp inner
 * highlight along the top edge and a soft inner shadow below it — a recessed screen in a machined
 * faceplate (`docs/04-design-system.md` §3).
 *
 * @param caption an uppercase monospace label rendered on the panel's top edge. Captions may
 * truncate; numbers never may.
 */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    caption: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    testTag: String = TestTags.PANEL,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Orbit7Theme.colors
    val shapes = Orbit7Theme.shapes

    Column(
        modifier =
            modifier
                .testTag(testTag)
                .fillMaxWidth()
                .background(colors.surfaceRaised, shapes.panel)
                .border(shapes.bezelWidth, colors.surfaceEdge, shapes.panel)
                .drawBehind {
                    // The inner highlight: a single lit pixel row where the faceplate catches the light.
                    drawLine(
                        brush =
                            Brush.horizontalGradient(
                                listOf(colors.surfaceEdge.copy(alpha = 0f), colors.onSurfaceMuted.copy(alpha = 0.25f)),
                            ),
                        start = Offset(0f, 1f),
                        end = Offset(size.width, 1f),
                        strokeWidth = 1f,
                    )
                }.padding(shapes.panelPadding),
        verticalArrangement = Arrangement.spacedBy(shapes.gridUnit * 2),
    ) {
        if (caption != null || trailing != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (caption != null) {
                    androidx.compose.material3.Text(
                        text = caption.uppercase(),
                        style = Orbit7Theme.typography.label,
                        color = colors.onSurfaceMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
                trailing?.invoke()
            }
        }
        content()
    }
}

/** A darker inset area inside a panel, where numbers live. */
@Composable
fun ReadoutWindow(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Orbit7Theme.colors
    Column(
        modifier =
            modifier
                .testTag(TestTags.READOUT_WINDOW)
                .fillMaxWidth()
                .background(colors.surface, Orbit7Theme.shapes.readoutWindow)
                .border(1.dp, colors.surfaceEdge, Orbit7Theme.shapes.readoutWindow)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        content = content,
    )
}
