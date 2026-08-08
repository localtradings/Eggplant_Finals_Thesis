package com.eggplant.detector.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.eggplant.detector.data.database.entity.LibraryDiseaseCatalogBundle
import com.eggplant.detector.data.database.entity.LibraryDiseaseEntity
import com.eggplant.detector.data.database.entity.LibraryDiseaseLocalizationEntity
import com.eggplant.detector.data.database.entity.LibraryDiseaseReferenceEntity
import com.eggplant.detector.data.database.entity.LibraryDiseaseSignEntity
import com.eggplant.detector.data.database.entity.LibraryTreatmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDiseaseCatalogDao {
    @Transaction
    @Query("SELECT * FROM library_diseases ORDER BY category, id")
    fun observeCatalog(): Flow<List<LibraryDiseaseCatalogBundle>>

    @Query("SELECT * FROM library_diseases")
    suspend fun allDiseases(): List<LibraryDiseaseEntity>

    @Upsert
    suspend fun upsertDiseases(rows: List<LibraryDiseaseEntity>)

    @Query("DELETE FROM library_diseases")
    suspend fun clearDiseases()

    @Query("DELETE FROM library_diseases WHERE id NOT IN (:ids)")
    suspend fun deleteDiseasesNotIn(ids: List<String>)

    @Upsert
    suspend fun upsertLocalizations(rows: List<LibraryDiseaseLocalizationEntity>)

    @Upsert
    suspend fun upsertSigns(rows: List<LibraryDiseaseSignEntity>)

    @Upsert
    suspend fun upsertTreatments(rows: List<LibraryTreatmentEntity>)

    @Upsert
    suspend fun upsertReferences(rows: List<LibraryDiseaseReferenceEntity>)

    @Query("DELETE FROM library_disease_localizations WHERE languageTag = :languageTag")
    suspend fun clearLocalizations(languageTag: String)

    @Query("DELETE FROM library_disease_signs WHERE languageTag = :languageTag")
    suspend fun clearSigns(languageTag: String)

    @Query("DELETE FROM library_treatments WHERE languageTag = :languageTag")
    suspend fun clearTreatments(languageTag: String)

    @Query("DELETE FROM library_disease_references WHERE languageTag = :languageTag")
    suspend fun clearReferences(languageTag: String)

    @Transaction
    suspend fun replaceLocalizedContent(
        languageTag: String,
        diseases: List<LibraryDiseaseEntity>,
        localizations: List<LibraryDiseaseLocalizationEntity>,
        signs: List<LibraryDiseaseSignEntity>,
        treatments: List<LibraryTreatmentEntity>,
        references: List<LibraryDiseaseReferenceEntity>,
    ) {
        if (diseases.isEmpty()) clearDiseases()
        else deleteDiseasesNotIn(diseases.map(LibraryDiseaseEntity::id))
        clearLocalizations(languageTag)
        clearSigns(languageTag)
        clearTreatments(languageTag)
        clearReferences(languageTag)
        // The response is the complete library snapshot. Removing only IDs
        // absent from it keeps the other language's cached content available.
        upsertDiseases(diseases)
        upsertLocalizations(localizations)
        upsertSigns(signs)
        upsertTreatments(treatments)
        upsertReferences(references)
    }
}
