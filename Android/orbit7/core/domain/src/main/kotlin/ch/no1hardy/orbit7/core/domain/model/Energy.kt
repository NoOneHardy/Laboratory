package ch.no1hardy.orbit7.core.domain.model

import java.time.Instant

/**
 * One row of the energy ledger.
 *
 * Hard rule 3 from `docs/05-architecture.md` §5: **EP is only ever changed by appending a row
 * here.** The balance is the sum of the ledger; there is no mutable balance field that can drift
 * out of sync, which removes the entire class of "my points disappeared" bugs and makes the economy
 * replayable in tests.
 */
data class EnergyLedgerEntry(
    val id: Long,
    val delta: Int,
    val reason: EnergyReason,
    val referenceId: String?,
    val occurredAt: Instant,
)

enum class EnergyReason {
    SETTLEMENT,
    OVERDRAW,
    SALVAGE,
    MISSION,
    MODULE_PURCHASE,
    CORRECTION,
}
