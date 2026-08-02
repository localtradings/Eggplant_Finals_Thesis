package com.eggplant.detector.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eggplant.detector.data.cloud.SharePhotoRevalidator
import com.eggplant.detector.data.database.EggplantDatabase
import com.eggplant.detector.data.files.ScanSnapshotStore
import com.eggplant.detector.detection.api.InputSource
import com.eggplant.detector.detection.api.RgbFrame
import com.eggplant.detector.domain.model.ScanCategory
import com.eggplant.detector.domain.model.ScanOutcome
import com.eggplant.detector.domain.model.ScanResult
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EggplantRepositoryCloudTest {
    @Test
    fun galleryShareQueuesConsentAndShareTogether() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.inMemoryDatabaseBuilder(context, EggplantDatabase::class.java).build()
        val snapshotStore = ScanSnapshotStore(context)
        val repository = EggplantRepository(
            database = database,
            snapshotStore = snapshotStore,
            cloudConfigured = { true },
            sharePhotoRevalidator = SharePhotoRevalidator { _, _ -> 0.87f },
        )
        val stagedPath = snapshotStore.stage(
            RgbFrame(
                width = 2,
                height = 2,
                rgbBytes = ByteArray(12) { 0x55 },
                timestampMillis = 1L,
                source = InputSource.GALLERY,
                sceneToken = 1L,
            ),
        )
        val result = ScanResult(
            id = "01890f3d-00d8-7b65-9a77-a79bfe3f8483",
            name = "Leaf Spot",
            category = ScanCategory.LEAF_DISEASE,
            outcome = ScanOutcome.DISEASE,
            confidence = 87,
            scannedAt = LocalDateTime.of(2026, 8, 2, 15, 0),
            signs = emptyList(),
            treatment = "Remove affected leaves.",
            diseaseId = "leaf-spot",
            source = "gallery",
            imagePath = stagedPath,
        )
        var outboxPhoto: String? = null
        try {
            repository.ensureCatalog()
            assertEquals(
                com.eggplant.detector.domain.model.ShareEligibility.Eligible,
                repository.enqueueGlobalShare(result, sharingEnabled = true),
            )

            val consent = database.cloudDao().outboxByIdempotencyKey("sharing-consent")
            val share = database.cloudDao().outboxByIdempotencyKey("global:${result.id}")
            assertEquals("SHARING_CONSENT", consent?.eventType)
            assertEquals("GLOBAL_SHARE", share?.eventType)
            val payload = Json.parseToJsonElement(requireNotNull(share).payloadJson).jsonObject
            assertEquals("gallery", payload.getValue("source").jsonPrimitive.content)
            outboxPhoto = payload.getValue("photoPath").jsonPrimitive.content
            assertTrue(java.io.File(requireNotNull(outboxPhoto)).isFile)
        } finally {
            snapshotStore.discard(stagedPath)
            snapshotStore.removeOutboxPhoto(outboxPhoto)
            database.close()
        }
    }
}
