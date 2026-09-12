package ch.no1hardy.orbit7.core.domain.model

import ch.no1hardy.orbit7.core.domain.money.IntegerMath
import ch.no1hardy.orbit7.core.domain.money.Money
import java.time.Instant
import java.time.LocalDate

/**
 * A recurring cost — a parasitic drain tapped into the station's power bus
 * (`docs/02-game-design.md` §6).
 */
data class Contract(
    val id: Long,
    val name: String,
    val categoryId: Long,
    val amount: Money,
    val cadence: Cadence,
    val nextChargeOn: LocalDate,
    val noticePeriod: NoticePeriod,
    val earliestCancellationOn: LocalDate,
    val status: ContractStatus = ContractStatus.ACTIVE,
    val previousAmount: Money? = null,
    val statusChangedOn: LocalDate? = null,
    val createdAt: Instant,
) {
    init {
        require(name.isNotBlank()) { "A contract needs a name" }
        require(amount.isPositive) { "A contract amount must be greater than zero" }
    }

    val isActive: Boolean get() = status == ContractStatus.ACTIVE

    /** The contract's cost normalised to one month, rounded half-up to minor units. */
    val monthlyEquivalent: Money
        get() = Money(IntegerMath.roundHalfUpDiv(amount.minor, cadence.months.toLong()))

    /** For a renegotiated contract, what it used to cost per month. */
    val previousMonthlyEquivalent: Money?
        get() = previousAmount?.let { Money(IntegerMath.roundHalfUpDiv(it.minor, cadence.months.toLong())) }

    /** The monthly saving a renegotiation achieved. */
    val monthlySaving: Money
        get() =
            when (status) {
                ContractStatus.CANCELLED -> monthlyEquivalent
                ContractStatus.RENEGOTIATED -> (previousMonthlyEquivalent ?: Money.ZERO) - monthlyEquivalent
                ContractStatus.ACTIVE -> Money.ZERO
            }
}

/** How often a contract charges. Normalised to a monthly equivalent everywhere else. */
enum class Cadence(
    val months: Int,
) {
    MONTHLY(1),
    QUARTERLY(3),
    YEARLY(12),
}

enum class ContractStatus {
    ACTIVE,
    CANCELLED,
    RENEGOTIATED,
}

/**
 * A cancellation notice period.
 *
 * Swiss contracts are usually stated in months ("3 Monate im Voraus"), and three months before
 * 31 March is 31 December, not 90 days earlier — so months and days are modelled separately and
 * applied as calendar arithmetic.
 */
data class NoticePeriod(
    val months: Int = 0,
    val days: Int = 0,
) {
    init {
        require(months >= 0 && days >= 0) { "A notice period cannot be negative" }
    }

    val isNone: Boolean get() = months == 0 && days == 0

    /** The last day on which notice can still be given for a renewal on [renewalDate]. */
    fun latestNoticeDateFor(renewalDate: LocalDate): LocalDate =
        renewalDate.minusMonths(months.toLong()).minusDays(days.toLong())

    companion object {
        val NONE = NoticePeriod()
        val ONE_MONTH = NoticePeriod(months = 1)
        val THREE_MONTHS = NoticePeriod(months = 3)

        /** The CH-typical presets offered in the contract editor (`docs/03-screens.md` §7). */
        val PRESETS = listOf(NONE, ONE_MONTH, THREE_MONTHS)
    }
}
