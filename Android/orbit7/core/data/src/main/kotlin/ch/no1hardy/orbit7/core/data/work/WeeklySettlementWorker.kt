package ch.no1hardy.orbit7.core.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ch.no1hardy.orbit7.core.data.notification.Orbit7Notifier
import ch.no1hardy.orbit7.core.domain.usecase.RefreshMissionOfferUseCase
import ch.no1hardy.orbit7.core.domain.usecase.ResolveMissionUseCase
import ch.no1hardy.orbit7.core.domain.usecase.SettleDueWeeksUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Settles every complete week that is still outstanding (`docs/05-architecture.md` §6).
 *
 * Runs periodically on Monday morning and again as a catch-up check whenever the app starts, so
 * "the user did not open the app for a month" is an ordinary path rather than an edge case. The
 * work is idempotent per week, so firing twice costs nothing.
 */
@HiltWorker
class WeeklySettlementWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted parameters: WorkerParameters,
        private val settleDueWeeks: SettleDueWeeksUseCase,
        private val resolveMission: ResolveMissionUseCase,
        private val refreshMissions: RefreshMissionOfferUseCase,
        private val notifier: Orbit7Notifier,
    ) : CoroutineWorker(context, parameters) {
        override suspend fun doWork(): Result =
            try {
                val settled = settleDueWeeks()
                settled.forEach { settlement -> resolveMission(settlement.week) }
                refreshMissions()
                if (settled.isNotEmpty()) {
                    notifier.postSettlementReady(settled.sumOf { it.awardedEp })
                }
                Result.success()
            } catch (failure: IllegalStateException) {
                // A settlement that raced with an expense write: retry rather than skip a week.
                Result.retry()
            }

        companion object {
            const val UNIQUE_NAME = "orbit7-weekly-settlement"
            const val CATCH_UP_NAME = "orbit7-settlement-catch-up"
        }
    }
