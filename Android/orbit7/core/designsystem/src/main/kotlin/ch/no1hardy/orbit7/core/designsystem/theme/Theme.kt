package ch.no1hardy.orbit7.core.designsystem.theme

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle

/**
 * The app's theme.
 *
 * **Material 3 defaults are deliberately not used.** Pulling in `MaterialTheme` colours would
 * quietly reintroduce Material blues through component defaults, and the whole point of
 * `docs/04-design-system.md` is that this app looks like an instrument, not like a Material app
 * with a dark palette.
 */
object Orbit7Theme {
    val colors: Orbit7Colors
        @Composable get() = LocalOrbit7Colors.current

    val typography: Orbit7Typography
        @Composable get() = LocalOrbit7Typography.current

    val shapes: Orbit7Shapes
        @Composable get() = LocalOrbit7Shapes.current

    val motion: Orbit7Motion
        @Composable get() = LocalOrbit7Motion.current
}

val LocalOrbit7Colors: ProvidableCompositionLocal<Orbit7Colors> = staticCompositionLocalOf { Orbit7Colors() }
val LocalOrbit7Typography: ProvidableCompositionLocal<Orbit7Typography> =
    staticCompositionLocalOf { Orbit7Typography() }
val LocalOrbit7Shapes: ProvidableCompositionLocal<Orbit7Shapes> = staticCompositionLocalOf { Orbit7Shapes() }
val LocalOrbit7Motion: ProvidableCompositionLocal<Orbit7Motion> = staticCompositionLocalOf { Orbit7Motion() }

/**
 * @param reduceEffects the user's setting. It is ORed with the system's own reduce-motion state:
 * scanline and flicker effects are a genuine photosensitivity risk, so honouring the system
 * setting is not optional (`docs/04-design-system.md` §6).
 */
@Composable
fun Orbit7Theme(
    reduceEffects: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemReducesMotion = systemAnimationsDisabled()
    val motion =
        remember(reduceEffects, systemReducesMotion) {
            Orbit7Motion(reduceEffects = reduceEffects || systemReducesMotion)
        }
    val colors = remember { Orbit7Colors() }
    val typography = remember { Orbit7Typography() }
    val shapes = remember { Orbit7Shapes() }

    CompositionLocalProvider(
        LocalOrbit7Colors provides colors,
        LocalOrbit7Typography provides typography,
        LocalOrbit7Shapes provides shapes,
        LocalOrbit7Motion provides motion,
    ) {
        androidx.compose.material3.ProvideTextStyle(
            value = typography.body.copy(color = colors.onSurface),
        ) {
            androidx.compose.foundation.layout.Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(colors.surface),
            ) {
                content()
            }
        }
    }
}

/** True when the system animation scale is 0 or the device is in a reduce-motion state. */
@Composable
private fun systemAnimationsDisabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale =
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        scale == 0f
    }
}

/** Convenience for components that want the body style with a different colour. */
@Composable
fun bodyStyle(): TextStyle = Orbit7Theme.typography.body.copy(color = Orbit7Theme.colors.onSurface)
