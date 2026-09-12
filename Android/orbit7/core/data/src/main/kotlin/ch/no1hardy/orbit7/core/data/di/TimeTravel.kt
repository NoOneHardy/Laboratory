package ch.no1hardy.orbit7.core.data.di

import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The offset the debug time-travel screen applies to the app's [java.time.Clock]
 * (`docs/02-game-design.md` §8).
 *
 * It exists in every build type so the clock has a single implementation, but only the debug-only
 * time-travel screen ever writes to it — in a release build the offset is zero for the life of the
 * process, and the clock is the system clock.
 */
@Singleton
class TimeTravel
    @Inject
    constructor() {
        private val offset = AtomicReference(Duration.ZERO)

        val current: Duration get() = offset.get()

        fun advance(by: Duration) {
            offset.updateAndGet { it.plus(by) }
        }

        fun reset() {
            offset.set(Duration.ZERO)
        }
    }
