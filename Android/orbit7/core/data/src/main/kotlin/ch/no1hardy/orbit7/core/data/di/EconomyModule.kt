package ch.no1hardy.orbit7.core.data.di

import ch.no1hardy.orbit7.core.data.backup.JsonBackupRepository
import ch.no1hardy.orbit7.core.data.db.Orbit7Database
import ch.no1hardy.orbit7.core.domain.GameBalance
import ch.no1hardy.orbit7.core.domain.economy.BaselineCalculator
import ch.no1hardy.orbit7.core.domain.economy.ContractSchedule
import ch.no1hardy.orbit7.core.domain.economy.MissionEvaluator
import ch.no1hardy.orbit7.core.domain.economy.MissionGenerator
import ch.no1hardy.orbit7.core.domain.economy.SalvageCalculator
import ch.no1hardy.orbit7.core.domain.economy.SettlementCalculator
import ch.no1hardy.orbit7.core.domain.economy.StationCatalog
import ch.no1hardy.orbit7.core.domain.repository.BackupRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * The economy's single set of constants and the stateless calculators built on them.
 *
 * One [GameBalance] instance for the whole app means a rebalancing is one edit in one file.
 */
@Module
@InstallIn(SingletonComponent::class)
object EconomyModule {
    @Provides @Singleton
    fun provideGameBalance(): GameBalance = GameBalance.DEFAULT

    @Provides @Singleton
    fun provideSettlementCalculator(balance: GameBalance) = SettlementCalculator(balance)

    @Provides @Singleton
    fun provideBaselineCalculator(balance: GameBalance) = BaselineCalculator(balance)

    @Provides @Singleton
    fun provideSalvageCalculator(balance: GameBalance) = SalvageCalculator(balance)

    @Provides @Singleton
    fun provideContractSchedule(balance: GameBalance) = ContractSchedule(balance)

    @Provides @Singleton
    fun provideStationCatalog(balance: GameBalance) = StationCatalog(balance)

    @Provides @Singleton
    fun provideMissionGenerator(balance: GameBalance) = MissionGenerator(balance)

    @Provides @Singleton
    fun provideMissionEvaluator() = MissionEvaluator()

    @Provides @Singleton
    fun provideBackupRepository(
        database: Orbit7Database,
        clock: Clock,
        ioContext: CoroutineContext,
        @Named("appVersion") appVersion: String,
    ): BackupRepository = JsonBackupRepository(database, clock, ioContext, appVersion)
}
