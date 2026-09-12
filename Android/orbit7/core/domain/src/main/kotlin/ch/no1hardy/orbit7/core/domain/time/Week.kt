package ch.no1hardy.orbit7.core.domain.time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * A settlement period: seven whole days, identified by its first day.
 *
 * The default period is Monday 00:00 to Sunday 23:59:59.999 in the user's own timezone
 * (`docs/02-game-design.md` §3); the first day is configurable in Settings, so it is a parameter
 * here rather than a constant.
 *
 * A week is always exactly seven calendar days — a DST changeover shortens or lengthens one day but
 * never changes the number of days a week's budget is prorated over.
 */
@JvmInline
value class Week(
    val start: LocalDate,
) : Comparable<Week> {
    val endInclusive: LocalDate get() = start.plusDays(DAYS - 1L)

    val days: List<LocalDate> get() = (0 until DAYS).map { start.plusDays(it.toLong()) }

    /** Stable identifier used as the settlement primary key and as a ledger reference. */
    val id: String get() = start.toString()

    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(endInclusive)

    fun next(): Week = Week(start.plusWeeks(1))

    fun previous(): Week = Week(start.minusWeeks(1))

    fun minusWeeks(count: Int): Week = Week(start.minusWeeks(count.toLong()))

    override fun compareTo(other: Week): Int = start.compareTo(other.start)

    override fun toString(): String = "Week($start)"

    companion object {
        const val DAYS = 7

        /** The week that contains [date], given the configured [firstDayOfWeek]. */
        fun containing(
            date: LocalDate,
            firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
        ): Week = Week(date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek)))

        /** Every complete week from [from] up to but excluding the week containing [exclusiveEnd]. */
        fun completeWeeksBetween(
            from: Week,
            exclusiveEnd: Week,
        ): List<Week> {
            if (from >= exclusiveEnd) return emptyList()
            val weeks = mutableListOf<Week>()
            var cursor = from
            while (cursor < exclusiveEnd) {
                weeks += cursor
                cursor = cursor.next()
            }
            return weeks
        }
    }
}

/** Parses a [Week] back from its [Week.id]. */
fun weekOfId(id: String): Week = Week(LocalDate.parse(id))
