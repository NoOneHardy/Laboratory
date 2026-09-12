package ch.no1hardy.orbit7

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import ch.no1hardy.orbit7.core.data.work.Orbit7WorkScheduler
import ch.no1hardy.orbit7.core.domain.repository.SettingsRepository
import ch.no1hardy.orbit7.notification.Orbit7NotificationChannels
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Application entry point.
 *
 * Two things happen on every start: the three workers are re-registered (they must survive reboot
 * and Doze), and the settlement catch-up runs — so a user returning after five weeks sees their
 * power immediately rather than next Monday.
 */
@HiltAndroidApp
class Orbit7Application :
    Application(),
    Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var scheduler: Orbit7WorkScheduler

    @Inject lateinit var settings: SettingsRepository

    @Inject lateinit var clock: Clock

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun onCreate() {
        super.onCreate()
        Orbit7NotificationChannels.ensure(this)

        applicationScope.launch {
            // The daily reminder is skipped on days the app was used at all; this is that record.
            settings.update { it.copy(lastAppUsageOn = LocalDate.now(clock)) }
            scheduler.scheduleAll()
        }
    }
}
