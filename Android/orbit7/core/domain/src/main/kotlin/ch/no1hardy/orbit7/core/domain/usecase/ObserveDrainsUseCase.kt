package ch.no1hardy.orbit7.core.domain.usecase

import ch.no1hardy.orbit7.core.domain.economy.ContractSchedule
import ch.no1hardy.orbit7.core.domain.economy.SalvageCalculator
import ch.no1hardy.orbit7.core.domain.model.Category
import ch.no1hardy.orbit7.core.domain.model.Contract
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.money.sum
import ch.no1hardy.orbit7.core.domain.repository.CategoryRepository
import ch.no1hardy.orbit7.core.domain.repository.ContractRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** One tap on the power bus. */
data class Drain(
    val contract: Contract,
    val category: Category?,
    val monthlyBleed: Money,
    val monthlyBleedEp: Int,
    val daysUntilDeadline: Long,
    val urgent: Boolean,
    val potentialSalvageEp: Int,
)

/** The Drains screen (`docs/03-screens.md` §6). */
data class DrainsState(
    val active: List<Drain>,
    val salvaged: List<Drain>,
    val totalMonthlyBleed: Money,
    val totalMonthlyBleedEp: Int,
    val monthlySavingsAchieved: Money,
)

/**
 * The drains view, sorted by bleed rate descending — the biggest target first, which is the whole
 * point of the screen.
 */
class ObserveDrainsUseCase
    @Inject
    constructor(
        private val contracts: ContractRepository,
        private val categories: CategoryRepository,
        private val schedule: ContractSchedule,
        private val salvage: SalvageCalculator,
        private val clock: Clock,
    ) {
        operator fun invoke(): Flow<DrainsState> {
            val today = LocalDate.now(clock)
            return combine(
                contracts.observeContracts(),
                categories.observeCategories(includeArchived = true),
            ) { allContracts, allCategories ->
                val byId = allCategories.associateBy { it.id }
                val drains =
                    allContracts.map { contract ->
                        Drain(
                            contract = contract,
                            category = byId[contract.categoryId],
                            monthlyBleed = contract.monthlyEquivalent,
                            monthlyBleedEp = salvage.monthlyBleedEp(listOf(contract)),
                            daysUntilDeadline = schedule.daysUntilDeadline(contract, today),
                            urgent = schedule.isUrgent(contract, today),
                            potentialSalvageEp = salvage.cancellationPayout(contract),
                        )
                    }
                val active = drains.filter { it.contract.isActive }.sortedByDescending { it.monthlyBleed.minor }
                val salvaged =
                    drains
                        .filterNot { it.contract.isActive }
                        .sortedByDescending { it.contract.statusChangedOn }

                DrainsState(
                    active = active,
                    salvaged = salvaged,
                    totalMonthlyBleed = active.map { it.monthlyBleed }.sum(),
                    totalMonthlyBleedEp = salvage.monthlyBleedEp(active.map { it.contract }),
                    monthlySavingsAchieved = salvaged.map { it.contract.monthlySaving }.sum(),
                )
            }
        }
    }
