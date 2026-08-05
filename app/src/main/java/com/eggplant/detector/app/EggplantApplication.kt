package com.eggplant.detector.app

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.eggplant.detector.data.repository.EggplantRepository
import com.eggplant.detector.data.files.ScanSnapshotStore
import com.eggplant.detector.data.database.EggplantDatabase
import com.eggplant.detector.data.database.migration.MIGRATION_1_TO_2
import com.eggplant.detector.data.database.migration.MIGRATION_2_TO_3
import com.eggplant.detector.data.database.migration.MIGRATION_3_TO_4
import com.eggplant.detector.data.database.migration.MIGRATION_4_TO_5
import com.eggplant.detector.data.database.migration.MIGRATION_5_TO_6
import com.eggplant.detector.detection.ncnn.NcnnDetectionEngine
import com.eggplant.detector.data.cloud.CloudApiClient
import com.eggplant.detector.data.cloud.CloudSyncScheduler
import com.eggplant.detector.data.cloud.NcnnSharePhotoRevalidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EggplantApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _startupReady = MutableStateFlow(false)
    val startupReady: StateFlow<Boolean> = _startupReady.asStateFlow()
    val detectionEngine: NcnnDetectionEngine by lazy { NcnnDetectionEngine(applicationContext) }
    val cloudApiClient: CloudApiClient by lazy { CloudApiClient(applicationContext) }

    val database: EggplantDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            EggplantDatabase::class.java,
            "eggplant_detector.db",
        ).addMigrations(MIGRATION_1_TO_2, MIGRATION_2_TO_3, MIGRATION_3_TO_4, MIGRATION_4_TO_5, MIGRATION_5_TO_6).build()
    }

    val repository: EggplantRepository by lazy {
        EggplantRepository(
            database = database,
            snapshotStore = ScanSnapshotStore(applicationContext),
            cloudSync = { CloudSyncScheduler.refresh(this) },
            cloudSyncLoadMore = { CloudSyncScheduler.loadMoreGlobalScans(this) },
            cloudSyncGlobalScans = { CloudSyncScheduler.refreshGlobalScans(this) },
            cloudConfigured = { cloudApiClient.isConfigured },
            cloudConfiguredState = cloudApiClient.configured,
            sharePhotoRevalidator = NcnnSharePhotoRevalidator(detectionEngine),
        )
    }

    override fun onCreate() {
        super.onCreate()
        CloudSyncScheduler.schedule(this)
        applicationScope.launch {
            runCatching {
                // These are the same local reads the first screen already
                // depends on. Waiting for them removes the blank startup
                // frame without delaying the app with a timer.
                repository.ensureCatalog()
                repository.settings.first()
                repository.history.first()
            }.onFailure { error ->
                // The bundled catalog/default UI can still open if a local
                // startup read fails; never leave the user on a loader.
                Log.w("EggplantStartup", "Local startup preparation failed", error)
            }
            _startupReady.value = true
        }
        applicationScope.launch {
            if (cloudApiClient.bootstrapConfiguration()) {
                CloudSyncScheduler.refresh(this@EggplantApplication)
            }
        }
    }
}
