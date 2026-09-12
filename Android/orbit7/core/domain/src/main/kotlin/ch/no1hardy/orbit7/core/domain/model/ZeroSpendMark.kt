package ch.no1hardy.orbit7.core.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * An explicit "nothing spent today" mark.
 *
 * This is the cheapest possible way to produce a signal day, and it is what keeps the pure-trust
 * model honest (`docs/02-game-design.md` §3.3). It is offered, never required.
 */
data class ZeroSpendMark(
    val date: LocalDate,
    val markedAt: Instant,
    val source: ZeroSpendSource,
)

enum class ZeroSpendSource {
    APP,
    NOTIFICATION,
}
