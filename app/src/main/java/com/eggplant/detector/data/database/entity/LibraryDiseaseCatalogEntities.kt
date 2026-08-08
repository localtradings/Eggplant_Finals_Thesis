package com.eggplant.detector.data.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Relation

/**
 * Library-only catalog rows published by an administrator.
 *
 * These tables deliberately do not share the detector model mapping tables;
 * adding educational content must never create a class that the NCNN model
 * can claim to detect.
 */
@Entity(
    tableName = "library_diseases",
    primaryKeys = ["id"],
    indices = [Index("category")],
)
data class LibraryDiseaseEntity(
    val id: String,
    val category: String,
    val artworkKey: String,
    val artworkPath: String?,
)

@Entity(
    tableName = "library_disease_localizations",
    primaryKeys = ["diseaseId", "languageTag"],
    foreignKeys = [
        ForeignKey(
            entity = LibraryDiseaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["diseaseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("diseaseId")],
)
data class LibraryDiseaseLocalizationEntity(
    val diseaseId: String,
    val languageTag: String,
    val name: String,
    val description: String,
    val symptomPreview: String,
    val prevention: String,
    val causes: String,
    val guidance: String,
    val whenToAct: String,
    val disclaimer: String,
)

@Entity(
    tableName = "library_disease_signs",
    primaryKeys = ["diseaseId", "languageTag", "position"],
    foreignKeys = [
        ForeignKey(
            entity = LibraryDiseaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["diseaseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("diseaseId")],
)
data class LibraryDiseaseSignEntity(
    val diseaseId: String,
    val languageTag: String,
    val position: Int,
    val text: String,
)

@Entity(
    tableName = "library_treatments",
    primaryKeys = ["diseaseId", "languageTag"],
    foreignKeys = [
        ForeignKey(
            entity = LibraryDiseaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["diseaseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("diseaseId")],
)
data class LibraryTreatmentEntity(
    val diseaseId: String,
    val languageTag: String,
    val title: String,
    val treatmentType: String,
    val procedures: String,
)

@Entity(
    tableName = "library_disease_references",
    primaryKeys = ["diseaseId", "languageTag", "position"],
    foreignKeys = [
        ForeignKey(
            entity = LibraryDiseaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["diseaseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("diseaseId")],
)
data class LibraryDiseaseReferenceEntity(
    val diseaseId: String,
    val languageTag: String,
    val position: Int,
    val publisher: String,
    val title: String,
    val url: String,
)

data class LibraryDiseaseCatalogBundle(
    @Embedded val disease: LibraryDiseaseEntity,
    @Relation(parentColumn = "id", entityColumn = "diseaseId")
    val localizations: List<LibraryDiseaseLocalizationEntity>,
    @Relation(parentColumn = "id", entityColumn = "diseaseId")
    val signs: List<LibraryDiseaseSignEntity>,
    @Relation(parentColumn = "id", entityColumn = "diseaseId")
    val treatments: List<LibraryTreatmentEntity>,
    @Relation(parentColumn = "id", entityColumn = "diseaseId")
    val references: List<LibraryDiseaseReferenceEntity>,
)
