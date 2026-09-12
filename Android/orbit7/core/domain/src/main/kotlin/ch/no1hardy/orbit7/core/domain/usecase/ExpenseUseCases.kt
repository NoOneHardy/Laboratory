package ch.no1hardy.orbit7.core.domain.usecase

import ch.no1hardy.orbit7.core.domain.model.Expense
import ch.no1hardy.orbit7.core.domain.model.ExpenseSource
import ch.no1hardy.orbit7.core.domain.model.ZeroSpendSource
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.repository.ExpenseRepository
import ch.no1hardy.orbit7.core.domain.repository.ZeroSpendRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Saves an expense.
 *
 * Logging a spend on a day that was marked as zero-spend silently clears that mark — the two
 * statements contradict each other and the expense is the stronger signal.
 */
class LogExpenseUseCase
    @Inject
    constructor(
        private val expenses: ExpenseRepository,
        private val zeroSpend: ZeroSpendRepository,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(
            amount: Money,
            categoryId: Long,
            occurredOn: LocalDate = LocalDate.now(clock),
            note: String? = null,
            currency: String = DEFAULT_CURRENCY,
        ): Long {
            require(amount.isPositive) { "An expense must be greater than zero" }
            val now = clock.instant()
            val id =
                expenses.add(
                    Expense(
                        id = 0,
                        amount = amount,
                        currency = currency,
                        categoryId = categoryId,
                        occurredOn = occurredOn,
                        note = note?.takeIf { it.isNotBlank() },
                        createdAt = now,
                        updatedAt = now,
                        source = ExpenseSource.MANUAL,
                    ),
                )
            zeroSpend.unmark(occurredOn)
            return id
        }

        private companion object {
            const val DEFAULT_CURRENCY = "CHF"
        }
    }

/** Updates an existing expense, keeping `createdAt` and refreshing `updatedAt`. */
class UpdateExpenseUseCase
    @Inject
    constructor(
        private val expenses: ExpenseRepository,
        private val zeroSpend: ZeroSpendRepository,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(
            id: Long,
            amount: Money,
            categoryId: Long,
            occurredOn: LocalDate,
            note: String?,
        ) {
            val existing = requireNotNull(expenses.expense(id)) { "No expense $id" }
            expenses.update(
                existing.copy(
                    amount = amount,
                    categoryId = categoryId,
                    occurredOn = occurredOn,
                    note = note?.takeIf { it.isNotBlank() },
                    updatedAt = clock.instant(),
                ),
            )
            zeroSpend.unmark(occurredOn)
        }
    }

/**
 * Marks a day as zero-spend — the confidence-factor affordance
 * (`docs/02-game-design.md` §3.3).
 *
 * One tap on the Bridge's today card or on the daily notification. Marking is idempotent, and a day
 * on which something was actually logged cannot be marked.
 */
class MarkZeroSpendDayUseCase
    @Inject
    constructor(
        private val zeroSpend: ZeroSpendRepository,
        private val expenses: ExpenseRepository,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(
            date: LocalDate = LocalDate.now(clock),
            source: ZeroSpendSource = ZeroSpendSource.APP,
        ): Boolean {
            if (expenses.between(date, date).isNotEmpty()) return false
            zeroSpend.mark(date, clock.instant(), source)
            return true
        }
    }

/** Undoes a zero-spend mark. Offered so the affordance is never a trap. */
class UnmarkZeroSpendDayUseCase
    @Inject
    constructor(
        private val zeroSpend: ZeroSpendRepository,
    ) {
        suspend operator fun invoke(date: LocalDate) = zeroSpend.unmark(date)
    }
