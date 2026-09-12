package ch.no1hardy.orbit7.core.data.notification

import ch.no1hardy.orbit7.core.domain.model.Contract
import ch.no1hardy.orbit7.core.domain.money.Money

/**
 * Posting notifications.
 *
 * The implementation lives in `:app`, which owns the activity the notifications open; this module
 * only decides *when* one is warranted. The ethics line from `docs/02-game-design.md` §8 is a
 * constraint on this interface: at most one scheduled notification per day, plus contract deadlines,
 * which are real-world deadlines with money attached.
 */
interface Orbit7Notifier {
    /** The daily reminder, with its quick-add and "nothing spent today" actions. */
    fun postDailyReminder(
        remainingThisWeek: Money,
        spentToday: Money,
    )

    /** A contract deadline at the 30, 14 or 3 day threshold. */
    fun postContractDeadline(
        contract: Contract,
        daysRemaining: Int,
    )

    /** A settled week is waiting to be looked at. Posted at most once per week. */
    fun postSettlementReady(awardedEp: Int)

    fun cancelDailyReminder()
}
