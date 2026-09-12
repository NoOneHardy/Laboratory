package ch.no1hardy.orbit7.core.domain.economy

import ch.no1hardy.orbit7.core.domain.GameBalance
import ch.no1hardy.orbit7.core.domain.model.Cadence
import ch.no1hardy.orbit7.core.domain.model.Contract
import ch.no1hardy.orbit7.core.domain.money.IntegerMath
import ch.no1hardy.orbit7.core.domain.money.Money

/**
 * Salvage payouts (`docs/02-game-design.md` §6.2).
 *
 * ```
 * cancel:      salvage = 6 × monthlyEquivalent × epPerMajorUnit
 * renegotiate: salvage = 6 × (previousMonthly − newMonthly) × epPerMajorUnit
 * ```
 *
 * Six months of the saving, paid once, immediately. Deliberately larger than a good week of daily
 * discipline, because the real-world win is larger and the action happens exactly once.
 *
 * The guardrail lives in the use case, not here: salvage is paid once per contract per state
 * transition, and a cancelled contract that comes back is entered as a *new* contract, which pays
 * nothing. That rule is simpler than any reversal logic and leaves nothing to farm.
 */
class SalvageCalculator(
    private val balance: GameBalance = GameBalance.DEFAULT,
) {
    /** What cancelling [contract] pays. */
    fun cancellationPayout(contract: Contract): Int = payoutFor(contract.monthlyEquivalent)

    /** What renegotiating from [previousAmount] down to [newAmount] pays. */
    fun renegotiationPayout(
        previousAmount: Money,
        newAmount: Money,
        cadence: Cadence,
    ): Int {
        val previousMonthly = monthly(previousAmount, cadence)
        val newMonthly = monthly(newAmount, cadence)
        return payoutFor((previousMonthly - newMonthly).coerceAtLeastZero())
    }

    /** The EP a set of drains bleeds per month — the headline figure on the Drains screen. */
    fun monthlyBleedEp(contracts: List<Contract>): Int =
        contracts
            .filter { it.isActive }
            .sumOf { epFor(it.monthlyEquivalent) }
            .toInt()

    private fun payoutFor(monthlySaving: Money): Int = epFor(Money(monthlySaving.minor * balance.salvageMonths)).toInt()

    private fun epFor(amount: Money): Long =
        IntegerMath.roundHalfUpDiv(amount.minor * balance.epPerMajorUnit, Money.MINOR_PER_MAJOR)

    private fun monthly(
        amount: Money,
        cadence: Cadence,
    ): Money = Money(IntegerMath.roundHalfUpDiv(amount.minor, cadence.months.toLong()))
}
