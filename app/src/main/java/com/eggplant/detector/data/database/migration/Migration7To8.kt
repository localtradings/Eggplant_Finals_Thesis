package com.eggplant.detector.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds isolated local tables for administrator-published Library entries. */
val MIGRATION_7_TO_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `library_diseases` (
                `id` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `artworkKey` TEXT NOT NULL,
                `artworkPath` TEXT,
                PRIMARY KEY(`id`))""".trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_diseases_category` ON `library_diseases` (`category`)")
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `library_disease_localizations` (
                `diseaseId` TEXT NOT NULL,
                `languageTag` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `symptomPreview` TEXT NOT NULL,
                `prevention` TEXT NOT NULL,
                `causes` TEXT NOT NULL,
                `guidance` TEXT NOT NULL,
                `whenToAct` TEXT NOT NULL,
                `disclaimer` TEXT NOT NULL,
                PRIMARY KEY(`diseaseId`, `languageTag`),
                FOREIGN KEY(`diseaseId`) REFERENCES `library_diseases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""".trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_disease_localizations_diseaseId` ON `library_disease_localizations` (`diseaseId`)")
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `library_disease_signs` (
                `diseaseId` TEXT NOT NULL,
                `languageTag` TEXT NOT NULL,
                `position` INTEGER NOT NULL,
                `text` TEXT NOT NULL,
                PRIMARY KEY(`diseaseId`, `languageTag`, `position`),
                FOREIGN KEY(`diseaseId`) REFERENCES `library_diseases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""".trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_disease_signs_diseaseId` ON `library_disease_signs` (`diseaseId`)")
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `library_treatments` (
                `diseaseId` TEXT NOT NULL,
                `languageTag` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `treatmentType` TEXT NOT NULL,
                `procedures` TEXT NOT NULL,
                PRIMARY KEY(`diseaseId`, `languageTag`),
                FOREIGN KEY(`diseaseId`) REFERENCES `library_diseases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""".trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_treatments_diseaseId` ON `library_treatments` (`diseaseId`)")
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `library_disease_references` (
                `diseaseId` TEXT NOT NULL,
                `languageTag` TEXT NOT NULL,
                `position` INTEGER NOT NULL,
                `publisher` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `url` TEXT NOT NULL,
                PRIMARY KEY(`diseaseId`, `languageTag`, `position`),
                FOREIGN KEY(`diseaseId`) REFERENCES `library_diseases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""".trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_disease_references_diseaseId` ON `library_disease_references` (`diseaseId`)")
    }
}
