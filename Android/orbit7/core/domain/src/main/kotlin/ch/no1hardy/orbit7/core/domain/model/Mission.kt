package ch.no1hardy.orbit7.core.domain.model

import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.time.Week

/**
 * A weekly challenge generated from the user's own data (`docs/02-game-design.md` §7).
 *
 * Missions are opt-in, never auto-assigned, and a failed mission costs nothing — the generator is a
 * pure function so it can be tested against a fixed history fixture.
 */
data class Mission(
    val id: String,
    val week: Week,
    val kind: MissionKind,
    val categoryId: Long? = null,
    val targetAmount: Money? = null,
    val targetCount: Int? = null,
    val payoutEp: Int,
    val status: MissionStatus = MissionStatus.OFFERED,
)

enum class MissionKind {
    /** "Keep Eating out under CHF 25 this week." */
    CATEGORY_CAP,

    /** "Three zero-spend days." */
    ZERO_SPEND_DAYS,

    /** "No delivery charges for 7 days." */
    AVOID_NOTE_PATTERN,
}

enum class MissionStatus {
    OFFERED,
    ACCEPTED,
    COMPLETED,
    FAILED,
}
