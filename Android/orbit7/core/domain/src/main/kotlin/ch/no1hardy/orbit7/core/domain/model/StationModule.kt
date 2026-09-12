package ch.no1hardy.orbit7.core.domain.model

import java.time.Instant

/** The player's progress on one station module. */
data class StationModuleState(
    val moduleKey: StationModuleKey,
    val level: Int,
    val unlockedAt: Instant?,
    val lastUpgradedAt: Instant?,
) {
    init {
        require(level >= 0) { "A module level cannot be negative" }
    }

    val isUnlocked: Boolean get() = level > 0
}

/**
 * The seven modules of ORBIT-7 (`docs/02-game-design.md` §5.1).
 *
 * [REACTOR_CORE] must be powered first; everything else can then be bought in any order, so the
 * player always has a choice between a cheap immediate win and a saved-up big one.
 */
enum class StationModuleKey(
    val maxLevel: Int,
    val baseCostEp: Long,
    val unlocks: ModuleUnlock? = null,
) {
    REACTOR_CORE(maxLevel = 5, baseCostEp = 60),
    LIFE_SUPPORT(maxLevel = 4, baseCostEp = 80),
    HYDROPONICS(maxLevel = 4, baseCostEp = 90),
    COMMS_ARRAY(maxLevel = 3, baseCostEp = 140, unlocks = ModuleUnlock.MISSIONS),
    OBSERVATION_DECK(maxLevel = 3, baseCostEp = 160, unlocks = ModuleUnlock.REPORTS),
    HANGAR(maxLevel = 4, baseCostEp = 200, unlocks = ModuleUnlock.SHIP_VARIANTS),
    CRYO_LAB(maxLevel = 3, baseCostEp = 400),
    ;

    /** Everything but the reactor needs the reactor lit first. */
    val prerequisite: StationModuleKey? get() = if (this == REACTOR_CORE) null else REACTOR_CORE
}

/** A feature a module switches on when it reaches level 1. */
enum class ModuleUnlock {
    MISSIONS,
    REPORTS,
    SHIP_VARIANTS,
}
