package ch.no1hardy.orbit7.core.data.di

import ch.no1hardy.orbit7.core.data.repository.RoomBudgetRepository
import ch.no1hardy.orbit7.core.data.repository.RoomCategoryRepository
import ch.no1hardy.orbit7.core.data.repository.RoomContractRepository
import ch.no1hardy.orbit7.core.data.repository.RoomEnergyRepository
import ch.no1hardy.orbit7.core.data.repository.RoomExpenseRepository
import ch.no1hardy.orbit7.core.data.repository.RoomMissionRepository
import ch.no1hardy.orbit7.core.data.repository.RoomSettlementRepository
import ch.no1hardy.orbit7.core.data.repository.RoomStationRepository
import ch.no1hardy.orbit7.core.data.repository.RoomZeroSpendRepository
import ch.no1hardy.orbit7.core.data.settings.DataStoreSettingsRepository
import ch.no1hardy.orbit7.core.domain.repository.BudgetRepository
import ch.no1hardy.orbit7.core.domain.repository.CategoryRepository
import ch.no1hardy.orbit7.core.domain.repository.ContractRepository
import ch.no1hardy.orbit7.core.domain.repository.EnergyRepository
import ch.no1hardy.orbit7.core.domain.repository.ExpenseRepository
import ch.no1hardy.orbit7.core.domain.repository.MissionRepository
import ch.no1hardy.orbit7.core.domain.repository.SettingsRepository
import ch.no1hardy.orbit7.core.domain.repository.SettlementRepository
import ch.no1hardy.orbit7.core.domain.repository.StationRepository
import ch.no1hardy.orbit7.core.domain.repository.ZeroSpendRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the domain's ports to their Room and DataStore implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindCategoryRepository(implementation: RoomCategoryRepository): CategoryRepository

    @Binds @Singleton
    abstract fun bindExpenseRepository(implementation: RoomExpenseRepository): ExpenseRepository

    @Binds @Singleton
    abstract fun bindBudgetRepository(implementation: RoomBudgetRepository): BudgetRepository

    @Binds @Singleton
    abstract fun bindContractRepository(implementation: RoomContractRepository): ContractRepository

    @Binds @Singleton
    abstract fun bindZeroSpendRepository(implementation: RoomZeroSpendRepository): ZeroSpendRepository

    @Binds @Singleton
    abstract fun bindEnergyRepository(implementation: RoomEnergyRepository): EnergyRepository

    @Binds @Singleton
    abstract fun bindSettlementRepository(implementation: RoomSettlementRepository): SettlementRepository

    @Binds @Singleton
    abstract fun bindStationRepository(implementation: RoomStationRepository): StationRepository

    @Binds @Singleton
    abstract fun bindMissionRepository(implementation: RoomMissionRepository): MissionRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(implementation: DataStoreSettingsRepository): SettingsRepository
}
