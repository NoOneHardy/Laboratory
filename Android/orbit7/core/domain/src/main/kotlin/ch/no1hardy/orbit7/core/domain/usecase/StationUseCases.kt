package ch.no1hardy.orbit7.core.domain.usecase

import ch.no1hardy.orbit7.core.domain.economy.StationCatalog
import ch.no1hardy.orbit7.core.domain.model.EnergyReason
import ch.no1hardy.orbit7.core.domain.model.StationModuleKey
import ch.no1hardy.orbit7.core.domain.repository.EnergyRepository
import ch.no1hardy.orbit7.core.domain.repository.StationRepository
import java.time.Clock
import javax.inject.Inject

/** Why a power-up could not happen, so the UI can say so instead of just disabling a button. */
enum class PurchaseRejection {
    ALREADY_MAX_LEVEL,
    PREREQUISITE_MISSING,
    NOT_ENOUGH_ENERGY,
}

sealed interface PurchaseResult {
    data class Success(
        val moduleKey: StationModuleKey,
        val newLevel: Int,
        val spentEp: Long,
    ) : PurchaseResult

    data class Rejected(
        val reason: PurchaseRejection,
        val shortfallEp: Long = 0,
    ) : PurchaseResult
}

/**
 * Spends EP on a station module (`docs/02-game-design.md` §5.3).
 *
 * Unlocking is instant and irreversible: no refund, no downgrade. The spend is a ledger row like
 * every other EP movement, so the station's cost is always visible in the player's own history.
 */
class PurchaseModuleUpgradeUseCase
    @Inject
    constructor(
        private val station: StationRepository,
        private val energy: EnergyRepository,
        private val catalog: StationCatalog,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(moduleKey: StationModuleKey): PurchaseResult {
            val states = station.modules().associateBy { it.moduleKey }
            val balance = energy.balance().toLong()
            val offer = catalog.offer(moduleKey, states, balance)

            val nextLevel = offer.nextLevel ?: return PurchaseResult.Rejected(PurchaseRejection.ALREADY_MAX_LEVEL)
            if (!offer.prerequisiteMet) return PurchaseResult.Rejected(PurchaseRejection.PREREQUISITE_MISSING)
            val cost = offer.nextLevelCostEp ?: return PurchaseResult.Rejected(PurchaseRejection.ALREADY_MAX_LEVEL)
            if (balance < cost) {
                return PurchaseResult.Rejected(PurchaseRejection.NOT_ENOUGH_ENERGY, cost - balance)
            }

            val now = clock.instant()
            energy.append(
                delta = -cost.toInt(),
                reason = EnergyReason.MODULE_PURCHASE,
                referenceId = "${moduleKey.name}:$nextLevel",
                at = now,
            )
            station.setLevel(moduleKey, nextLevel, now)
            return PurchaseResult.Success(moduleKey, nextLevel, cost)
        }
    }
