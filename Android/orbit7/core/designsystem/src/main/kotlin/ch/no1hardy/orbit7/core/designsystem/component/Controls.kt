package ch.no1hardy.orbit7.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme

/**
 * A physical switch with a throw, not a Material pill (`docs/04-design-system.md` §3).
 *
 * The haptic uses the standard Android feedback constants, so it follows the system setting.
 */
@Composable
fun SwitchToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    stateOn: String,
    stateOff: String,
    modifier: Modifier = Modifier,
    testTag: String = TestTags.SWITCH_TOGGLE,
    enabled: Boolean = true,
) {
    val colors = Orbit7Theme.colors
    val motion = Orbit7Theme.motion
    val haptics = LocalHapticFeedback.current
    val knobOffset by animateDpAsState(
        targetValue = if (checked) TOGGLE_THROW else 0.dp,
        animationSpec = tween(motion.toggle, easing = motion.mechanicalEasing),
        label = "toggle",
    )

    Row(
        modifier =
            modifier
                .testTag(testTag)
                .fillMaxWidth()
                .defaultMinSize(minHeight = Orbit7Theme.shapes.minimumTouchTarget)
                .clickable(enabled = enabled) {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onCheckedChange(!checked)
                }.semantics {
                    role = Role.Switch
                    contentDescription = label
                    stateDescription = if (checked) stateOn else stateOff
                },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = Orbit7Theme.typography.body,
            color = if (enabled) colors.onSurface else colors.onSurfaceMuted,
        )
        Box(
            modifier =
                Modifier
                    .width(TOGGLE_WIDTH)
                    .height(TOGGLE_HEIGHT)
                    .background(colors.surface, Orbit7Theme.shapes.chip)
                    .border(1.dp, colors.surfaceEdge, Orbit7Theme.shapes.chip),
        ) {
            Box(
                modifier =
                    Modifier
                        .offset(x = knobOffset)
                        .padding(2.dp)
                        .size(KNOB_SIZE)
                        .background(
                            if (checked) colors.energy else colors.onSurfaceMuted,
                            Orbit7Theme.shapes.chip,
                        ),
            )
        }
    }
}

/** A chunky keycap whose pressed state actually sinks. */
@Composable
fun NumpadKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = TestTags.numpadKey(label),
    contentDescription: String = label,
) {
    val colors = Orbit7Theme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    val sink by animateDpAsState(
        targetValue = if (pressed) 2.dp else 0.dp,
        animationSpec = tween(Orbit7Theme.motion.toggle),
        label = "key",
    )

    Box(
        modifier =
            modifier
                .testTag(testTag)
                .defaultMinSize(
                    minWidth = Orbit7Theme.shapes.minimumTouchTarget,
                    minHeight = Orbit7Theme.shapes.minimumTouchTarget,
                ).offset(y = sink)
                .background(colors.surfaceRaised, Orbit7Theme.shapes.key)
                .border(1.dp, colors.surfaceEdge, Orbit7Theme.shapes.key)
                .clickable(interactionSource = interactionSource, indication = null) {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onClick()
                }.semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = Orbit7Theme.typography.readoutL, color = colors.onSurface)
    }
}

/** A category chip. Selection is signalled by fill *and* by a leading marker, never by colour alone. */
@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    stateDescription: String,
    modifier: Modifier = Modifier,
    testTag: String,
) {
    val colors = Orbit7Theme.colors
    Row(
        modifier =
            modifier
                .testTag(testTag)
                .defaultMinSize(minHeight = Orbit7Theme.shapes.minimumTouchTarget)
                .background(if (selected) colors.surfaceEdge else colors.surfaceRaised, Orbit7Theme.shapes.chip)
                .border(
                    1.dp,
                    if (selected) colors.accent else colors.surfaceEdge,
                    Orbit7Theme.shapes.chip,
                ).clickable(onClick = onClick)
                .padding(horizontal = 12.dp)
                .semantics {
                    role = Role.Tab
                    this.stateDescription = stateDescription
                },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (selected) {
            Box(
                modifier =
                    Modifier
                        .size(6.dp)
                        .background(colors.accent, Orbit7Theme.shapes.chip),
            )
        }
        Text(
            text = label,
            style = Orbit7Theme.typography.body,
            color = if (selected) colors.onSurface else colors.onSurfaceMuted,
        )
    }
}

/** Thin connector lines implying wiring. Purely decorative, and among the first things removed. */
@Composable
fun BusLine(modifier: Modifier = Modifier) {
    val colors = Orbit7Theme.colors
    if (!Orbit7Theme.motion.ambientEnabled) return
    Box(
        modifier =
            modifier
                .testTag(TestTags.BUS_LINE)
                .fillMaxWidth()
                .height(BUS_LINE_HEIGHT)
                .drawBehind {
                    drawLine(
                        color = colors.surfaceEdge,
                        start =
                            androidx.compose.ui.geometry
                                .Offset(0f, size.height / 2),
                        end =
                            androidx.compose.ui.geometry
                                .Offset(size.width, size.height / 2),
                        strokeWidth = 2f,
                    )
                }.semantics { },
    )
}

private val TOGGLE_WIDTH = 52.dp
private val TOGGLE_HEIGHT = 28.dp
private val TOGGLE_THROW = 24.dp
private val KNOB_SIZE = 24.dp
private val BUS_LINE_HEIGHT = 8.dp
