package ch.no1hardy.orbit7.core.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ch.no1hardy.orbit7.core.data.db.dao.ContractDao
import ch.no1hardy.orbit7.core.data.db.entity.ContractNotificationEntity
import ch.no1hardy.orbit7.core.data.notification.Orbit7Notifier
import ch.no1hardy.orbit7.core.domain.economy.ContractSchedule
import ch.no1hardy.orbit7.core.domain.repository.ContractRepository
import ch.no1hardy.orbit7.core.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import java.time.LocalDate

/**
 * Contract deadline reminders at 30 / 14 / 3 days (`docs/02-game-design.md` §6.3).
 *
 * These are the only notifications allowed to exceed one per day, because they are real-world
 * deadlines with money attached. Each threshold fires exactly once per contract: what has already
 * been posted is recorded in the database, not inferred.
 */
@HiltWorker
class ContractDeadlineWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted parameters: WorkerParameters,
        private val contracts: ContractRepository,
        private val contractDao: ContractDao,
        private val settings: SettingsRepository,
        private val schedule: ContractSchedule,
        private val notifier: Orbit7Notifier,
        private val clock: Clock,
    ) : CoroutineWorker(context, parameters) {
        override suspend fun doWork(): Result {
            if (!settings.settings().contractRemindersEnabled) return Result.success()

            val today = LocalDate.now(clock)
            contracts.contracts().filter { it.isActive }.forEach { contract ->
                val alreadyNotified = contractDao.notifiedThresholds(contract.id).toSet()
                val due = schedule.dueReminderThreshold(contract, today, alreadyNotified) ?: return@forEach

                notifier.postContractDeadline(
                    contract = contract,
                    daysRemaining = schedule.daysUntilDeadline(contract, today).toInt(),
                )
                contractDao.recordNotification(
                    ContractNotificationEntity(
                        contractId = contract.id,
                        thresholdDays = due,
                        notifiedOn = today,
                    ),
                )
            }
            return Result.success()
        }

        companion object {
            const val UNIQUE_NAME = "orbit7-contract-deadlines"
        }
    }
