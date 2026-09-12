package ch.no1hardy.orbit7.core.domain.money

import ch.no1hardy.orbit7.core.domain.test.chf
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MoneyTest {
    @Test
    fun `money is minor units all the way down`() {
        chf(12, 50).minor shouldBe 1_250L
        Money.ofMajor(1_234, 50) shouldBe Money(123_450)
    }

    @Test
    fun `arithmetic stays exact`() {
        (chf(10) + chf(0, 5)) shouldBe chf(10, 5)
        (chf(10) - chf(12)) shouldBe Money(-200)
        (chf(1, 25) * 4) shouldBe chf(5)
    }

    @Test
    fun `a negative amount coerces to zero for surplus and overdraw maths`() {
        Money(-500).coerceAtLeastZero() shouldBe Money.ZERO
        chf(5).coerceAtLeastZero() shouldBe chf(5)
    }

    @Test
    fun `comparison and predicates`() {
        (chf(5) > chf(4, 99)).shouldBeTrue()
        Money.ZERO.isPositive.shouldBeFalse()
        Money.ZERO.isZero.shouldBeTrue()
    }

    @Test
    fun `summing a list never leaves the integer domain`() {
        listOf(chf(0, 10), chf(0, 20), chf(0, 30)).sum() shouldBe chf(0, 60)
    }
}
