package com.eggplant.detector.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds only the local cache for administrator-published notifications. */
val MIGRATION_8_TO_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `remote_notifications` (
                `id` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `titleEn` TEXT NOT NULL,
                `bodyEn` TEXT NOT NULL,
                `titleFil` TEXT NOT NULL,
                `bodyFil` TEXT NOT NULL,
                `publishedAt` TEXT NOT NULL,
                `expiresAt` TEXT,
                PRIMARY KEY(`id`))""".trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_remote_notifications_publishedAt` ON `remote_notifications` (`publishedAt`)")
    }
}
