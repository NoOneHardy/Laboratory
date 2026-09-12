package ch.no1hardy.orbit7.core.data.db

import androidx.room.migration.Migration

/**
 * Every migration ORBIT-7 has ever shipped.
 *
 * The list is empty at version 1 and the exported schema JSON is committed anyway — that is the
 * point of hard rule 4. When version 2 arrives it is added here together with a migration test that
 * opens the v1 schema, writes representative rows, migrates and asserts the data survived. There is
 * no cloud backup to fall back on, so `fallbackToDestructiveMigration` is never an option.
 */
object Migrations {
    val ALL: Array<Migration> = emptyArray()
}
