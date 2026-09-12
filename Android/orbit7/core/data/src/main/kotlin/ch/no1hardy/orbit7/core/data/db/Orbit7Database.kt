package ch.no1hardy.orbit7.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ch.no1hardy.orbit7.core.data.db.dao.BudgetDao
import ch.no1hardy.orbit7.core.data.db.dao.CategoryDao
import ch.no1hardy.orbit7.core.data.db.dao.ContractDao
import ch.no1hardy.orbit7.core.data.db.dao.EnergyLedgerDao
import ch.no1hardy.orbit7.core.data.db.dao.ExpenseDao
import ch.no1hardy.orbit7.core.data.db.dao.MissionDao
import ch.no1hardy.orbit7.core.data.db.dao.SettlementDao
import ch.no1hardy.orbit7.core.data.db.dao.StationDao
import ch.no1hardy.orbit7.core.data.db.dao.ZeroSpendDao
import ch.no1hardy.orbit7.core.data.db.entity.BudgetEntity
import ch.no1hardy.orbit7.core.data.db.entity.CategoryEntity
import ch.no1hardy.orbit7.core.data.db.entity.ContractEntity
import ch.no1hardy.orbit7.core.data.db.entity.ContractNotificationEntity
import ch.no1hardy.orbit7.core.data.db.entity.EnergyLedgerEntity
import ch.no1hardy.orbit7.core.data.db.entity.ExpenseEntity
import ch.no1hardy.orbit7.core.data.db.entity.MissionEntity
import ch.no1hardy.orbit7.core.data.db.entity.SettlementCategoryEntity
import ch.no1hardy.orbit7.core.data.db.entity.StationModuleEntity
import ch.no1hardy.orbit7.core.data.db.entity.WeeklySettlementEntity
import ch.no1hardy.orbit7.core.data.db.entity.ZeroSpendMarkEntity

/**
 * The single local database. There is no other copy of this data anywhere
 * (`docs/01-concept.md`, privacy stance), which is why hard rule 4 exists: every schema change
 * ships a migration and a migration test, and a destructive migration is never acceptable.
 */
@Database(
    entities = [
        CategoryEntity::class,
        ExpenseEntity::class,
        BudgetEntity::class,
        ContractEntity::class,
        ContractNotificationEntity::class,
        ZeroSpendMarkEntity::class,
        EnergyLedgerEntity::class,
        WeeklySettlementEntity::class,
        SettlementCategoryEntity::class,
        StationModuleEntity::class,
        MissionEntity::class,
    ],
    version = Orbit7Database.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class Orbit7Database : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao

    abstract fun expenseDao(): ExpenseDao

    abstract fun budgetDao(): BudgetDao

    abstract fun contractDao(): ContractDao

    abstract fun zeroSpendDao(): ZeroSpendDao

    abstract fun energyLedgerDao(): EnergyLedgerDao

    abstract fun settlementDao(): SettlementDao

    abstract fun stationDao(): StationDao

    abstract fun missionDao(): MissionDao

    companion object {
        const val VERSION = 1
        const val NAME = "orbit7.db"
    }
}
