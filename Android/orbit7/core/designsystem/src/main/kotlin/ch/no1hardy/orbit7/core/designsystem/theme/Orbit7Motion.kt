package ch.no1hardy.orbit7.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.Immutable

/**
 * Every duration in the app (`docs/04-design-system.md` §4).
 *
 * Motion carries the whole "this is a machine" illusion, so it is specified rather than improvised —
 * and it all lives here so that [reduceEffects] has exactly one place to intervene.
 */
@Immutable
data class Orbit7Motion(
    val reduceEffects: Boolean = false,
    private val bootSweepMillis: Int = 600,
    private val digitRollMillis: Int = 320,
    private val digitStaggerMillis: Int = 30,
    private val toggleMillis: Int = 120,
    private val savePulseMillis: Int = 300,
    private val settlementMillis: Int = 2_500,
    private val powerUpMillis: Int = 1_800,
    private val scanlineLoopMillis: Int = 24_000,
) {
    val bootSweep: Int get() = if (reduceEffects) 0 else bootSweepMillis

    /** Digit rolls become instant with effects reduced; the number still changes, it just arrives. */
    val digitRoll: Int get() = if (reduceEffects) 0 else digitRollMillis
    val digitStagger: Int get() = if (reduceEffects) 0 else digitStaggerMillis
    val toggle: Int get() = if (reduceEffects) 0 else toggleMillis
    val savePulse: Int get() = if (reduceEffects) 0 else savePulseMillis
    val settlement: Int get() = if (reduceEffects) 0 else settlementMillis
    val powerUp: Int get() = if (reduceEffects) 0 else powerUpMillis
    val scanlineLoop: Int get() = scanlineLoopMillis

    /** Ambient effects: scanline drift, vignette flicker, bus-line particles. */
    val ambientEnabled: Boolean get() = !reduceEffects

    val standardEasing: Easing get() = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val mechanicalEasing: Easing get() = CubicBezierEasing(0.9f, 0f, 0.1f, 1f)
    val linear: Easing get() = LinearEasing
}
