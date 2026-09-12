package ch.no1hardy.orbit7.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ch.no1hardy.orbit7.core.data.work.Orbit7WorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Re-registers the three workers after a reboot.
 *
 * Without this the settlement would simply stop happening on a phone that was restarted, and the
 * user would never know why their weeks stopped paying out.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: Orbit7WorkScheduler

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                scheduler.scheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
