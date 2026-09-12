package ch.no1hardy.orbit7.core.domain.economy

import ch.no1hardy.orbit7.core.domain.GameBalance
import ch.no1hardy.orbit7.core.domain.model.Contract
import ch.no1hardy.orbit7.core.domain.model.NoticePeriod
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Deadline maths for Contract Hunter (`docs/02-game-design.md` §6.3).
 *
 * The derived dates are always shown with their calculation spelled out in the editor, because a
 * wrong renewal date costs real money and should be obvious before it does.
 */
class ContractSchedule(
    private val balance: GameBalance = GameBalance.DEFAULT,
) {
    /**
     * The last day on which the contract can still be cancelled before it renews.
     *
     * Calendar arithmetic, not 30-day months: three months' notice on a renewal of 31 March is
     * 31 December, and on 31 May it is 28 February (29 in a leap year).
     */
    fun earliestCancellationOn(
        nextChargeOn: LocalDate,
        noticePeriod: NoticePeriod,
    ): LocalDate = noticePeriod.latestNoticeDateFor(nextChargeOn)

    /** Days from [today] to the deadline. Negative once the deadline has passed. */
    fun daysUntilDeadline(
        contract: Contract,
        today: LocalDate,
    ): Long = ChronoUnit.DAYS.between(today, contract.earliestCancellationOn)

    /** Inside 14 days the row shouts — the one place the design system is allowed to. */
    fun isUrgent(
        contract: Contract,
        today: LocalDate,
    ): Boolean {
        val days = daysUntilDeadline(contract, today)
        return contract.isActive && days >= 0 && days <= balance.deadlineUrgentWithinDays
    }

    /**
     * The reminder threshold that is now due for [contract], or `null` if none is.
     *
     * Thresholds already in [alreadyNotified] never fire again, so each of 30 / 14 / 3 days produces
     * exactly one notification per contract.
     */
    fun dueReminderThreshold(
        contract: Contract,
        today: LocalDate,
        alreadyNotified: Set<Int>,
    ): Int? {
        if (!contract.isActive) return null
        val days = daysUntilDeadline(contract, today)
        if (days < 0) return null
        return balance.deadlineReminderDays
            .filter { it !in alreadyNotified && days <= it }
            .minOrNull()
    }

    /**
     * Rolls [nextChargeOn] forward by whole cadence periods until it is no longer in the past.
     *
     * Used when the app has not been opened for a while: a monthly contract charged on the 3rd does
     * not accumulate missed charges, it simply has a new next charge date.
     */
    fun rollForward(
        contract: Contract,
        today: LocalDate,
    ): Contract {
        if (!contract.nextChargeOn.isBefore(today)) return contract
        var next = contract.nextChargeOn
        while (next.isBefore(today)) {
            next = next.plusMonths(contract.cadence.months.toLong())
        }
        return contract.copy(
            nextChargeOn = next,
            earliestCancellationOn = earliestCancellationOn(next, contract.noticePeriod),
        )
    }
}
