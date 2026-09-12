package ch.no1hardy.orbit7.core.designsystem

import androidx.compose.ui.graphics.Color
import ch.no1hardy.orbit7.core.designsystem.theme.Contrast
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Colors
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.Test
import kotlin.math.round

/**
 * The measured contrast table from `docs/04-design-system.md` §1, asserted.
 *
 * A heavy aesthetic fails accessibility by default, so this is a build failure rather than an audit
 * item: a "small tweak" to a colour that breaks AA cannot reach a device.
 */
class ColorContrastTest {
    private val colors = Orbit7Colors()

    private val surfaces =
        mapOf(
            "void" to colors.surface,
            "panel" to colors.surfaceRaised,
            "bezel" to colors.surfaceEdge,
        )

    private val foregrounds =
        mapOf(
            "cream" to colors.onSurface,
            "phosphorGreen" to colors.energy,
            "phosphorAmber" to colors.accent,
            "alert" to colors.danger,
            "muted" to colors.onSurfaceMuted,
        )

    @Test
    fun `the published contrast table is exactly what the tokens measure`() {
        val expected =
            mapOf(
                "cream" to mapOf("void" to 15.45, "panel" to 14.10, "bezel" to 11.92),
                "phosphorGreen" to mapOf("void" to 14.86, "panel" to 13.56, "bezel" to 11.46),
                "phosphorAmber" to mapOf("void" to 10.89, "panel" to 9.94, "bezel" to 8.40),
                "alert" to mapOf("void" to 6.06, "panel" to 5.53, "bezel" to 4.67),
                "muted" to mapOf("void" to 6.51, "panel" to 5.94, "bezel" to 5.02),
            )

        expected.forEach { (foreground, perSurface) ->
            perSurface.forEach { (surface, ratio) ->
                val measured =
                    round(
                        Contrast.ratio(foregrounds.getValue(foreground), surfaces.getValue(surface)) * 100,
                    ) / 100
                measured shouldBe ratio
            }
        }
    }

    @Test
    fun `every token pairing clears WCAG AA for normal text`() {
        foregrounds.forEach { (foregroundName, foreground) ->
            surfaces.forEach { (surfaceName, surface) ->
                withClue(foregroundName, surfaceName) {
                    Contrast.ratio(foreground, surface) shouldBeGreaterThanOrEqual Contrast.AA
                }
            }
        }
    }

    @Test
    fun `the rejected muted candidate is still rejected`() {
        // #6E7F86 was the first choice and failed AA on bezel at 3.70; it must never come back.
        val rejected = Color(0xFF6E7F86)

        val ratio = round(Contrast.ratio(rejected, colors.surfaceEdge) * 100) / 100

        ratio shouldBe 3.70
        (ratio < Contrast.AA) shouldBe true
    }

    private inline fun withClue(
        foreground: String,
        surface: String,
        block: () -> Unit,
    ) {
        try {
            block()
        } catch (failure: AssertionError) {
            throw AssertionError("$foreground on $surface: ${failure.message}", failure)
        }
    }
}
