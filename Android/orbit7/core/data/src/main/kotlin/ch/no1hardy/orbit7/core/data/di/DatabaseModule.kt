package ch.no1hardy.orbit7.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.room.Room
import androidx.work.WorkManager
import ch.no1hardy.orbit7.core.data.db.Migrations
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
import ch.no1hardy.orbit7.core.data.settings.SettingsDto
import ch.no1hardy.orbit7.core.data.settings.SettingsSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): Orbit7Database =
        Room
            .databaseBuilder(context, Orbit7Database::class.java, Orbit7Database.NAME)
            .addMigrations(*Migrations.ALL)
            // Deliberately no fallbackToDestructiveMigration: this database is the only copy of the
            // user's data (docs/05-architecture.md §8).
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

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<SettingsDto> =
        DataStoreFactory.create(
            serializer = SettingsSerializer,
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        ) { context.dataStoreFile("orbit7-settings.json") }

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context,
    ): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideIoContext(): CoroutineContext = Dispatchers.IO
}
