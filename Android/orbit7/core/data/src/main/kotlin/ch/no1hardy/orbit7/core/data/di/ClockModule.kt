package ch.no1hardy.orbit7.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Singleton

/**
 * The only place in the whole app that is allowed to read the wall clock.
 *
 * Hard rule 1 (`docs/05-architecture.md` §5) is enforced by the `NoDirectClockAccess` detekt rule,
 * which exempts exactly this file. Everything else takes a [Clock] and is therefore testable on any
 * date — including the Sundays Europe/Zurich changes its offset.
 */
@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
    @Provides
    @Singleton
    fun provideClock(timeTravel: TimeTravel): Clock = TimeTravelClock(Clock.systemDefaultZone(), timeTravel)
}

/**
 * The system clock, plus whatever offset the debug time-travel screen has applied.
 *
 * In a release build [TimeTravel] is never written to, so this is the system clock with an
 * unconditional `plus(ZERO)`.
 */
private class TimeTravelClock(
    private val delegate: Clock,
    private val timeTravel: TimeTravel,
) : Clock() {
    override fun getZone(): ZoneId = delegate.zone

    override fun withZone(zone: ZoneId): Clock = TimeTravelClock(delegate.withZone(zone), timeTravel)

    override fun instant(): Instant = delegate.instant().plus(offset())

    private fun offset(): Duration = timeTravel.current
}
