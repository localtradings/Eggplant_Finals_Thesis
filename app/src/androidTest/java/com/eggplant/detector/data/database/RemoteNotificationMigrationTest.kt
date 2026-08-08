package com.eggplant.detector.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eggplant.detector.data.database.migration.MIGRATION_8_TO_9
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteNotificationMigrationTest {
    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EggplantDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrationEightToNineAddsOnlyRemoteNotificationCache() {
        val databaseName = "migration-8-9-notifications"
        migrationHelper.createDatabase(databaseName, 8).close()
        migrationHelper.runMigrationsAndValidate(databaseName, 9, true, MIGRATION_8_TO_9).use { migrated ->
            migrated.query(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'remote_notifications'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            migrated.query("PRAGMA table_info(remote_notifications)").use { cursor ->
                assertEquals(8, cursor.count)
            }
        }
    }
}
