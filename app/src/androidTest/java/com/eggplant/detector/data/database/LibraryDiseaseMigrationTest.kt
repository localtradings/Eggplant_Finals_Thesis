package com.eggplant.detector.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eggplant.detector.data.database.migration.MIGRATION_7_TO_8
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryDiseaseMigrationTest {
    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EggplantDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrationSevenToEightAddsOnlyLibraryCatalogTables() {
        val databaseName = "migration-7-8-library"
        migrationHelper.createDatabase(databaseName, 7).close()
        migrationHelper.runMigrationsAndValidate(databaseName, 8, true, MIGRATION_7_TO_8).use { migrated ->
            migrated.query(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name IN " +
                    "('library_diseases','library_disease_localizations','library_disease_signs','library_treatments','library_disease_references')",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(5, cursor.getInt(0))
            }
        }
    }
}
