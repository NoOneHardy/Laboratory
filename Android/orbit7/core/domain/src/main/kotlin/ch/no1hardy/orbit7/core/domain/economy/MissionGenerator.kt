package ch.no1hardy.orbit7.core.domain.economy

import ch.no1hardy.orbit7.core.domain.GameBalance
import ch.no1hardy.orbit7.core.domain.model.Expense
import ch.no1hardy.orbit7.core.domain.model.Mission
import ch.no1hardy.orbit7.core.domain.model.MissionKind
import ch.no1hardy.orbit7.core.domain.model.MissionStatus
import ch.no1hardy.orbit7.core.domain.money.IntegerMath
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.money.sum
import ch.no1hardy.orbit7.core.domain.time.Week
import java.time.LocalDate

/** The history a mission offer is generated from. A fixed fixture in tests. */
data class MissionContext(
    val week: Week,
    val baselines: Map<Long, Money>,
    val previousWeekExpenses: List<Expense>,
    val previousWeekZeroSpendDays: Int,
)

/**
 * Generates the weekly mission offer (`docs/02-game-design.md` §7).
 *
 * Three missions are offered at a time and the player picks one — or none. Missions are derived from
 * the player's own data, never from a difficulty table, so a challenge is always within reach of the
 * person actually holding the phone. A failed mission costs nothing.
 *
 * Deterministic by construction: the same context produces the same offer, which is what makes it
 * testable and what keeps a re-render from reshuffling the cards.
 */
class MissionGenerator(
    private val balance: GameBalance = GameBalance.DEFAULT,
) {
    fun generate(context: MissionContext): List<Mission> =
        listOfNotNull(
            categoryCap(context),
            zeroSpendDays(context),
            avoidPattern(context),
        ).take(balance.missionOfferSize)

    /** "Keep Eating out under CHF 25 this week" — the baseline, cut by a quarter. */
    private fun categoryCap(context: MissionContext): Mission? {
        val (categoryId, baseline) =
            context.baselines
                .filterValues { it.isPositive }
                .maxByOrNull { it.value.minor } ?: return null
        val target = Money(IntegerMath.applyBasisPoints(baseline.minor, CATEGORY_CAP_BASIS_POINTS))
        return Mission(
            id = idOf(context.week, MissionKind.CATEGORY_CAP, categoryId.toString()),
            week = context.week,
            kind = MissionKind.CATEGORY_CAP,
            categoryId = categoryId,
            targetAmount = target,
            payoutEp = balance.missionPayoutMedium,
        )
    }

    /** "Three zero-spend days" — one more than last week managed. */
    private fun zeroSpendDays(context: MissionContext): Mission {
        val target = (context.previousWeekZeroSpendDays + 1).coerceIn(1, MAX_ZERO_SPEND_TARGET)
        return Mission(
            id = idOf(context.week, MissionKind.ZERO_SPEND_DAYS, target.toString()),
            week = context.week,
            kind = MissionKind.ZERO_SPEND_DAYS,
            targetCount = target,
            payoutEp =
                if (target <= EASY_ZERO_SPEND_TARGET) {
                    balance.missionPayoutEasy
                } else {
                    balance.missionPayoutMedium
                },
        )
    }

    /** "No delivery charges for 7 days" — only offered when the pattern actually recurs. */
    private fun avoidPattern(context: MissionContext): Mission? {
        val pattern =
            context.previousWeekExpenses
                .mapNotNull {
                    it.note
                        ?.trim()
                        ?.lowercase()
                        ?.takeIf(String::isNotBlank)
                }.groupingBy { it }
                .eachCount()
                .filterValues { it >= MINIMUM_PATTERN_OCCURRENCES }
                .maxByOrNull { it.value }
                ?.key ?: return null
        return Mission(
            id = idOf(context.week, MissionKind.AVOID_NOTE_PATTERN, pattern),
            week = context.week,
            kind = MissionKind.AVOID_NOTE_PATTERN,
            payoutEp = balance.missionPayoutHard,
        ).copy(status = MissionStatus.OFFERED)
    }

    private fun idOf(
        week: Week,
        kind: MissionKind,
        discriminator: String,
    ): String = "${week.id}:${kind.name}:$discriminator"

    private companion object {
        /** A category cap sits ~25% below the baseline. */
        const val CATEGORY_CAP_BASIS_POINTS = 7_500L
        const val MAX_ZERO_SPEND_TARGET = 5
        const val EASY_ZERO_SPEND_TARGET = 2
        const val MINIMUM_PATTERN_OCCURRENCES = 2
    }
}

/**
 * Decides whether an accepted mission was completed, from the week that actually happened.
 *
 * Pure, like the generator, and evaluated at settlement time.
 */
class MissionEvaluator {
    fun isCompleted(
        mission: Mission,
        weekExpenses: List<Expense>,
        zeroSpendDates: Set<LocalDate>,
    ): Boolean =
        when (mission.kind) {
            MissionKind.CATEGORY_CAP -> {
                val spent =
                    weekExpenses
                        .filter { it.categoryId == mission.categoryId && it.occurredOn in mission.week }
                        .map { it.amount }
                        .sum()
                val target = mission.targetAmount ?: Money.ZERO
                spent <= target
            }

            MissionKind.ZERO_SPEND_DAYS -> {
                val spentDays = weekExpenses.filter { it.occurredOn in mission.week }.map { it.occurredOn }.toSet()
                val quiet = mission.week.days.count { it in zeroSpendDates && it !in spentDays }
                quiet >= (mission.targetCount ?: 0)
            }

            MissionKind.AVOID_NOTE_PATTERN -> {
                val pattern = mission.id.substringAfterLast(':')
                weekExpenses
                    .filter { it.occurredOn in mission.week }
                    .none { it.note?.trim()?.lowercase() == pattern }
            }
        }
}
