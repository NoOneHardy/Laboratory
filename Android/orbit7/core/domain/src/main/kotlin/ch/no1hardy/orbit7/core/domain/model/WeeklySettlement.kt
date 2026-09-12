package ch.no1hardy.orbit7.core.domain.model

import ch.no1hardy.orbit7.core.domain.money.IntegerMath
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.time.Week
import java.time.Instant

/**
 * The immutable record of one settled week.
 *
 * Keyed by the week's first day, which is exactly what makes settlement idempotent: a week already
 * settled can never be settled twice, however often the worker fires.
 */
data class WeeklySettlement(
    val week: Week,
    val baseEp: Int,
    val ventedEp: Int,
    val confidenceBasisPoints: Int,
    val streakBasisPoints: Int,
    val awardedEp: Int,
    val signalDays: Int,
    val goodWeek: Boolean,
    val budgeted: Money,
    val spent: Money,
    val settledAt: Instant,
    val bestCategoryId: Long? = null,
    val worstCategoryId: Long? = null,
) {
    /** The streak bonus expressed as whole percent, for display ("+15%"). */
    val streakBonusPercent: Int
        get() = ((streakBasisPoints - IntegerMath.ONE_BP) / PERCENT_BP).toInt()

    /** The confidence factor expressed as whole percent, for display ("91%"). */
    val confidencePercent: Int
        get() = (confidenceBasisPoints / PERCENT_BP).toInt()

    private companion object {
        const val PERCENT_BP = 100L
    }
}

/**
 * The per-category detail of a settlement, used by the settlement summary screen to name the best
 * and worst category of the week.
 */
data class CategorySettlement(
    val categoryId: Long,
    val budgeted: Money,
    val spent: Money,
) {
    val surplus: Money get() = (budgeted - spent).coerceAtLeastZero()
    val overdraw: Money get() = (spent - budgeted).coerceAtLeastZero()
}
