package ch.no1hardy.orbit7.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Reference tokens (`docs/04-design-system.md` §1).
 *
 * Dark-only by design — a CRT does not have a light mode. Screens never touch these values: they
 * use the semantic tokens in [Orbit7Colors] instead, which is what makes the whole look a token
 * layer rather than hex codes sprinkled through the UI.
 *
 * Any new colour must be contrast-checked before it enters this list. The measured ratios are
 * asserted by `ColorContrastTest`, so a "small tweak" cannot silently break AA.
 */
internal object ReferenceColors {
    /** App background — near-black with a green cast, not pure black. */
    val Void = Color(0xFF07090A)
    val Panel = Color(0xFF11161A)
    val Bezel = Color(0xFF1D262B)
    val PhosphorAmber = Color(0xFFFFB000)
    val PhosphorGreen = Color(0xFF33FF66)
    val Alert = Color(0xFFFF4E2B)

    /**
     * Deliberately lighter than the classic terminal grey: the first candidate (#6E7F86) failed AA
     * on `bezel` at 3.70 and was rejected.
     */
    val Muted = Color(0xFF8496A0)

    /** Primary text — warm off-white, never #FFFFFF. */
    val Cream = Color(0xFFE8E2D4)
}

/**
 * The semantic tokens screens actually use.
 *
 * Colour is never the only signal: overdrawn also gets hazard striping and a label, positive deltas
 * also get a `+` and an upward glyph, lit modules are also brighter *and* animated
 * (`docs/04-design-system.md` §1).
 */
@Immutable
data class Orbit7Colors(
    val surface: Color = ReferenceColors.Void,
    val surfaceRaised: Color = ReferenceColors.Panel,
    val surfaceEdge: Color = ReferenceColors.Bezel,
    val onSurface: Color = ReferenceColors.Cream,
    val onSurfaceMuted: Color = ReferenceColors.Muted,
    val accent: Color = ReferenceColors.PhosphorAmber,
    val energy: Color = ReferenceColors.PhosphorGreen,
    val warning: Color = ReferenceColors.PhosphorAmber,
    val danger: Color = ReferenceColors.Alert,
    val gaugeTrack: Color = ReferenceColors.Bezel,
    val gaugeFill: Color = ReferenceColors.PhosphorGreen,
    val hazardStripe: Color = ReferenceColors.PhosphorAmber,
    val scanline: Color = Color(0x1433FF66),
)
