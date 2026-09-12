package ch.no1hardy.orbit7.core.domain.time

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

class WeekTest {
    @Test
    fun `a week runs Monday to Sunday by default`() {
        val week = Week.containing(LocalDate.of(2025, 4, 17)) // a Thursday

        week.start shouldBe LocalDate.of(2025, 4, 14)
        week.endInclusive shouldBe LocalDate.of(2025, 4, 20)
        week.days.size shouldBe 7
    }

    @Test
    fun `the first day of the week is configurable`() {
        val week = Week.containing(LocalDate.of(2025, 4, 17), DayOfWeek.SUNDAY)

        week.start shouldBe LocalDate.of(2025, 4, 13)
        week.endInclusive shouldBe LocalDate.of(2025, 4, 19)
    }

    @Test
    fun `containment is inclusive at both ends`() {
        val week = Week(LocalDate.of(2025, 4, 14))

        (LocalDate.of(2025, 4, 14) in week).shouldBeTrue()
        (LocalDate.of(2025, 4, 20) in week).shouldBeTrue()
        (LocalDate.of(2025, 4, 13) in week).shouldBeFalse()
        (LocalDate.of(2025, 4, 21) in week).shouldBeFalse()
    }

    @Test
    fun `complete weeks between two points exclude the open week`() {
        val from = Week(LocalDate.of(2025, 3, 24))
        val until = Week(LocalDate.of(2025, 4, 14))

        Week.completeWeeksBetween(from, until).map { it.start } shouldBe
            listOf(
                LocalDate.of(2025, 3, 24),
                LocalDate.of(2025, 3, 31),
                LocalDate.of(2025, 4, 7),
            )
    }

    @Test
    fun `the id round-trips`() {
        val week = Week(LocalDate.of(2025, 4, 14))

        weekOfId(week.id) shouldBe week
        week.id shouldBe "2025-04-14"
    }
}
