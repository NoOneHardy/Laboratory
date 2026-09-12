package ch.no1hardy.orbit7.core.domain.model

import ch.no1hardy.orbit7.core.domain.money.Money
import java.time.Instant
import java.time.LocalDate

/** A single logged spend. */
data class Expense(
    val id: Long,
    val amount: Money,
    val currency: String,
    val categoryId: Long,
    val occurredOn: LocalDate,
    val note: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val source: ExpenseSource = ExpenseSource.MANUAL,
) {
    init {
        require(amount.isPositive) { "An expense must be greater than zero" }
    }
}

/**
 * Where an expense came from.
 *
 * Only [MANUAL] exists in v1; the enum exists so a future importer never needs a schema migration.
 */
enum class ExpenseSource {
    MANUAL,
    IMPORT,
}
