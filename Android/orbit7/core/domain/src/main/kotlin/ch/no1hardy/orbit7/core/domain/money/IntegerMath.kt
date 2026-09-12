package ch.no1hardy.orbit7.core.domain.money

/**
 * Integer arithmetic helpers.
 *
 * The economy is specified with percentages and multipliers, but no floating point is allowed in
 * this module (hard rule 2). Every factor is therefore carried as **basis points** — 10 000 bp is
 * ×1.0 — and every division rounds half-up exactly once, explicitly.
 */
object IntegerMath {
    /** One in basis points. A factor of ×1.0. */
    const val ONE_BP: Long = 10_000

    /**
     * Divides [numerator] by [denominator], rounding halves away from zero.
     *
     * Half-up is the rounding the game design specifies; it is applied once per conversion, never
     * accumulated.
     */
    fun roundHalfUpDiv(
        numerator: Long,
        denominator: Long,
    ): Long {
        require(denominator != 0L) { "Division by zero" }
        val sign = if ((numerator < 0) != (denominator < 0)) -1 else 1
        val n = Math.abs(numerator)
        val d = Math.abs(denominator)
        val quotient = n / d
        val remainder = n % d
        // `remainder >= d - remainder` is `2 × remainder >= d` without the overflow.
        return sign * (if (remainder >= d - remainder) quotient + 1 else quotient)
    }

    /** Applies a basis-point factor to [value], rounding half-up once. */
    fun applyBasisPoints(
        value: Long,
        basisPoints: Long,
    ): Long = roundHalfUpDiv(value * basisPoints, ONE_BP)

    /** Rounds [value] to the nearest multiple of [step], halves up. */
    fun roundToNearest(
        value: Long,
        step: Long,
    ): Long = roundHalfUpDiv(value, step) * step

    /** Integer power, used for the module cost curve so the growth factor never becomes a Double. */
    fun pow(
        base: Long,
        exponent: Int,
    ): Long {
        require(exponent >= 0) { "Negative exponent: $exponent" }
        var result = 1L
        repeat(exponent) { result *= base }
        return result
    }
}

/**
 * An exact non-negative fraction, used to accumulate a week's budget proration without rounding
 * each of the seven daily terms (`docs/02-game-design.md` §3.2 — "rounding to minor units happens
 * once on the sum").
 */
internal data class Fraction(
    val numerator: Long,
    val denominator: Long,
) {
    init {
        require(denominator > 0) { "Denominator must be positive" }
    }

    operator fun plus(other: Fraction): Fraction {
        val gcd = gcd(denominator, other.denominator)
        val commonDenominator = denominator / gcd * other.denominator
        val n =
            numerator * (commonDenominator / denominator) +
                other.numerator * (commonDenominator / other.denominator)
        return Fraction(n, commonDenominator).reduced()
    }

    fun roundHalfUp(): Long = IntegerMath.roundHalfUpDiv(numerator, denominator)

    private fun reduced(): Fraction {
        if (numerator == 0L) return ZERO
        val gcd = gcd(Math.abs(numerator), denominator)
        return Fraction(numerator / gcd, denominator / gcd)
    }

    companion object {
        val ZERO = Fraction(0, 1)

        private tailrec fun gcd(
            a: Long,
            b: Long,
        ): Long = if (b == 0L) a else gcd(b, a % b)
    }
}
