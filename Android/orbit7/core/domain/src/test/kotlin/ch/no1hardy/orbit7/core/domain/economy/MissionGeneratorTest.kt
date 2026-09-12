package ch.no1hardy.orbit7.core.domain.economy

import ch.no1hardy.orbit7.core.domain.model.MissionKind
import ch.no1hardy.orbit7.core.domain.test.Fixtures
import ch.no1hardy.orbit7.core.domain.test.aWeek
import ch.no1hardy.orbit7.core.domain.test.anExpense
import ch.no1hardy.orbit7.core.domain.test.chf
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MissionGeneratorTest {
    private val generator = MissionGenerator()
    private val evaluator = MissionEvaluator()
    private val week = aWeek(LocalDate.of(2025, 4, 14))
    private val previous = week.previous()

    @Test
    @DisplayName("missions come from the player's own data, not from a difficulty table")
    fun `an offer is built from baselines, quiet days and repeated notes`() {
        val offer =
            generator.generate(
                MissionContext(
                    week = week,
                    baselines = mapOf(Fixtures.EATING_OUT to chf(100), Fixtures.TRANSPORT to chf(20)),
                    previousWeekExpenses =
                        listOf(
                            anExpense(chf(8), previous.start, note = "Lieferung"),
                            anExpense(chf(9), previous.start.plusDays(2), note = "Lieferung", id = 2),
                        ),
                    previousWeekZeroSpendDays = 2,
                ),
            )

        offer shouldHaveSize 3
        val cap = offer.first { it.kind == MissionKind.CATEGORY_CAP }
        cap.categoryId shouldBe Fixtures.EATING_OUT // the biggest baseline is the interesting one
        cap.targetAmount shouldBe chf(75) // 25% below the baseline
        offer.first { it.kind == MissionKind.ZERO_SPEND_DAYS }.targetCount shouldBe 3 // one more than last week
        offer.any { it.kind == MissionKind.AVOID_NOTE_PATTERN }.shouldBeTrue()
    }

    @Test
    fun `a note that happened once is not a pattern`() {
        val offer =
            generator.generate(
                MissionContext(
                    week = week,
                    baselines = emptyMap(),
                    previousWeekExpenses = listOf(anExpense(chf(8), previous.start, note = "Lieferung")),
                    previousWeekZeroSpendDays = 0,
                ),
            )

        offer.none { it.kind == MissionKind.AVOID_NOTE_PATTERN }.shouldBeTrue()
        offer.none { it.kind == MissionKind.CATEGORY_CAP }.shouldBeTrue()
        offer shouldHaveSize 1
    }

    @Test
    fun `generation is deterministic, so a re-render never reshuffles the cards`() {
        val context = MissionContext(week, mapOf(Fixtures.GROCERIES to chf(180)), emptyList(), 1)

        generator.generate(context) shouldBe generator.generate(context)
    }

    @Test
    fun `a category cap is completed by staying under the target`() {
        val mission =
            generator
                .generate(
                    MissionContext(week, mapOf(Fixtures.EATING_OUT to chf(100)), emptyList(), 0),
                ).first { it.kind == MissionKind.CATEGORY_CAP }

        val under = listOf(anExpense(chf(70), week.start, Fixtures.EATING_OUT))
        val over = listOf(anExpense(chf(80), week.start, Fixtures.EATING_OUT))

        evaluator.isCompleted(mission, under, emptySet()).shouldBeTrue()
        evaluator.isCompleted(mission, over, emptySet()).shouldBeFalse()
    }

    @Test
    fun `zero-spend days count only days that are marked and carry no expense`() {
        val mission =
            generator
                .generate(MissionContext(week, emptyMap(), emptyList(), 1))
                .first { it.kind == MissionKind.ZERO_SPEND_DAYS } // target: 2 days

        val marks = setOf(week.start, week.start.plusDays(1))
        evaluator.isCompleted(mission, emptyList(), marks).shouldBeTrue()
        evaluator
            .isCompleted(
                mission,
                listOf(anExpense(chf(5), week.start)),
                marks,
            ).shouldBeFalse()
    }

    @Test
    fun `avoiding a pattern means the note does not reappear`() {
        val mission =
            generator
                .generate(
                    MissionContext(
                        week = week,
                        baselines = emptyMap(),
                        previousWeekExpenses =
                            listOf(
                                anExpense(chf(8), previous.start, note = "Lieferung"),
                                anExpense(chf(9), previous.start.plusDays(1), note = "Lieferung", id = 2),
                            ),
                        previousWeekZeroSpendDays = 0,
                    ),
                ).first { it.kind == MissionKind.AVOID_NOTE_PATTERN }

        evaluator
            .isCompleted(mission, listOf(anExpense(chf(8), week.start, note = "Kaffee")), emptySet())
            .shouldBeTrue()
        evaluator
            .isCompleted(mission, listOf(anExpense(chf(8), week.start, note = "Lieferung")), emptySet())
            .shouldBeFalse()
    }
}
