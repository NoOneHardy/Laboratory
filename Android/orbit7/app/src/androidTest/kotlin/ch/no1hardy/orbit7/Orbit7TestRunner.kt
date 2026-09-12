package ch.no1hardy.orbit7

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Swaps in the Hilt test application, which injects an in-memory database and a controllable
 * `FakeClock` (`docs/06-test-strategy.md` §8).
 */
class Orbit7TestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        loader: ClassLoader?,
        name: String?,
        context: Context?,
    ): Application = super.newApplication(loader, HiltTestApplication::class.java.name, context)
}
