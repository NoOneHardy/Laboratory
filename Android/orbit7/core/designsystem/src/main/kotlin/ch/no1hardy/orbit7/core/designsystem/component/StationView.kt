package ch.no1hardy.orbit7.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme
import ch.no1hardy.orbit7.core.domain.model.StationModuleKey

/**
 * ORBIT-7 itself: a vector cross-section, drawn from the design tokens
 * (`docs/07-roadmap.md`, open question 4 — vector, so it is fully themeable, animatable, and scales
 * with the tokens rather than with a raster asset).
 *
 * Every module is drawn dark and becomes lit as its level rises, so the station *is* the progress
 * bar. The whole graphic carries one spoken description phrased as values ("Reactor Core, level 2
 * of 5…"), because a screen reader cannot see a picture and should not be told there is one.
 *
 * @param levels the player's level per module; absent keys are dark.
 * @param contentDescription the spoken state of the station, assembled by the caller from resources.
 */
@Composable
fun StationView(
    levels: Map<StationModuleKey, Int>,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = Orbit7Theme.colors
    val motion = Orbit7Theme.motion
    val reactorLevel = levels[StationModuleKey.REACTOR_CORE] ?: 0

    // Overall brightness steps with the reactor: each level makes the whole station visibly brighter.
    val brightness by animateFloatAsState(
        targetValue = BASE_BRIGHTNESS + reactorLevel * BRIGHTNESS_PER_LEVEL,
        animationSpec = tween(motion.powerUp.coerceAtLeast(1), easing = motion.standardEasing),
        label = "brightness",
    )
    val pulse by rememberInfiniteTransition(label = "reactor").animateFloat(
        initialValue = if (motion.ambientEnabled) 0.85f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse),
        label = "pulse",
    )

    Canvas(
        modifier =
            modifier
                .testTag(TestTags.STATION_VIEWPORT)
                .fillMaxWidth()
                .aspectRatio(ASPECT)
                .semantics { this.contentDescription = contentDescription },
    ) {
        val unit = size.minDimension / GRID
        val centre = Offset(size.width / 2, size.height / 2)

        drawSpine(colors.surfaceEdge, centre, unit, brightness)
        drawReactor(colors, centre, unit, reactorLevel, brightness * pulse)
        drawRing(
            colors = colors,
            centre = centre,
            unit = unit,
            level = levels[StationModuleKey.HYDROPONICS] ?: 0,
            radiusUnits = HYDROPONICS_RADIUS,
            litColor = colors.energy,
            brightness = brightness,
        )
        drawPod(
            colors,
            centre + Offset(-unit * 6, -unit * 3),
            unit,
            levels[StationModuleKey.LIFE_SUPPORT] ?: 0,
            brightness,
        )
        drawPod(
            colors,
            centre + Offset(unit * 6, -unit * 3),
            unit,
            levels[StationModuleKey.OBSERVATION_DECK] ?: 0,
            brightness,
        )
        drawPod(colors, centre + Offset(unit * 6, unit * 3), unit, levels[StationModuleKey.HANGAR] ?: 0, brightness)
        drawPod(colors, centre + Offset(-unit * 6, unit * 3), unit, levels[StationModuleKey.CRYO_LAB] ?: 0, brightness)
        drawAntenna(colors, centre + Offset(0f, -unit * 7), unit, levels[StationModuleKey.COMMS_ARRAY] ?: 0, brightness)
    }
}

private fun DrawScope.drawSpine(
    color: Color,
    centre: Offset,
    unit: Float,
    brightness: Float,
) {
    drawLine(
        color = color.copy(alpha = brightness),
        start = Offset(centre.x - unit * 7, centre.y),
        end = Offset(centre.x + unit * 7, centre.y),
        strokeWidth = unit * 0.6f,
    )
}

private fun DrawScope.drawReactor(
    colors: ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Colors,
    centre: Offset,
    unit: Float,
    level: Int,
    brightness: Float,
) {
    val radius = unit * 2.4f
    drawCircle(color = colors.surfaceEdge, radius = radius, center = centre)
    if (level > 0) {
        drawCircle(
            color = colors.accent.copy(alpha = brightness),
            radius = radius * (0.4f + level * 0.12f),
            center = centre,
        )
    }
    drawCircle(
        color = if (level > 0) colors.accent else colors.surfaceEdge,
        radius = radius,
        center = centre,
        style = Stroke(width = unit * 0.25f),
    )
}

private fun DrawScope.drawRing(
    colors: ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Colors,
    centre: Offset,
    unit: Float,
    level: Int,
    radiusUnits: Float,
    litColor: Color,
    brightness: Float,
) {
    drawCircle(
        color = if (level > 0) litColor.copy(alpha = brightness * (0.4f + level * 0.15f)) else colors.surfaceEdge,
        radius = unit * radiusUnits,
        center = centre,
        style = Stroke(width = unit * 0.2f),
    )
}

private fun DrawScope.drawPod(
    colors: ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Colors,
    position: Offset,
    unit: Float,
    level: Int,
    brightness: Float,
) {
    val podSize = Size(unit * 2.6f, unit * 1.8f)
    val topLeft = Offset(position.x - podSize.width / 2, position.y - podSize.height / 2)
    drawRect(color = colors.surfaceRaised, topLeft = topLeft, size = podSize)
    if (level > 0) {
        drawRect(
            color = colors.accent.copy(alpha = brightness * (0.35f + level * 0.15f)),
            topLeft = topLeft,
            size = podSize,
        )
    }
    drawRect(
        color = if (level > 0) colors.accent else colors.surfaceEdge,
        topLeft = topLeft,
        size = podSize,
        style = Stroke(width = unit * 0.15f),
    )
}

private fun DrawScope.drawAntenna(
    colors: ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Colors,
    position: Offset,
    unit: Float,
    level: Int,
    brightness: Float,
) {
    val color = if (level > 0) colors.energy.copy(alpha = brightness) else colors.surfaceEdge
    drawLine(color, start = position, end = position + Offset(0f, unit * 3), strokeWidth = unit * 0.2f)
    drawCircle(color = color, radius = unit * 0.8f, center = position, style = Stroke(width = unit * 0.15f))
}

private const val ASPECT = 16f / 10f
private const val GRID = 18f
private const val BASE_BRIGHTNESS = 0.25f
private const val BRIGHTNESS_PER_LEVEL = 0.15f
private const val HYDROPONICS_RADIUS = 4.2f
private const val PULSE_MILLIS = 2_400
