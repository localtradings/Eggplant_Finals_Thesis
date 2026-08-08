package com.eggplant.detector.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eggplant.detector.data.cloud.SharePhotoRevalidator
import com.eggplant.detector.data.database.EggplantDatabase
import com.eggplant.detector.data.database.entity.GlobalScanCacheEntity
import com.eggplant.detector.data.files.ScanSnapshotStore
import com.eggplant.detector.detection.api.InputSource
import com.eggplant.detector.detection.api.RgbFrame
import com.eggplant.detector.domain.model.ScanCategory
import com.eggplant.detector.domain.model.ScanDetectionResult
import com.eggplant.detector.domain.model.ScanOutcome
import com.eggplant.detector.domain.model.ScanResult
import com.eggplant.detector.detection.api.NormalizedBox
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EggplantRepositoryCloudTest {
    @Test
    fun contentReportQueuesOnceForTheSelectedScan() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.inMemoryDatabaseBuilder(context, EggplantDatabase::class.java).build()
        val repository = EggplantRepository(database = database, cloudConfigured = { true })
        val scanId = UUID.fromString("01890f3d-00d8-7b65-9a77-a79bfe3f8483").toString()
        try {
            repository.enqueueContentReport(scanId, "incorrect_result")
            repository.enqueueContentReport(scanId, "incorrect_result")

            val event = database.cloudDao().outboxByIdempotencyKey("report:$scanId")
            assertEquals("CONTENT_REPORT", event?.eventType)
            assertTrue(event?.payloadJson?.contains("/api/mobile/v1/global-scans/$scanId/reports") == true)
        } finally {
            database.close()
        }
    }

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
            detections = listOf(
                ScanDetectionResult(
                    id = "detection-1",
                    diseaseId = "leaf-spot",
                    name = "Leaf Spot",
                    modelClassIndex = 5,
                    modelLabel = "Leaf-Spot",
                    confidence = 87,
                    bounds = NormalizedBox(.1f, .1f, .9f, .9f),
                ),
            ),
        )
        var outboxPhoto: String? = null
        var annotatedOutboxPhoto: String? = null
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
            annotatedOutboxPhoto = payload.getValue("annotatedPhotoPath").jsonPrimitive.content
            assertTrue(java.io.File(requireNotNull(outboxPhoto)).isFile)
            assertTrue(java.io.File(requireNotNull(annotatedOutboxPhoto)).isFile)

            repository.enqueueGlobalShare(result, sharingEnabled = true)
            assertEquals(share.id, database.cloudDao().outboxByIdempotencyKey("global:${result.id}")?.id)
        } finally {
            snapshotStore.discard(stagedPath)
            snapshotStore.removeOutboxPhoto(outboxPhoto)
            snapshotStore.removeOutboxPhoto(annotatedOutboxPhoto)
            database.close()
        }
    }

    @Test
    fun galleryDiseaseRequestQueuesThePhotoAndSource() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.inMemoryDatabaseBuilder(context, EggplantDatabase::class.java).build()
        val snapshotStore = ScanSnapshotStore(context)
        val repository = EggplantRepository(
            database = database,
            snapshotStore = snapshotStore,
            cloudConfigured = { true },
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
        var outboxPhoto: String? = null
        try {
            val clientRequestId = repository.enqueueDiseaseRequest(
                requestedName = "Unknown disease",
                notes = "Please review this gallery photo.",
                photoPaths = listOf(stagedPath),
                photoSources = listOf("gallery"),
                rightsConsent = true,
                clientRequestId = "01890f3d-00d8-7b65-9a77-a79bfe3f8483",
            )

            val request = requireNotNull(database.cloudDao().diseaseRequestByClientId(clientRequestId))
            assertEquals("QUEUED", request.state)
            val photos = database.cloudDao().requestPhotos(request.id)
            assertEquals(listOf("gallery"), photos.map { it.captureSource })
            outboxPhoto = photos.single().localPhotoPath

            val event = database.cloudDao().outboxByIdempotencyKey("request:$clientRequestId")
            assertEquals("DISEASE_REQUEST", event?.eventType)
            val payload = Json.parseToJsonElement(requireNotNull(event).payloadJson).jsonObject
            assertEquals(
                listOf("gallery"),
                payload.getValue("photoSources").jsonArray.map { it.jsonPrimitive.content },
            )

            assertEquals(
                clientRequestId,
                repository.enqueueDiseaseRequest(
                    requestedName = "Unknown disease",
                    notes = "Please review this gallery photo.",
                    photoPaths = listOf(stagedPath),
                    photoSources = listOf("gallery"),
                    rightsConsent = true,
                    clientRequestId = clientRequestId,
                ),
            )
            assertEquals(event.id, database.cloudDao().outboxByIdempotencyKey("request:$clientRequestId")?.id)
        } finally {
            snapshotStore.discard(stagedPath)
            snapshotStore.removeOutboxPhoto(outboxPhoto)
            database.close()
        }
    }

    @Test
    fun globalRefreshRequestsCloudWorkAndPublishesNewCacheRows() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.inMemoryDatabaseBuilder(context, EggplantDatabase::class.java).build()
        val refreshRequested = AtomicBoolean(false)
        val repository = EggplantRepository(
            database = database,
            cloudConfigured = { true },
            cloudSyncGlobalScans = { refreshRequested.set(true) },
        )
        try {
            repository.ensureCatalog()
            repository.refreshGlobalScans()
            assertTrue(refreshRequested.get())

            database.cloudDao().upsertGlobalScans(
                listOf(
                    GlobalScanCacheEntity(
                        id = UUID.fromString("01890f3d-00d8-7b65-9a77-a79bfe3f8483").toString(),
                        diseaseId = "leaf-spot",
                        confidence = .87f,
                        source = "gallery",
                        modelVersion = "eggplant-yolo26m-v3-clean-768-20260707",
                        cachedPhotoPath = null,
                        annotatedCachedPhotoPath = null,
                        publishedAt = "2026-08-06T00:00:00Z",
                        expiresAt = "2027-08-06T00:00:00Z",
                        contentJson = "{\"signs\":[],\"content\":{\"name\":\"Leaf Spot\"}}",
                    ),
                ),
            )

            val visible = repository.globalScans.first { scans -> scans.any { it.diseaseId == "leaf-spot" } }
            val scan = visible.single { it.diseaseId == "leaf-spot" }
            assertEquals("Leaf Spot", scan.diseaseName)
            assertEquals(87, scan.confidence)
        } finally {
            database.close()
        }
    }
}
