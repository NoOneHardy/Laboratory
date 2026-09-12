package ch.no1hardy.orbit7.core.domain

import ch.no1hardy.orbit7.core.domain.money.IntegerMath

/**
 * Every tunable constant of the economy, in one place.
 *
 * `docs/02-game-design.md` §8: all balancing constants live here so a year of simulated play can be
 * re-run against a changed number without hunting through feature code. Factors are basis points
 * (10 000 = ×1.0) because this module contains no floating point arithmetic.
 */
data class GameBalance(
    /** EP earned per major currency unit (CHF) saved. */
    val epPerMajorUnit: Long = 1,
    /** Overdraw vents power at half the rate at which surplus generates it. */
    val overdrawRateBasisPoints: Long = 5_000,
    /** Confidence floor: a completely silent week still pays ×0.4. */
    val confidenceFloorBasisPoints: Long = 4_000,
    /** The span confidence can travel: floor + span = ×1.0 at 7 signal days. */
    val confidenceSpanBasisPoints: Long = 6_000,
    /** Each consecutive good week adds +5%. */
    val streakStepBasisPoints: Long = 500,
    /** The streak bonus caps at +50%, reached after 10 good weeks. */
    val streakCapBasisPoints: Long = 5_000,
    /** Days of history a category needs before a baseline is computed. */
    val baselineMinimumHistoryDays: Int = 28,
    /** The rolling window, in complete weeks, of the baseline median. */
    val baselineWindowWeeks: Int = 12,
    /** A baseline suggestion proposes cutting the baseline by this much. */
    val suggestedCutBasisPoints: Long = 1_000,
    /** A dismissed suggestion stays dismissed for this long. */
    val suggestionCooldownDays: Int = 28,
    /** Salvage pays this many months of the saving, once. */
    val salvageMonths: Int = 6,
    /**
     * Module cost curve: `cost(n) = baseCost × 1.8^(n−1)`, rounded to the nearest 10 EP.
     *
     * The growth factor is a reduced fraction (9/5) rather than a decimal, so raising it to the
     * fourth power for a level-5 module stays comfortably inside a `Long`.
     */
    val moduleCostGrowthNumerator: Long = 9,
    val moduleCostGrowthDenominator: Long = 5,
    val moduleCostRoundingStep: Long = 10,
    /** Contract deadline reminders fire this many days before the last cancellation date. */
    val deadlineReminderDays: List<Int> = listOf(30, 14, 3),
    /** Inside this many days a contract deadline is rendered as urgent (hazard striping). */
    val deadlineUrgentWithinDays: Int = 14,
    /** Mission payouts, by difficulty. */
    val missionPayoutEasy: Int = 15,
    val missionPayoutMedium: Int = 25,
    val missionPayoutHard: Int = 40,
    /** Missions offered at a time. */
    val missionOfferSize: Int = 3,
) {
    init {
        require(epPerMajorUnit > 0) { "EP rate must be positive" }
        require(confidenceFloorBasisPoints + confidenceSpanBasisPoints == IntegerMath.ONE_BP) {
            "Confidence must reach exactly ×1.0 at full engagement"
        }
    }

    companion object {
        /** The shipped balance. Changing anything here changes the game, and only the game. */
        val DEFAULT = GameBalance()
    }
}
