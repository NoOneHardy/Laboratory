package ch.no1hardy.orbit7.core.domain.usecase

import ch.no1hardy.orbit7.core.domain.economy.ContractSchedule
import ch.no1hardy.orbit7.core.domain.economy.SalvageCalculator
import ch.no1hardy.orbit7.core.domain.model.Contract
import ch.no1hardy.orbit7.core.domain.model.ContractStatus
import ch.no1hardy.orbit7.core.domain.model.EnergyReason
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.repository.ContractRepository
import ch.no1hardy.orbit7.core.domain.repository.EnergyRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** The outcome of cutting a drain. */
sealed interface SalvageResult {
    data class Paid(
        val contractId: Long,
        val ep: Int,
        val monthlySaving: Money,
    ) : SalvageResult

    data object AlreadySalvaged : SalvageResult

    data object NotFound : SalvageResult

    data object NoSaving : SalvageResult
}

/**
 * Cancels a contract and pays the salvage (`docs/02-game-design.md` §6.2).
 *
 * The guardrail is here: salvage is paid **once per contract per state transition**, recorded in the
 * ledger with the contract id as reference, so a status cannot be toggled back and forth to farm
 * payouts. A cancelled contract that comes back is entered as a new contract, which pays nothing.
 */
class CancelContractUseCase
    @Inject
    constructor(
        private val contracts: ContractRepository,
        private val energy: EnergyRepository,
        private val salvage: SalvageCalculator,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(contractId: Long): SalvageResult {
            val contract = contracts.contract(contractId) ?: return SalvageResult.NotFound
            if (!contract.isActive) return SalvageResult.AlreadySalvaged
            val reference = salvageReference(contractId, ContractStatus.CANCELLED)
            if (energy.hasEntryFor(EnergyReason.SALVAGE, reference)) return SalvageResult.AlreadySalvaged

            val payout = salvage.cancellationPayout(contract)
            val today = LocalDate.now(clock)
            contracts.updateStatus(contractId, ContractStatus.CANCELLED, today, newAmount = null)
            if (payout > 0) {
                energy.append(payout, EnergyReason.SALVAGE, reference, clock.instant())
            }
            return SalvageResult.Paid(contractId, payout, contract.monthlyEquivalent)
        }
    }

/** Renegotiates a contract down to a new amount and pays salvage on the delta only. */
class RenegotiateContractUseCase
    @Inject
    constructor(
        private val contracts: ContractRepository,
        private val energy: EnergyRepository,
        private val salvage: SalvageCalculator,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(
            contractId: Long,
            newAmount: Money,
        ): SalvageResult {
            val contract = contracts.contract(contractId) ?: return SalvageResult.NotFound
            if (!contract.isActive) return SalvageResult.AlreadySalvaged
            if (newAmount >= contract.amount) return SalvageResult.NoSaving

            val reference = salvageReference(contractId, ContractStatus.RENEGOTIATED)
            if (energy.hasEntryFor(EnergyReason.SALVAGE, reference)) return SalvageResult.AlreadySalvaged

            val payout = salvage.renegotiationPayout(contract.amount, newAmount, contract.cadence)
            val today = LocalDate.now(clock)
            contracts.updateStatus(contractId, ContractStatus.RENEGOTIATED, today, newAmount)
            if (payout > 0) {
                energy.append(payout, EnergyReason.SALVAGE, reference, clock.instant())
            }
            val saving =
                Money(contract.monthlyEquivalent.minor) -
                    Money(newAmount.minor / contract.cadence.months)
            return SalvageResult.Paid(contractId, payout, saving.coerceAtLeastZero())
        }
    }

/** Creates or edits a contract, always deriving the cancellation deadline from the notice period. */
class SaveContractUseCase
    @Inject
    constructor(
        private val contracts: ContractRepository,
        private val schedule: ContractSchedule,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(
            contract: Contract,
            overrideCancellationDate: LocalDate? = null,
        ): Long {
            val derived =
                overrideCancellationDate
                    ?: schedule.earliestCancellationOn(contract.nextChargeOn, contract.noticePeriod)
            return contracts.upsert(
                contract.copy(
                    earliestCancellationOn = derived,
                    createdAt = if (contract.id == 0L) clock.instant() else contract.createdAt,
                ),
            )
        }
    }

/** Ledger reference for a salvage payout. One per contract per transition — never two. */
internal fun salvageReference(
    contractId: Long,
    status: ContractStatus,
): String = "contract:$contractId:${status.name}"
