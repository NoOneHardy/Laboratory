package ch.no1hardy.orbit7.core.domain.money

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class IntegerMathTest {
    @ParameterizedTest(name = "{0}/{1} rounds half-up to {2}")
    @CsvSource(
        "10, 4, 3", // 2.5 → 3
        "-10, 4, -3", // −2.5 → −3, away from zero
        "9, 4, 2", // 2.25 → 2
        "11, 4, 3", // 2.75 → 3
        "0, 7, 0",
        "2606, 100, 26", // the worked example's base EP
        "7165, 1000, 7", // the worked example's vented EP
    )
    fun `half-up division`(
        numerator: Long,
        denominator: Long,
        expected: Long,
    ) {
        IntegerMath.roundHalfUpDiv(numerator, denominator) shouldBe expected
    }

    @Test
    fun `basis points apply as a factor of one`() {
        IntegerMath.applyBasisPoints(20_000, 10_000) shouldBe 20_000L
        IntegerMath.applyBasisPoints(20_000, 5_000) shouldBe 10_000L
        IntegerMath.applyBasisPoints(18_000, 9_000) shouldBe 16_200L
    }

    @Test
    fun `rounding to the nearest ten is what the cost curve needs`() {
        IntegerMath.roundToNearest(194, 10) shouldBe 190L
        IntegerMath.roundToNearest(195, 10) shouldBe 200L
        IntegerMath.roundToNearest(1_166, 10) shouldBe 1_170L
    }

    @Test
    fun `integer power replaces the growth factor's floating point`() {
        IntegerMath.pow(18_000, 0) shouldBe 1L
        IntegerMath.pow(18_000, 2) shouldBe 324_000_000L
    }

    @Test
    fun `a fraction sums exactly and rounds only at the end`() {
        // 1/3 + 1/3 + 1/3 must be 1, not 0.99 — this is what keeps a week's proration honest.
        var sum = Fraction.ZERO
        repeat(3) { sum += Fraction(100, 3) }

        sum.roundHalfUp() shouldBe 100L
    }
}
