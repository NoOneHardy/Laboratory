package ch.no1hardy.orbit7.core.domain.usecase

import ch.no1hardy.orbit7.core.domain.economy.SettlementCalculator
import ch.no1hardy.orbit7.core.domain.economy.SettlementInput
import ch.no1hardy.orbit7.core.domain.model.EnergyReason
import ch.no1hardy.orbit7.core.domain.model.WeeklySettlement
import ch.no1hardy.orbit7.core.domain.repository.BudgetRepository
import ch.no1hardy.orbit7.core.domain.repository.EnergyRepository
import ch.no1hardy.orbit7.core.domain.repository.ExpenseRepository
import ch.no1hardy.orbit7.core.domain.repository.SettingsRepository
import ch.no1hardy.orbit7.core.domain.repository.SettlementRepository
import ch.no1hardy.orbit7.core.domain.repository.ZeroSpendRepository
import ch.no1hardy.orbit7.core.domain.time.Week
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

/**
 * Settles every complete week that has not been settled yet, oldest first.
 *
 * Two properties matter more than anything else here, and both are covered by the property tests in
 * `docs/06-test-strategy.md` §3:
 *
 * 1. **Idempotence.** A week already settled can never be settled twice, however often WorkManager
 *    fires. The settlement table's primary key is the week start, and the insert is the guard.
 * 2. **Catch-up equals piecemeal.** Settling five missed weeks in one pass produces exactly the
 *    same ledger as settling them one at a time would have — the user who did not open the app for
 *    a month is not punished and not rewarded.
 *
 * The running balance is floored at zero: a bad week can pay nothing, never a debt, and EP already
 * spent on modules is never reclaimed (`docs/02-game-design.md` §3.1).
 */
class SettleDueWeeksUseCase
    @Inject
    constructor(
        private val expenses: ExpenseRepository,
        private val budgets: BudgetRepository,
        private val zeroSpend: ZeroSpendRepository,
        private val settlements: SettlementRepository,
        private val energy: EnergyRepository,
        private val settings: SettingsRepository,
        private val calculator: SettlementCalculator,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(): List<WeeklySettlement> {
            val today = LocalDate.now(clock)
            val firstDayOfWeek = settings.settings().firstDayOfWeek
            val currentWeek = Week.containing(today, firstDayOfWeek)

            val startWeek = startWeek(currentWeek, firstDayOfWeek) ?: return emptyList()
            val due = Week.completeWeeksBetween(startWeek, currentWeek)
            if (due.isEmpty()) return emptyList()

            val settled = mutableListOf<WeeklySettlement>()
            var consecutiveGoodWeeks = consecutiveGoodWeeksBefore(startWeek)

            for (week in due) {
                if (settlements.settlement(week) != null) {
                    consecutiveGoodWeeks =
                        if (settlements.settlement(week)?.goodWeek == true) {
                            consecutiveGoodWeeks + 1
                        } else {
                            0
                        }
                    continue
                }

                val result =
                    calculator.settle(
                        SettlementInput(
                            week = week,
                            budgets = budgets.versionsCovering(week.start, week.endInclusive),
                            expenses = expenses.between(week.start, week.endInclusive),
                            zeroSpendDates = zeroSpend.between(week.start, week.endInclusive).map { it.date }.toSet(),
                            consecutiveGoodWeeksBefore = consecutiveGoodWeeks,
                            settledAt = clock.instant(),
                        ),
                    )

                val stored = settlements.insert(result.settlement, result.categories)
                if (!stored) continue // Another worker settled this week first; nothing more to do.

                appendLedgerEntry(result.settlement)
                settled += result.settlement
                consecutiveGoodWeeks = if (result.settlement.goodWeek) consecutiveGoodWeeks + 1 else 0
            }
            return settled
        }

        /** Records the week's outcome as a single ledger row, clamped so the balance never goes below zero. */
        private suspend fun appendLedgerEntry(settlement: WeeklySettlement) {
            val awarded = settlement.awardedEp
            if (awarded == 0) return
            val balance = energy.balance()
            val delta = if (awarded < 0) maxOf(awarded, -balance) else awarded
            if (delta == 0) return
            energy.append(
                delta = delta,
                reason = if (delta >= 0) EnergyReason.SETTLEMENT else EnergyReason.OVERDRAW,
                referenceId = settlement.week.id,
                at = settlement.settledAt,
            )
        }

        /**
         * The first week that still needs settling.
         *
         * After the last settled week, normally. On a fresh install it is the week of the first signal
         * the app ever saw — an expense, a zero-spend mark, or failing both, the day budgeting started
         * at onboarding, so a disciplined week that happened to contain no spending still pays.
         *
         * The catch-up is capped at [MAX_CATCH_UP_WEEKS]: an import carrying years-old budgets must not
         * make the worker replay half a decade of empty weeks.
         */
        private suspend fun startWeek(
            currentWeek: Week,
            firstDayOfWeek: DayOfWeek,
        ): Week? {
            val floor = currentWeek.minusWeeks(MAX_CATCH_UP_WEEKS)
            settlements.latest()?.let { return maxOf(it.week.next(), floor) }
            val anchor =
                listOfNotNull(
                    expenses.earliestExpenseDate(),
                    zeroSpend.earliestMarkDate(),
                    budgets.earliestValidFrom(),
                ).minOrNull() ?: return null
            return maxOf(Week.containing(anchor, firstDayOfWeek), floor).takeIf { it < currentWeek }
        }

        private companion object {
            /** A year of catch-up is plenty; beyond that the weeks are archaeology, not gameplay. */
            const val MAX_CATCH_UP_WEEKS = 52
        }

        private suspend fun consecutiveGoodWeeksBefore(week: Week): Int {
            val history = settlements.settlements().filter { it.week < week }.sortedByDescending { it.week }
            var count = 0
            for (settlement in history) {
                if (!settlement.goodWeek) break
                count++
            }
            return count
        }
    }
