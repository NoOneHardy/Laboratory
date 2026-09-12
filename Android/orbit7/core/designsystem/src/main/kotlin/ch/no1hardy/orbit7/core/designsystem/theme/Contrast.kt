package ch.no1hardy.orbit7.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * WCAG 2.1 relative luminance and contrast ratio.
 *
 * Used by the token contrast test, which asserts the measured table in
 * `docs/04-design-system.md` §1 — so a colour tweak that breaks AA fails the build rather than
 * shipping.
 */
object Contrast {
    fun ratio(
        foreground: Color,
        background: Color,
    ): Double {
        val lighter = maxOf(relativeLuminance(foreground), relativeLuminance(background))
        val darker = minOf(relativeLuminance(foreground), relativeLuminance(background))
        return (lighter + OFFSET) / (darker + OFFSET)
    }

    fun relativeLuminance(color: Color): Double =
        RED_WEIGHT * channel(color.red) + GREEN_WEIGHT * channel(color.green) + BLUE_WEIGHT * channel(color.blue)

    private fun channel(value: Float): Double {
        val v = value.toDouble()
        return if (v <= THRESHOLD) v / LOW_DIVISOR else ((v + ALPHA) / (1 + ALPHA)).pow(GAMMA)
    }

    /** WCAG AA for normal text. */
    const val AA = 4.5

    /** WCAG AAA for normal text. */
    const val AAA = 7.0

    private const val OFFSET = 0.05
    private const val RED_WEIGHT = 0.2126
    private const val GREEN_WEIGHT = 0.7152
    private const val BLUE_WEIGHT = 0.0722
    private const val THRESHOLD = 0.03928
    private const val LOW_DIVISOR = 12.92
    private const val ALPHA = 0.055
    private const val GAMMA = 2.4
}
