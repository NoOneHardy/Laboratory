package ch.no1hardy.orbit7.core.domain.economy

import ch.no1hardy.orbit7.core.domain.GameBalance
import ch.no1hardy.orbit7.core.domain.model.ModuleUnlock
import ch.no1hardy.orbit7.core.domain.model.StationModuleKey
import ch.no1hardy.orbit7.core.domain.model.StationModuleState
import ch.no1hardy.orbit7.core.domain.money.IntegerMath

/** What the player can do with a module right now. */
data class ModuleOffer(
    val moduleKey: StationModuleKey,
    val level: Int,
    val nextLevel: Int?,
    val nextLevelCostEp: Long?,
    val affordable: Boolean,
    val prerequisiteMet: Boolean,
    val epShortfall: Long,
) {
    val isMaxed: Boolean get() = nextLevel == null
    val canPurchase: Boolean get() = !isMaxed && affordable && prerequisiteMet
}

/**
 * The station's cost curve (`docs/02-game-design.md` §5.2).
 *
 * ```
 * cost(n) = baseCost × growth^(n−1)     growth = 1.8, rounded to the nearest 10 EP
 * ```
 *
 * Computed in integer arithmetic from the growth factor in basis points, and asserted against the
 * cost table published in the game design document — so a rebalancing cannot silently contradict
 * the documentation.
 */
class StationCatalog(
    private val balance: GameBalance = GameBalance.DEFAULT,
) {
    /** The EP cost of reaching [level] (1-based) of [moduleKey]. */
    fun costOfLevel(
        moduleKey: StationModuleKey,
        level: Int,
    ): Long {
        require(level in 1..moduleKey.maxLevel) {
            "${moduleKey.name} has no level $level (max ${moduleKey.maxLevel})"
        }
        val steps = level - 1
        val numerator = moduleKey.baseCostEp * IntegerMath.pow(balance.moduleCostGrowthNumerator, steps)
        val denominator =
            IntegerMath.pow(balance.moduleCostGrowthDenominator, steps) *
                balance.moduleCostRoundingStep
        return IntegerMath.roundHalfUpDiv(numerator, denominator) * balance.moduleCostRoundingStep
    }

    /** The full cost curve of a module, level 1 upward. */
    fun costs(moduleKey: StationModuleKey): List<Long> = (1..moduleKey.maxLevel).map { costOfLevel(moduleKey, it) }

    /** What every module currently offers, given the player's [states] and EP [balanceEp]. */
    fun offers(
        states: Map<StationModuleKey, StationModuleState>,
        balanceEp: Long,
    ): List<ModuleOffer> = StationModuleKey.entries.map { key -> offer(key, states, balanceEp) }

    fun offer(
        moduleKey: StationModuleKey,
        states: Map<StationModuleKey, StationModuleState>,
        balanceEp: Long,
    ): ModuleOffer {
        val level = states[moduleKey]?.level ?: 0
        val nextLevel = (level + 1).takeIf { it <= moduleKey.maxLevel }
        val cost = nextLevel?.let { costOfLevel(moduleKey, it) }
        val prerequisiteMet = moduleKey.prerequisite?.let { (states[it]?.level ?: 0) > 0 } ?: true
        return ModuleOffer(
            moduleKey = moduleKey,
            level = level,
            nextLevel = nextLevel,
            nextLevelCostEp = cost,
            affordable = cost != null && balanceEp >= cost,
            prerequisiteMet = prerequisiteMet,
            epShortfall = cost?.minus(balanceEp)?.coerceAtLeast(0) ?: 0,
        )
    }

    /** The cheapest thing the player could work toward next — the Station screen's "X EP to go". */
    fun nearestGoal(
        states: Map<StationModuleKey, StationModuleState>,
        balanceEp: Long,
    ): ModuleOffer? =
        offers(states, balanceEp)
            .filter { !it.isMaxed && it.prerequisiteMet }
            .minByOrNull { it.nextLevelCostEp ?: Long.MAX_VALUE }

    /** Whether a feature-gating module has been powered at all. */
    fun isUnlocked(
        unlock: ModuleUnlock,
        states: Map<StationModuleKey, StationModuleState>,
    ): Boolean =
        StationModuleKey.entries
            .filter { it.unlocks == unlock }
            .any { (states[it]?.level ?: 0) > 0 }

    /** Total EP sunk into the station, used by Reports. */
    fun investedEp(states: Map<StationModuleKey, StationModuleState>): Long =
        states.values.sumOf { state -> (1..state.level).sumOf { costOfLevel(state.moduleKey, it) } }
}
