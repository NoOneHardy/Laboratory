package ch.no1hardy.orbit7.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme

/**
 * One module on the Station screen.
 *
 * States are distinguished by more than colour: an affordable module is outlined and shows its
 * cost, an unaffordable one states the shortfall, a locked one names its prerequisite, and a maxed
 * one says so in words.
 */
@Composable
fun ModuleTile(
    name: String,
    levelLabel: String,
    statusLabel: String,
    contentDescription: String,
    affordable: Boolean,
    locked: Boolean,
    lit: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val colors = Orbit7Theme.colors
    val borderColor =
        when {
            locked -> colors.surfaceEdge
            affordable -> colors.energy
            else -> colors.surfaceEdge
        }

    Column(
        modifier =
            modifier
                .testTag(testTag)
                .fillMaxWidth()
                .background(colors.surfaceRaised, Orbit7Theme.shapes.panel)
                .border(Orbit7Theme.shapes.bezelWidth, borderColor, Orbit7Theme.shapes.panel)
                .clickable(onClick = onClick)
                .padding(12.dp)
                .semantics { this.contentDescription = contentDescription },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = name,
                style = Orbit7Theme.typography.titleM,
                color = if (lit) colors.onSurface else colors.onSurfaceMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            LevelPips(levelLabel = levelLabel, lit = lit)
        }
        Text(
            text = statusLabel,
            style = Orbit7Theme.typography.readoutM,
            color = if (affordable) colors.energy else colors.onSurfaceMuted,
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Visible,
        )
    }
}

@Composable
private fun LevelPips(
    levelLabel: String,
    lit: Boolean,
) {
    val colors = Orbit7Theme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = levelLabel,
            style = Orbit7Theme.typography.label,
            color = if (lit) colors.accent else colors.onSurfaceMuted,
        )
        androidx.compose.foundation.layout.Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .height(8.dp)
                    .background(if (lit) colors.accent else colors.surfaceEdge, Orbit7Theme.shapes.chip),
        )
    }
}
