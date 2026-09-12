package ch.no1hardy.orbit7.core.domain.test

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The single source of time in every test.
 *
 * Hard rule 1 (`docs/05-architecture.md` §5) is what makes this possible: production code never
 * reads the wall clock, so a test can put the app on any Monday it likes — including the two
 * Sundays a year on which Europe/Zurich changes its offset.
 */
class FakeClock(
    private var now: Instant = Instant.parse("2025-04-14T08:00:00Z"),
    private val zone: ZoneId = ZoneId.of("Europe/Zurich"),
) : Clock() {
    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = FakeClock(now, zone)

    override fun instant(): Instant = now

    fun setInstant(instant: Instant) {
        now = instant
    }

    /** Moves the clock to 08:00 local time on [date]. */
    fun setDate(
        date: LocalDate,
        time: LocalTime = LocalTime.of(8, 0),
    ) {
        now = date.atTime(time).atZone(zone).toInstant()
    }

    fun advance(duration: Duration) {
        now = now.plus(duration)
    }

    fun advanceDays(days: Long) = advance(Duration.ofDays(days))

    fun advanceWeeks(weeks: Long) = advanceDays(weeks * DAYS_PER_WEEK)

    fun today(): LocalDate = LocalDate.ofInstant(now, zone)

    private companion object {
        const val DAYS_PER_WEEK = 7L
    }
}
