package ch.no1hardy.orbit7.core.testing

import ch.no1hardy.orbit7.core.domain.model.Budget
import ch.no1hardy.orbit7.core.domain.model.Category
import ch.no1hardy.orbit7.core.domain.model.CategoryKey
import ch.no1hardy.orbit7.core.domain.model.Expense
import ch.no1hardy.orbit7.core.domain.test.aBudget
import ch.no1hardy.orbit7.core.domain.test.aCategory
import ch.no1hardy.orbit7.core.domain.test.anExpense
import ch.no1hardy.orbit7.core.domain.test.chf
import java.time.LocalDate

/**
 * The canonical "three months of realistic Swiss spending" dataset.
 *
 * Baseline tests, report tests and every screenshot fixture draw from the same history, so the
 * numbers on a golden image are believable *and* stable — a screenshot with invented figures is a
 * screenshot nobody reviews properly.
 */
object FixtureHistory {
    /** The Monday the fixture history starts on. */
    val START: LocalDate = LocalDate.of(2025, 1, 6)

    val GROCERIES = 1L
    val EATING_OUT = 2L
    val TRANSPORT = 3L
    val HOUSEHOLD = 4L
    val LEISURE = 5L

    val categories: List<Category> =
        listOf(
            aCategory(id = GROCERIES, key = CategoryKey.GROCERIES, sortOrder = 0),
            aCategory(id = EATING_OUT, key = CategoryKey.EATING_OUT, sortOrder = 1),
            aCategory(id = TRANSPORT, key = CategoryKey.TRANSPORT, sortOrder = 2),
            aCategory(id = HOUSEHOLD, key = CategoryKey.HOUSEHOLD, sortOrder = 3),
            aCategory(id = LEISURE, key = CategoryKey.LEISURE, sortOrder = 4),
        )

    val budgets: List<Budget> =
        listOf(
            aBudget(id = 1, categoryId = GROCERIES, monthly = chf(600), validFrom = START),
            aBudget(id = 2, categoryId = EATING_OUT, monthly = chf(250), validFrom = START),
            aBudget(id = 3, categoryId = TRANSPORT, monthly = chf(120), validFrom = START),
            aBudget(id = 4, categoryId = HOUSEHOLD, monthly = chf(200), validFrom = START),
            aBudget(id = 5, categoryId = LEISURE, monthly = chf(180), validFrom = START),
        )

    /** Twelve weeks of spending, with the mild downward drift a working app is supposed to produce. */
    val expenses: List<Expense> =
        buildList {
            var id = 1L
            repeat(WEEKS) { weekIndex ->
                val monday = START.plusWeeks(weekIndex.toLong())
                val drift = weekIndex * DRIFT_PER_WEEK_RAPPEN
                add(anExpense(chf(0, 14_200 - drift), monday, GROCERIES, id++))
                add(anExpense(chf(0, 3_180), monday.plusDays(1), EATING_OUT, id++, note = "Mittagessen"))
                add(anExpense(chf(0, 780), monday.plusDays(1), TRANSPORT, id++))
                add(anExpense(chf(0, 6_450 - drift / 2), monday.plusDays(3), GROCERIES, id++))
                add(anExpense(chf(0, 2_900), monday.plusDays(4), LEISURE, id++))
                if (weekIndex % 2 == 0) {
                    add(anExpense(chf(0, 4_990), monday.plusDays(5), HOUSEHOLD, id++))
                }
                if (weekIndex % 3 == 0) {
                    add(anExpense(chf(0, 1_890), monday.plusDays(6), EATING_OUT, id++, note = "Lieferung"))
                }
            }
        }

    private const val WEEKS = 12
    private const val DRIFT_PER_WEEK_RAPPEN = 120L
}
