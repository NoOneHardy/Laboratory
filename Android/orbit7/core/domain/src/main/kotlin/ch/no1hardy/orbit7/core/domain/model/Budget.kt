package ch.no1hardy.orbit7.core.domain.model

import ch.no1hardy.orbit7.core.domain.money.Money
import java.time.LocalDate

/**
 * A monthly budget for one category, **versioned**.
 *
 * Changing a budget closes the current row ([validTo]) and opens a new one, so a settlement from
 * March still evaluates against March's budget even if the budget was raised in May
 * (`docs/02-game-design.md` §4.2).
 */
data class Budget(
    val id: Long,
    val categoryId: Long,
    val amountPerMonth: Money,
    val validFrom: LocalDate,
    val validTo: LocalDate? = null,
) {
    init {
        require(amountPerMonth.minor >= 0) { "A budget cannot be negative" }
        require(validTo == null || !validTo.isBefore(validFrom)) { "validTo precedes validFrom" }
    }

    val isOpen: Boolean get() = validTo == null

    /** Whether this version is the one in force on [date]. */
    fun coversDate(date: LocalDate): Boolean = !date.isBefore(validFrom) && (validTo == null || !date.isAfter(validTo))
}
