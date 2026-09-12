package ch.no1hardy.orbit7.core.domain.money

/**
 * An amount of money, always in **minor units** (Rappen).
 *
 * Hard rule 2 from `docs/05-architecture.md` §5: money is `Long` minor units end to end, never a
 * floating point number, at any layer. Formatting to `CHF 1'234.50` happens exactly once, in
 * `MoneyFormatter` at the UI edge.
 */
@JvmInline
value class Money(
    val minor: Long,
) : Comparable<Money> {
    operator fun plus(other: Money): Money = Money(minor + other.minor)

    operator fun minus(other: Money): Money = Money(minor - other.minor)

    operator fun times(factor: Int): Money = Money(minor * factor)

    operator fun unaryMinus(): Money = Money(-minor)

    val isZero: Boolean get() = minor == 0L

    val isPositive: Boolean get() = minor > 0L

    fun coerceAtLeastZero(): Money = if (minor < 0) ZERO else this

    override fun compareTo(other: Money): Int = minor.compareTo(other.minor)

    override fun toString(): String = "Money(${minor}rp)"

    companion object {
        val ZERO = Money(0)

        /** Minor units in one major unit. CHF has 100 Rappen. */
        const val MINOR_PER_MAJOR: Long = 100

        /** Convenience for fixtures and defaults: `Money.ofMajor(12, 50)` is CHF 12.50. */
        fun ofMajor(
            major: Long,
            minorPart: Long = 0,
        ): Money = Money(major * MINOR_PER_MAJOR + minorPart)
    }
}

/** Sums money without leaving the integer domain. */
fun Iterable<Money>.sum(): Money = Money(sumOf { it.minor })
