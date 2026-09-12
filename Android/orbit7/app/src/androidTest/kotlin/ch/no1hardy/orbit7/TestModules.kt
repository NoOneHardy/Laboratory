package ch.no1hardy.orbit7

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.room.Room
import androidx.work.WorkManager
import ch.no1hardy.orbit7.core.data.db.Orbit7Database
import ch.no1hardy.orbit7.core.data.db.dao.BudgetDao
import ch.no1hardy.orbit7.core.data.db.dao.CategoryDao
import ch.no1hardy.orbit7.core.data.db.dao.ContractDao
import ch.no1hardy.orbit7.core.data.db.dao.EnergyLedgerDao
import ch.no1hardy.orbit7.core.data.db.dao.ExpenseDao
import ch.no1hardy.orbit7.core.data.db.dao.MissionDao
import ch.no1hardy.orbit7.core.data.db.dao.SettlementDao
import ch.no1hardy.orbit7.core.data.db.dao.StationDao
import ch.no1hardy.orbit7.core.data.db.dao.ZeroSpendDao
import ch.no1hardy.orbit7.core.data.di.ClockModule
import ch.no1hardy.orbit7.core.data.di.DatabaseModule
import ch.no1hardy.orbit7.core.data.settings.SettingsDto
import ch.no1hardy.orbit7.core.data.settings.SettingsSerializer
import ch.no1hardy.orbit7.core.domain.test.FakeClock
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * The end-to-end suite drives time explicitly; nothing in it waits for a real clock.
 *
 * `TimeTravel` is not re-provided here — it has an `@Inject` constructor, so Hilt builds it either
 * way, and providing it twice would be a duplicate binding.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [ClockModule::class])
object TestClockModule {
    @Provides
    @Singleton
    fun provideFakeClock(): FakeClock = FakeClock()

    @Provides
    @Singleton
    fun provideClock(fake: FakeClock): Clock = fake
}

/**
 * An in-memory database, so each journey starts from a clean install.
 *
 * This replaces the whole of [DatabaseModule], so everything that module provides has to be
 * provided again here — the DAOs, the settings store, WorkManager and the IO context.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object TestDatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): Orbit7Database =
        Room
            .inMemoryDatabaseBuilder(context, Orbit7Database::class.java)
            .allowMainThreadQueries()
            .build()

    @Provides fun provideCategoryDao(database: Orbit7Database): CategoryDao = database.categoryDao()

    @Provides fun provideExpenseDao(database: Orbit7Database): ExpenseDao = database.expenseDao()

    @Provides fun provideBudgetDao(database: Orbit7Database): BudgetDao = database.budgetDao()

    @Provides fun provideContractDao(database: Orbit7Database): ContractDao = database.contractDao()

    @Provides fun provideZeroSpendDao(database: Orbit7Database): ZeroSpendDao = database.zeroSpendDao()

    @Provides fun provideLedgerDao(database: Orbit7Database): EnergyLedgerDao = database.energyLedgerDao()

    @Provides fun provideSettlementDao(database: Orbit7Database): SettlementDao = database.settlementDao()

    @Provides fun provideStationDao(database: Orbit7Database): StationDao = database.stationDao()

    @Provides fun provideMissionDao(database: Orbit7Database): MissionDao = database.missionDao()

    /** A per-run file, so one journey's settings never leak into the next. */
    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<SettingsDto> =
        DataStoreFactory.create(
            serializer = SettingsSerializer,
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        ) { context.dataStoreFile("orbit7-settings-test-${java.util.UUID.randomUUID()}.json") }

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context,
    ): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideIoContext(): CoroutineContext = Dispatchers.IO
}
