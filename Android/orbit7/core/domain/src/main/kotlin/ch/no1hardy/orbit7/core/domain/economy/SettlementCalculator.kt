package ch.no1hardy.orbit7.core.domain.economy

import ch.no1hardy.orbit7.core.domain.GameBalance
import ch.no1hardy.orbit7.core.domain.model.Budget
import ch.no1hardy.orbit7.core.domain.model.CategorySettlement
import ch.no1hardy.orbit7.core.domain.model.Expense
import ch.no1hardy.orbit7.core.domain.model.WeeklySettlement
import ch.no1hardy.orbit7.core.domain.money.IntegerMath
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.money.sum
import ch.no1hardy.orbit7.core.domain.time.Week
import java.time.Instant
import java.time.LocalDate

/** Everything one week's settlement needs. A pure input — no repositories, no clock, no I/O. */
data class SettlementInput(
    val week: Week,
    val budgets: List<Budget>,
    val expenses: List<Expense>,
    val zeroSpendDates: Set<LocalDate>,
    val consecutiveGoodWeeksBefore: Int,
    val settledAt: Instant,
)

/** The outcome of a settlement: the record to store, and the per-category detail behind it. */
data class SettlementResult(
    val settlement: WeeklySettlement,
    val categories: List<CategorySettlement>,
)

/**
 * The heart of the loop (`docs/02-game-design.md` §3).
 *
 * ```
 * base       = Σ surplus  × epPerMajorUnit
 * vented     = Σ overdraw × epPerMajorUnit × overdrawRate
 * confidence = 0.4 + 0.6 × (signalDays / 7)
 * streak     = 1.0 + min(0.05 × consecutiveGoodWeeks, 0.50)
 * epAwarded  = roundHalfUp(base × confidence × streak) − roundHalfUp(vented)
 * ```
 *
 * All of it in integer arithmetic: factors are basis points, money is Rappen, and every division
 * rounds half-up exactly where the specification says it does.
 */
class SettlementCalculator(
    private val balance: GameBalance = GameBalance.DEFAULT,
) {
    fun settle(input: SettlementInput): SettlementResult {
        val categories = perCategory(input)

        val surplus = categories.map { it.surplus }.sum()
        val overdraw = categories.map { it.overdraw }.sum()

        val baseEp = toEp(surplus)
        val ventedEp =
            IntegerMath.roundHalfUpDiv(
                overdraw.minor * balance.epPerMajorUnit * balance.overdrawRateBasisPoints,
                Money.MINOR_PER_MAJOR * IntegerMath.ONE_BP,
            )

        val signalDays = signalDays(input)
        val confidenceBp = confidenceBasisPoints(signalDays)
        val streakBp = streakBasisPoints(input.consecutiveGoodWeeksBefore)

        val boosted =
            IntegerMath.roundHalfUpDiv(
                baseEp * confidenceBp * streakBp,
                IntegerMath.ONE_BP * IntegerMath.ONE_BP,
            )
        val awarded = boosted - ventedEp

        val budgeted = categories.map { it.budgeted }.sum()
        val spent = categories.map { it.spent }.sum()

        val settlement =
            WeeklySettlement(
                week = input.week,
                baseEp = baseEp.toInt(),
                ventedEp = ventedEp.toInt(),
                confidenceBasisPoints = confidenceBp.toInt(),
                streakBasisPoints = streakBp.toInt(),
                awardedEp = awarded.toInt(),
                signalDays = signalDays,
                goodWeek = spent <= budgeted,
                budgeted = budgeted,
                spent = spent,
                settledAt = input.settledAt,
                bestCategoryId =
                    categories
                        .filter { it.surplus.isPositive }
                        .maxByOrNull { it.surplus.minor }
                        ?.categoryId,
                worstCategoryId =
                    categories
                        .filter {
                            it.overdraw.isPositive
                        }.maxByOrNull { it.overdraw.minor }
                        ?.categoryId,
            )
        return SettlementResult(settlement, categories)
    }

    /**
     * The confidence factor (`docs/02-game-design.md` §3.3).
     *
     * The app cannot verify spending and deliberately does not demand a daily confirmation tap, so a
     * silent week would otherwise look exactly like a perfect one. The fix is economic rather than
     * procedural: silence still pays — just never in full.
     */
    fun confidenceBasisPoints(signalDays: Int): Long {
        val clamped = signalDays.coerceIn(0, Week.DAYS)
        return balance.confidenceFloorBasisPoints +
            IntegerMath.roundHalfUpDiv(balance.confidenceSpanBasisPoints * clamped, Week.DAYS.toLong())
    }

    /**
     * The streak factor (`docs/02-game-design.md` §3.4): +5% per consecutive good week, capped at
     * +50%. A bad week resets the multiplier — it never removes EP already earned.
     */
    fun streakBasisPoints(consecutiveGoodWeeks: Int): Long {
        val bonus =
            (balance.streakStepBasisPoints * consecutiveGoodWeeks.coerceAtLeast(0))
                .coerceAtMost(balance.streakCapBasisPoints)
        return IntegerMath.ONE_BP + bonus
    }

    /** Days in the week carrying a signal: a logged expense, or an explicit zero-spend mark. */
    fun signalDays(input: SettlementInput): Int {
        val expenseDays = input.expenses.filter { it.occurredOn in input.week }.map { it.occurredOn }
        val markedDays = input.zeroSpendDates.filter { it in input.week }
        return (expenseDays + markedDays).distinct().size
    }

    /** Converts saved money into power. */
    fun toEp(amount: Money): Long =
        IntegerMath.roundHalfUpDiv(amount.minor * balance.epPerMajorUnit, Money.MINOR_PER_MAJOR)

    private fun perCategory(input: SettlementInput): List<CategorySettlement> {
        val weekExpenses = input.expenses.filter { it.occurredOn in input.week }
        val spentByCategory =
            weekExpenses
                .groupBy { it.categoryId }
                .mapValues { (_, expenses) -> expenses.map { it.amount }.sum() }
        val budgetedByCategory = ProrationCalculator.weeklyBudgets(input.week, input.budgets)

        return (budgetedByCategory.keys + spentByCategory.keys)
            .sorted()
            .map { categoryId ->
                CategorySettlement(
                    categoryId = categoryId,
                    budgeted = budgetedByCategory[categoryId] ?: Money.ZERO,
                    spent = spentByCategory[categoryId] ?: Money.ZERO,
                )
            }
    }
}
