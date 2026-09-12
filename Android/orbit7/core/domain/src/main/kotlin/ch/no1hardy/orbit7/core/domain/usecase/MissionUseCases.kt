package ch.no1hardy.orbit7.core.domain.usecase

import ch.no1hardy.orbit7.core.domain.economy.BaselineCalculator
import ch.no1hardy.orbit7.core.domain.economy.MissionContext
import ch.no1hardy.orbit7.core.domain.economy.MissionEvaluator
import ch.no1hardy.orbit7.core.domain.economy.MissionGenerator
import ch.no1hardy.orbit7.core.domain.economy.StationCatalog
import ch.no1hardy.orbit7.core.domain.model.EnergyReason
import ch.no1hardy.orbit7.core.domain.model.Mission
import ch.no1hardy.orbit7.core.domain.model.MissionStatus
import ch.no1hardy.orbit7.core.domain.model.ModuleUnlock
import ch.no1hardy.orbit7.core.domain.repository.EnergyRepository
import ch.no1hardy.orbit7.core.domain.repository.ExpenseRepository
import ch.no1hardy.orbit7.core.domain.repository.MissionRepository
import ch.no1hardy.orbit7.core.domain.repository.SettingsRepository
import ch.no1hardy.orbit7.core.domain.repository.StationRepository
import ch.no1hardy.orbit7.core.domain.repository.ZeroSpendRepository
import ch.no1hardy.orbit7.core.domain.time.Week
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Refreshes the weekly mission offer, if the Comms Array is powered
 * (`docs/02-game-design.md` §7).
 *
 * Missions are offered, never assigned; three at a time, and picking none is a valid week.
 */
class RefreshMissionOfferUseCase
    @Inject
    constructor(
        private val missions: MissionRepository,
        private val expenses: ExpenseRepository,
        private val zeroSpend: ZeroSpendRepository,
        private val station: StationRepository,
        private val settings: SettingsRepository,
        private val catalog: StationCatalog,
        private val baselines: BaselineCalculator,
        private val generator: MissionGenerator,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(): List<Mission> {
            val states = station.modules().associateBy { it.moduleKey }
            if (!catalog.isUnlocked(ModuleUnlock.MISSIONS, states)) return emptyList()

            val today = LocalDate.now(clock)
            val firstDayOfWeek = settings.settings().firstDayOfWeek
            val week = Week.containing(today, firstDayOfWeek)
            missions.missions(week).takeIf { it.isNotEmpty() }?.let { return it }

            val previous = week.previous()
            val previousExpenses = expenses.between(previous.start, previous.endInclusive)
            val allExpenses = expenses.between(today.minusDays(HISTORY_DAYS), today)
            val baselineByCategory =
                allExpenses
                    .map { it.categoryId }
                    .distinct()
                    .mapNotNull { categoryId ->
                        baselines.baseline(categoryId, allExpenses, today, firstDayOfWeek)?.let { categoryId to it }
                    }.toMap()

            val offer =
                generator.generate(
                    MissionContext(
                        week = week,
                        baselines = baselineByCategory,
                        previousWeekExpenses = previousExpenses,
                        previousWeekZeroSpendDays = zeroSpend.between(previous.start, previous.endInclusive).size,
                    ),
                )
            missions.replaceOffer(week, offer)
            return offer
        }

        private companion object {
            const val HISTORY_DAYS = 120L
        }
    }

/** Accepts one of the offered missions. At most one is active at a time. */
class AcceptMissionUseCase
    @Inject
    constructor(
        private val missions: MissionRepository,
    ) {
        suspend operator fun invoke(
            week: Week,
            missionId: String,
        ) {
            missions.missions(week).forEach { mission ->
                val status =
                    when {
                        mission.id == missionId -> MissionStatus.ACCEPTED
                        mission.status == MissionStatus.ACCEPTED -> MissionStatus.OFFERED
                        else -> mission.status
                    }
                if (status != mission.status) missions.updateStatus(mission.id, status)
            }
        }
    }

/** Resolves an accepted mission at week close. A failed mission costs nothing. */
class ResolveMissionUseCase
    @Inject
    constructor(
        private val missions: MissionRepository,
        private val expenses: ExpenseRepository,
        private val zeroSpend: ZeroSpendRepository,
        private val energy: EnergyRepository,
        private val evaluator: MissionEvaluator,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(week: Week): Mission? {
            val accepted = missions.missions(week).firstOrNull { it.status == MissionStatus.ACCEPTED } ?: return null
            val weekExpenses = expenses.between(week.start, week.endInclusive)
            val marks = zeroSpend.between(week.start, week.endInclusive).map { it.date }.toSet()

            val completed = evaluator.isCompleted(accepted, weekExpenses, marks)
            missions.updateStatus(
                accepted.id,
                if (completed) MissionStatus.COMPLETED else MissionStatus.FAILED,
            )
            if (completed && !energy.hasEntryFor(EnergyReason.MISSION, accepted.id)) {
                energy.append(accepted.payoutEp, EnergyReason.MISSION, accepted.id, clock.instant())
            }
            return accepted.copy(status = if (completed) MissionStatus.COMPLETED else MissionStatus.FAILED)
        }
    }
