package ch.no1hardy.orbit7.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hosts the widget and refreshes it.
 *
 * Updates happen on data change (the app calls [refresh]) and at most every 30 minutes otherwise,
 * which the widget info XML declares.
 */
@AndroidEntryPoint
class Orbit7WidgetReceiver : GlanceAppWidgetReceiver() {
    @Inject lateinit var repository: Orbit7WidgetRepository

    override val glanceAppWidget: GlanceAppWidget
        get() = Orbit7Widget { repository.widgetState() }

    companion object {
        /** Called after an expense is logged or a week is settled. */
        fun refresh(
            context: Context,
            repository: Orbit7WidgetRepository,
        ) {
            CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                Orbit7Widget { repository.widgetState() }.updateAll(context)
            }
        }
    }
}
