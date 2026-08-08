package com.eggplant.detector.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds only local scan metadata needed for favorites and complete result restoration. */
val MIGRATION_6_TO_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `scan_sessions` ADD COLUMN `isFavorite` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `scan_sessions` ADD COLUMN `resultName` TEXT")
        db.execSQL("ALTER TABLE `scan_sessions` ADD COLUMN `resultCategory` TEXT")
        db.execSQL("ALTER TABLE `scan_sessions` ADD COLUMN `resultOutcome` TEXT")
        db.execSQL("ALTER TABLE `scan_sessions` ADD COLUMN `resultConfidence` INTEGER")
        db.execSQL("ALTER TABLE `scan_sessions` ADD COLUMN `resultDiseaseId` TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_scan_sessions_savedAt_isFavorite` ON `scan_sessions` (`savedAt`, `isFavorite`)")
    }
}
