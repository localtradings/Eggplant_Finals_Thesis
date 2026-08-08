package com.eggplant.detector.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eggplant.detector.data.database.entity.LibraryDiseaseEntity
import com.eggplant.detector.data.database.entity.LibraryDiseaseLocalizationEntity
import com.eggplant.detector.data.database.entity.LibraryDiseaseReferenceEntity
import com.eggplant.detector.data.database.entity.LibraryDiseaseSignEntity
import com.eggplant.detector.data.database.entity.LibraryTreatmentEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryDiseaseCatalogTest {
    @Test
    fun libraryOnlyEntryPersistsBilingualContentAndArtworkPath() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, EggplantDatabase::class.java).build()
        try {
            database.libraryDiseaseCatalogDao().replaceLocalizedContent(
                languageTag = "en",
                diseases = listOf(LibraryDiseaseEntity("purple-leaf-blight", "LEAF_DISEASE", "purple-leaf-blight", "/files/catalog-artwork/purple-leaf-blight.jpg")),
                localizations = listOf(
                    LibraryDiseaseLocalizationEntity("purple-leaf-blight", "en", "Purple Leaf Blight", "Description", "Spots", "Prevention", "Cause", "Guidance", "When to act", "Screening aid"),
                ),
                signs = listOf(LibraryDiseaseSignEntity("purple-leaf-blight", "en", 0, "Dark leaf lesions")),
                treatments = listOf(LibraryTreatmentEntity("purple-leaf-blight", "en", "Recommended action", "RECOMMENDED_ACTION", "Remove affected leaves")),
                references = listOf(LibraryDiseaseReferenceEntity("purple-leaf-blight", "en", 0, "Publisher", "Reference", "https://example.org/reference")),
            )

            val row = database.libraryDiseaseCatalogDao().observeCatalog().first().single()
            assertEquals("Purple Leaf Blight", row.localizations.single().name)
            assertEquals("/files/catalog-artwork/purple-leaf-blight.jpg", row.disease.artworkPath)
            assertFalse(database.catalogDao().diseaseIds(listOf("purple-leaf-blight")).contains("purple-leaf-blight"))
        } finally {
            database.close()
        }
    }

    @Test
    fun replacingTheLibrarySnapshotRemovesDeletedDiseaseAndPreservesOtherLanguage() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, EggplantDatabase::class.java).build()
        try {
            val dao = database.libraryDiseaseCatalogDao()
            dao.replaceLocalizedContent(
                languageTag = "en",
                diseases = listOf(
                    LibraryDiseaseEntity("first-disease", "LEAF_DISEASE", "first-disease", null),
                    LibraryDiseaseEntity("deleted-disease", "FRUIT_DISEASE", "deleted-disease", null),
                ),
                localizations = listOf(
                    LibraryDiseaseLocalizationEntity("first-disease", "en", "First disease", "Description", "Symptoms", "Prevention", "Causes", "Guidance", "When to act", "Disclaimer"),
                ),
                signs = emptyList(),
                treatments = emptyList(),
                references = emptyList(),
            )
            dao.replaceLocalizedContent(
                languageTag = "fil",
                diseases = listOf(LibraryDiseaseEntity("first-disease", "LEAF_DISEASE", "first-disease", null)),
                localizations = listOf(
                    LibraryDiseaseLocalizationEntity("first-disease", "fil", "Unang sakit", "Paglalarawan", "Sintomas", "Pag-iwas", "Sanhi", "Gabay", "Kailan kikilos", "Paalala"),
                ),
                signs = emptyList(),
                treatments = emptyList(),
                references = emptyList(),
            )

            assertEquals(listOf("first-disease"), dao.allDiseases().map(LibraryDiseaseEntity::id))
            assertEquals(
                listOf("en", "fil"),
                dao.observeCatalog().first().single().localizations.map(LibraryDiseaseLocalizationEntity::languageTag).sorted(),
            )
        } finally {
            database.close()
        }
    }
}
