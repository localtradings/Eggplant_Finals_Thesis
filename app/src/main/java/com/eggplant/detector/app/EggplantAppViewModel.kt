package com.eggplant.detector.app

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.eggplant.detector.feature.camera.CameraScene
import com.eggplant.detector.data.catalog.DiseaseCatalog
import com.eggplant.detector.data.catalog.DiseaseContentResolver
import com.eggplant.detector.data.repository.EggplantRepository
import com.eggplant.detector.data.database.entity.AppSettingsEntity
import com.eggplant.detector.detection.api.DetectionBox
import com.eggplant.detector.detection.ncnn.ModelMetadata
import com.eggplant.detector.domain.model.DiseaseType
import com.eggplant.detector.domain.model.ScanCategory
import com.eggplant.detector.domain.model.ScanDetectionResult
import com.eggplant.detector.domain.model.ScanOutcome
import com.eggplant.detector.domain.model.ScanResult
import com.eggplant.detector.domain.model.GlobalScan
import com.eggplant.detector.domain.model.GlobalRanking
import com.eggplant.detector.domain.model.DiseaseRequest
import com.eggplant.detector.domain.model.MotionPreference
import com.eggplant.detector.domain.model.ShareEligibility
import com.eggplant.detector.domain.model.CloudDeletionState
import com.eggplant.detector.domain.model.GlobalFeedState
import com.eggplant.detector.domain.model.RemoteNotification
import com.eggplant.detector.domain.model.SyncOutboxEvent
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

enum class ThemePreference(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System default"),
}

enum class LanguagePreference(val languageTag: String, val displayName: String) {
    ENGLISH("en", "English"),
    FILIPINO("fil", "Filipino (Tagalog)"),
}

enum class SaveState {
    IDLE,
    SAVING,
    SAVED,
    ALREADY_SAVED,
    FAILED,
}

enum class ResultWarning {
    SNAPSHOT_UNAVAILABLE,
}

enum class SnapshotState { PREPARING, READY, UNAVAILABLE }

sealed interface CloudActionState {
    data object Idle : CloudActionState
    data object Working : CloudActionState
    data class Queued(val message: String) : CloudActionState
    data class Error(val message: String) : CloudActionState
}

data class ReportActionState(
    val scanId: String,
    val state: CloudActionState,
)

internal fun shareActionState(eligibility: ShareEligibility): CloudActionState = when (eligibility) {
    // The result screen renders the outbox event itself so it can transition
    // from queued -> uploading -> published/failed. A second queued action
    // banner would remain stale after the worker completed.
    ShareEligibility.Eligible -> CloudActionState.Idle
    is ShareEligibility.Ineligible -> CloudActionState.Error(
        eligibility.reason.name.lowercase().replace('_', ' '),
    )
}

data class DiseaseRequestDraftState(
    val photoPaths: List<String> = emptyList(),
    val photoSources: List<String> = emptyList(),
    val error: String? = null,
)

class EggplantAppViewModel(
    initialHistory: List<ScanResult> = emptyList(),
    private val nowProvider: () -> LocalDateTime = { LocalDateTime.now() },
    private val repository: EggplantRepository? = null,
    scanSaver: (suspend (ScanResult) -> ScanResult)? = null,
    private val snapshotStager: (suspend (com.eggplant.detector.detection.api.RgbFrame) -> String?)? = null,
) : ViewModel() {
    /**
     * Cloud configuration is a runtime capability, not user data. Expose it
     * so the UI can react when the public configuration is bootstrapped after
     * the APK starts instead of silently ignoring cloud interactions.
     */
    val cloudConfigured: Boolean
        get() = _cloudConfigured.value

    private val _cloudConfigured = MutableStateFlow(repository?.isCloudConfigured == true)
    val cloudConfiguredState: StateFlow<Boolean> = _cloudConfigured.asStateFlow()

    private val scanSaver = scanSaver ?: repository?.let { localRepository ->
        suspend { result: ScanResult -> localRepository.saveScan(result) }
    }
    private val _history = MutableStateFlow(initialHistory)
    val history: StateFlow<List<ScanResult>> = _history.asStateFlow()

    private val _catalog = MutableStateFlow(DiseaseCatalog.diseases)
    val catalog: StateFlow<List<com.eggplant.detector.domain.model.Disease>> = _catalog.asStateFlow()

    private val _currentResult = MutableStateFlow<ScanResult?>(null)
    val currentResult: StateFlow<ScanResult?> = _currentResult.asStateFlow()

    private val _lastScan = MutableStateFlow(initialHistory.firstOrNull())
    val lastScan: StateFlow<ScanResult?> = _lastScan.asStateFlow()

    private val _themePreference = MutableStateFlow(ThemePreference.SYSTEM)
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    private var legacyUnitSystem = "SYSTEM"

    private val _languagePreference = MutableStateFlow(
        if (java.util.Locale.getDefault().language in setOf("fil", "tl")) LanguagePreference.FILIPINO
        else LanguagePreference.ENGLISH,
    )
    val languagePreference: StateFlow<LanguagePreference> = _languagePreference.asStateFlow()

    private val _detectHealthyLeafEnabled = MutableStateFlow(false)
    val detectHealthyLeafEnabled: StateFlow<Boolean> = _detectHealthyLeafEnabled.asStateFlow()

    private val _detectHealthyPlantEnabled = MutableStateFlow(false)
    val detectHealthyPlantEnabled: StateFlow<Boolean> = _detectHealthyPlantEnabled.asStateFlow()

    private val _readNotificationKeys = MutableStateFlow<Set<String>>(emptySet())
    val readNotificationKeys: StateFlow<Set<String>> = _readNotificationKeys.asStateFlow()

    private val _remoteNotifications = MutableStateFlow<List<RemoteNotification>>(emptyList())
    val remoteNotifications: StateFlow<List<RemoteNotification>> = _remoteNotifications.asStateFlow()

    private val _saveState = MutableStateFlow(SaveState.IDLE)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _resultWarning = MutableStateFlow<ResultWarning?>(null)
    val resultWarning: StateFlow<ResultWarning?> = _resultWarning.asStateFlow()

    private val _snapshotState = MutableStateFlow(SnapshotState.READY)
    val snapshotState: StateFlow<SnapshotState> = _snapshotState.asStateFlow()

    private val _globalScans = MutableStateFlow<List<GlobalScan>>(emptyList())
    val globalScans: StateFlow<List<GlobalScan>> = _globalScans.asStateFlow()

    private val _globalRankings = MutableStateFlow<List<GlobalRanking>>(emptyList())
    val globalRankings: StateFlow<List<GlobalRanking>> = _globalRankings.asStateFlow()

    private val _globalFeedState = MutableStateFlow(GlobalFeedState())
    val globalFeedState: StateFlow<GlobalFeedState> = _globalFeedState.asStateFlow()

    private val _diseaseRequests = MutableStateFlow<List<DiseaseRequest>>(emptyList())
    val diseaseRequests: StateFlow<List<DiseaseRequest>> = _diseaseRequests.asStateFlow()

    private val _globalSharingEnabled = MutableStateFlow(false)
    val globalSharingEnabled: StateFlow<Boolean> = _globalSharingEnabled.asStateFlow()

    private val _motionPreference = MutableStateFlow(MotionPreference.SYSTEM)
    val motionPreference: StateFlow<MotionPreference> = _motionPreference.asStateFlow()

    private val _cloudActionState = MutableStateFlow<CloudActionState>(CloudActionState.Idle)
    val cloudActionState: StateFlow<CloudActionState> = _cloudActionState.asStateFlow()

    private val _reportActionState = MutableStateFlow<ReportActionState?>(null)
    val reportActionState: StateFlow<ReportActionState?> = _reportActionState.asStateFlow()

    private val _cloudDeletionState = MutableStateFlow<CloudDeletionState>(CloudDeletionState.Idle)
    val cloudDeletionState: StateFlow<CloudDeletionState> = _cloudDeletionState.asStateFlow()

    private val _syncOutboxEvents = MutableStateFlow<List<SyncOutboxEvent>>(emptyList())
    val syncOutboxEvents: StateFlow<List<SyncOutboxEvent>> = _syncOutboxEvents.asStateFlow()

    private val _diseaseRequestDraft = MutableStateFlow(DiseaseRequestDraftState())
    val diseaseRequestDraft: StateFlow<DiseaseRequestDraftState> = _diseaseRequestDraft.asStateFlow()

    private var persistedSettings = AppSettingsEntity()
    private var liveFinalizationId: String? = null
    private val shareCelebrationLock = Any()
    private val armedShareCelebrations = mutableSetOf<String>()

    init {
        repository?.let { localRepository ->
            viewModelScope.launch {
                localRepository.cloudConfiguration.collect { configured ->
                    _cloudConfigured.value = configured
                }
            }
            viewModelScope.launch {
                localRepository.history.collect { savedHistory ->
                    _history.value = savedHistory
                    _lastScan.value = savedHistory.firstOrNull()
                }
            }
            viewModelScope.launch {
                localRepository.settings.collect { settings ->
                    persistedSettings = settings
                    _themePreference.value = enumValueOrDefault(settings.theme, ThemePreference.SYSTEM)
                    legacyUnitSystem = settings.unitSystem
                    _languagePreference.value = LanguagePreference.entries.firstOrNull {
                        it.languageTag == settings.languageTag
                    } ?: LanguagePreference.ENGLISH
                    _detectHealthyLeafEnabled.value = settings.detectHealthyLeafEnabled
                    _detectHealthyPlantEnabled.value = settings.detectHealthyPlantEnabled
                    _globalSharingEnabled.value = settings.globalSharingEnabled
                    _motionPreference.value = enumValueOrDefault(settings.motionPreference, MotionPreference.SYSTEM)
                }
            }
            viewModelScope.launch {
                localRepository.readNotificationKeys.collect { _readNotificationKeys.value = it }
            }
            viewModelScope.launch {
                localRepository.remoteNotifications.collect { _remoteNotifications.value = it }
            }
            viewModelScope.launch {
                _languagePreference.collectLatest { language ->
                    localRepository.catalog(language.languageTag)
                        .filter { it.isNotEmpty() }
                        .collect { _catalog.value = it }
                }
            }
            viewModelScope.launch { localRepository.globalScans.collect { _globalScans.value = it } }
            viewModelScope.launch { localRepository.globalRankings.collect { _globalRankings.value = it } }
            viewModelScope.launch { localRepository.globalFeedState.collect { _globalFeedState.value = it } }
            viewModelScope.launch { localRepository.cloudDeletionState.collect { _cloudDeletionState.value = it } }
            viewModelScope.launch { localRepository.syncOutboxEvents.collect { _syncOutboxEvents.value = it } }
            viewModelScope.launch { localRepository.diseaseRequests.collect { _diseaseRequests.value = it } }
        }
    }

    fun openDetectionScene(
        scene: CameraScene,
        primary: DetectionBox,
        onReady: () -> Unit = {},
    ) {
        _saveState.value = SaveState.IDLE
        _cloudActionState.value = CloudActionState.Idle
        _resultWarning.value = null
        val localRepository = repository
        val stageSnapshot = snapshotStager ?: localRepository?.let { repo ->
            suspend { frame: com.eggplant.detector.detection.api.RgbFrame -> repo.stageSnapshot(frame) }
        }
        val pendingResult = scene.toScanResult(
            primary,
            imagePath = null,
            _catalog.value,
            nowProvider(),
            _languagePreference.value.languageTag,
        )
        _currentResult.value = pendingResult
        if (stageSnapshot == null) {
            _snapshotState.value = SnapshotState.UNAVAILABLE
            _resultWarning.value = ResultWarning.SNAPSHOT_UNAVAILABLE
            persistCompletedResult(pendingResult, automaticSaveMode(scene.rgbFrame.source.name))
            onReady()
            return
        }
        _snapshotState.value = SnapshotState.PREPARING
        onReady()
        viewModelScope.launch {
            val imagePath = runCatching { stageSnapshot(scene.rgbFrame) }
                .onFailure { _resultWarning.value = ResultWarning.SNAPSHOT_UNAVAILABLE }
                .getOrNull()
            if (_currentResult.value?.id == pendingResult.id) {
                val readyResult = pendingResult.copy(imagePath = imagePath)
                _currentResult.value = readyResult
                _snapshotState.value = if (imagePath == null) SnapshotState.UNAVAILABLE else SnapshotState.READY
                persistCompletedResult(readyResult, automaticSaveMode(scene.rgbFrame.source.name))
            }
        }
    }

    /**
     * Called only when the long-press live session is released with a retained
     * result. The Result route opens immediately with the detected data while
     * photo staging and the required local save continue in this ViewModel
     * scope. The result screen observes the save state as it completes.
     */
    fun finalizeLiveDetectionScene(
        scene: CameraScene,
        primary: DetectionBox,
        onReady: () -> Unit = {},
    ) {
        if (liveFinalizationId != null) return
        _saveState.value = SaveState.IDLE
        _cloudActionState.value = CloudActionState.Idle
        _resultWarning.value = null
        val pendingResult = scene.toScanResult(
            primary,
            imagePath = null,
            _catalog.value,
            nowProvider(),
            _languagePreference.value.languageTag,
        )
        val finalizationId = pendingResult.id
        liveFinalizationId = finalizationId
        _currentResult.value = pendingResult
        val stageSnapshot = snapshotStager ?: repository?.let { repo ->
            suspend { frame: com.eggplant.detector.detection.api.RgbFrame -> repo.stageSnapshot(frame) }
        }
        _snapshotState.value = if (stageSnapshot == null) SnapshotState.UNAVAILABLE else SnapshotState.PREPARING
        if (stageSnapshot == null) _resultWarning.value = ResultWarning.SNAPSHOT_UNAVAILABLE

        // Keep the no-storage/no-repository path deterministic for previews
        // and JVM tests. Production always has a repository and follows the
        // asynchronous snapshot + Room transaction below.
        if (stageSnapshot == null && scanSaver == null) {
            persistCompletedResult(pendingResult, "LIVE")
            completeLiveFinalization(finalizationId)
            onReady()
            return
        }

        // Open the result immediately. JPEG staging and the Room transaction
        // continue below so a slow local save cannot block the live release.
        _saveState.value = SaveState.SAVING
        onReady()
        viewModelScope.launch {
            val imagePath = stageSnapshot?.let { stage ->
                runCatching { stage(scene.rgbFrame) }
                    .onFailure { error ->
                        Log.e(LOG_TAG, "live_snapshot_stage_failed", error)
                        _resultWarning.value = ResultWarning.SNAPSHOT_UNAVAILABLE
                    }
                    .getOrNull()
            }
            if (liveFinalizationId != finalizationId) {
                repository?.discardSnapshot(imagePath)
                return@launch
            }
            val readyResult = pendingResult.copy(imagePath = imagePath)
            _currentResult.value = readyResult
            _snapshotState.value = if (imagePath == null) SnapshotState.UNAVAILABLE else SnapshotState.READY

            persistCompletedResult(readyResult, "LIVE")
            completeLiveFinalization(finalizationId)
        }
    }

    fun openHistoryResult(result: ScanResult) {
        _cloudActionState.value = CloudActionState.Idle
        _resultWarning.value = null
        _snapshotState.value = if (result.imagePath == null) SnapshotState.UNAVAILABLE else SnapshotState.READY
        _currentResult.value = result
    }

    fun toggleHistoryFavorite(id: String) {
        val result = _history.value.firstOrNull { it.id == id }
            ?: _currentResult.value?.takeIf { it.id == id }
            ?: return
        val updated = result.copy(isFavorite = !result.isFavorite)
        replaceHistoryResult(updated)
        repository?.let { localRepository ->
            viewModelScope.launch {
                runCatching { localRepository.setHistoryFavorite(id, updated.isFavorite) }
                    .onFailure { replaceHistoryResult(result) }
            }
        }
    }

    fun deleteHistory(id: String, onDeleted: () -> Unit = {}) {
        val result = _history.value.firstOrNull { it.id == id } ?: return
        val localRepository = repository
        if (localRepository == null) {
            removeHistoryResult(result)
            onDeleted()
            return
        }
        viewModelScope.launch {
            runCatching { localRepository.deleteHistory(id) }
                .onSuccess {
                    removeHistoryResult(result)
                    onDeleted()
                }
                .onFailure { error -> Log.e(LOG_TAG, "history_delete_failed", error) }
        }
    }

    fun openNoMatchScene(
        scene: CameraScene,
        onReady: () -> Unit = {},
    ) {
        _saveState.value = SaveState.IDLE
        _cloudActionState.value = CloudActionState.Idle
        _resultWarning.value = null
        val localRepository = repository
        val stageSnapshot = snapshotStager ?: localRepository?.let { repo ->
            suspend { frame: com.eggplant.detector.detection.api.RgbFrame -> repo.stageSnapshot(frame) }
        }
        val pendingResult = scene.toNoMatchScanResult(
            imagePath = null,
            timestamp = nowProvider(),
            languageTag = _languagePreference.value.languageTag,
        )
        _currentResult.value = pendingResult
        if (stageSnapshot == null) {
            _snapshotState.value = SnapshotState.UNAVAILABLE
            _resultWarning.value = ResultWarning.SNAPSHOT_UNAVAILABLE
            persistCompletedResult(pendingResult, automaticSaveMode(scene.rgbFrame.source.name))
            onReady()
            return
        }
        _snapshotState.value = SnapshotState.PREPARING
        onReady()
        viewModelScope.launch {
            val imagePath = runCatching { stageSnapshot(scene.rgbFrame) }
                .onFailure { _resultWarning.value = ResultWarning.SNAPSHOT_UNAVAILABLE }
                .getOrNull()
            if (_currentResult.value?.id == pendingResult.id) {
                val readyResult = pendingResult.copy(imagePath = imagePath)
                _currentResult.value = readyResult
                _snapshotState.value = if (imagePath == null) SnapshotState.UNAVAILABLE else SnapshotState.READY
                persistCompletedResult(readyResult, automaticSaveMode(scene.rgbFrame.source.name))
            }
        }
    }

    private fun persistCompletedResult(result: ScanResult, saveMode: String) {
        if (_history.value.any { it.id == result.id }) {
            _saveState.value = SaveState.ALREADY_SAVED
            return
        }
        val savedResult = result.copy(scannedAt = nowProvider(), saveMode = saveMode)
        _saveState.value = SaveState.SAVING
        val persist = scanSaver
        if (persist == null) {
            commitSavedResult(savedResult, SaveState.SAVED)
            return
        }
        viewModelScope.launch {
            try {
                val committed = persist(savedResult)
                val favoriteOverride = _currentResult.value
                    ?.takeIf { it.id == savedResult.id }
                    ?.isFavorite
                    ?: committed.isFavorite
                if (favoriteOverride != committed.isFavorite) {
                    repository?.setHistoryFavorite(committed.id, favoriteOverride)
                }
                commitSavedResult(committed.copy(isFavorite = favoriteOverride), SaveState.SAVED)
            } catch (error: Exception) {
                Log.e(LOG_TAG, "history_save_failed", error)
                repository?.discardSnapshot(savedResult.imagePath)
                _saveState.value = SaveState.FAILED
            }
        }
    }

    private fun commitSavedResult(result: ScanResult, completedState: SaveState = SaveState.SAVED) {
        if (_history.value.none { it.id == result.id }) {
            _history.value = listOf(result) + _history.value
        }
        _currentResult.value = result
        _lastScan.value = result
        val hadReadySnapshot = _snapshotState.value == SnapshotState.READY
        _snapshotState.value = if (result.imagePath == null) SnapshotState.UNAVAILABLE else SnapshotState.READY
        if (hadReadySnapshot && result.imagePath == null) {
            // History is still durable when photo storage is unavailable, but
            // global sharing must remain disabled until a real photo exists.
            _resultWarning.value = ResultWarning.SNAPSHOT_UNAVAILABLE
        }
        _saveState.value = completedState
    }

    private fun replaceHistoryResult(result: ScanResult) {
        _history.value = _history.value.map { if (it.id == result.id) result else it }
        if (_currentResult.value?.id == result.id) _currentResult.value = result
        _lastScan.value = _history.value.firstOrNull()
    }

    private fun removeHistoryResult(result: ScanResult) {
        _history.value = _history.value.filterNot { it.id == result.id }
        if (_currentResult.value?.id == result.id) _currentResult.value = null
        _lastScan.value = _history.value.firstOrNull()
    }

    fun setTheme(preference: ThemePreference) {
        _themePreference.value = preference
        persistSettings()
    }

    fun setLanguage(preference: LanguagePreference) {
        _languagePreference.value = preference
        repository?.let { localRepository ->
            val settings = settingsSnapshot()
            persistedSettings = settings
            viewModelScope.launch {
                localRepository.saveSettings(settings)
                localRepository.refreshCloud()
            }
        }
    }

    fun setDetectHealthyLeaf(enabled: Boolean) {
        _detectHealthyLeafEnabled.value = enabled
        persistSettings()
    }

    fun setDetectHealthyPlant(enabled: Boolean) {
        _detectHealthyPlantEnabled.value = enabled
        persistSettings()
    }

    fun setGlobalSharing(enabled: Boolean) {
        if (enabled && repository?.isCloudConfigured == false) {
            _globalSharingEnabled.value = false
            _cloudActionState.value = CloudActionState.Error(cloudMessage("Cloud is unavailable in this build.", "Hindi available ang cloud sa build na ito."))
            return
        }
        _globalSharingEnabled.value = enabled
        persistedSettings = persistedSettings.copy(
            globalSharingEnabled = enabled,
            sharingConsentVersion = if (enabled) 1 else null,
            sharingConsentedAt = if (enabled) java.time.Instant.now().toString() else null,
        )
        repository?.let { localRepository ->
            val settings = persistedSettings
            viewModelScope.launch {
                val consentResult = runCatching { localRepository.setSharingConsent(enabled) }
                val settingsResult = runCatching { localRepository.saveSettings(settings) }
                (consentResult.exceptionOrNull() ?: settingsResult.exceptionOrNull())?.let { error ->
                    _cloudActionState.value = CloudActionState.Error(
                        error.message ?: cloudMessage(
                            "Could not update anonymous sharing.",
                            "Hindi ma-update ang anonymous sharing.",
                        ),
                    )
                }
            }
        }
    }

    fun setMotionPreference(preference: MotionPreference) {
        _motionPreference.value = preference
        persistSettings()
    }

    fun refreshGlobalScans() {
        val localRepository = repository
        if (localRepository == null) {
            _cloudActionState.value = CloudActionState.Error(cloudMessage("Cloud is unavailable in this build.", "Hindi available ang cloud sa build na ito."))
            return
        }
        _cloudActionState.value = CloudActionState.Working
        // The worker also bootstraps the public configuration. This lets a
        // user recover after opening the app offline and coming back online.
        localRepository.refreshGlobalScans()
        _cloudActionState.value = CloudActionState.Queued("Refreshing Global Scans")
    }

    /** Refresh catalog, request status, and the public feed without changing the selected screen. */
    fun refreshCloudData() {
        repository?.refreshCloud()
    }

    fun loadMoreGlobalScans() {
        if (_globalFeedState.value.isLoading || !_globalFeedState.value.hasMore) return
        val localRepository = repository
        if (localRepository == null) {
            _cloudActionState.value = CloudActionState.Error(cloudMessage("Cloud is unavailable in this build.", "Hindi available ang cloud sa build na ito."))
            return
        }
        localRepository.loadMoreGlobalScans()
    }

    /**
     * Queues a share after the user confirms the publication dialog. That
     * dialog is also the explicit consent action, so the caller may pass true
     * to atomically queue consent and the share in the repository transaction.
     * This avoids racing an asynchronous settings write against the share.
     */
    fun shareCurrentResult(allowSharingConsent: Boolean = false) {
        if (_cloudActionState.value == CloudActionState.Working) return
        val localRepository = repository ?: return
        if (!localRepository.isCloudConfigured) {
            _cloudActionState.value = CloudActionState.Error(cloudMessage("Cloud is unavailable in this build.", "Hindi available ang cloud sa build na ito."))
            return
        }
        val result = _currentResult.value ?: run {
            _cloudActionState.value = CloudActionState.Error(cloudMessage("No result is available to share.", "Walang resultang maaaring i-share."))
            return
        }
        val existingShare = _syncOutboxEvents.value.firstOrNull { it.idempotencyKey == "global:${result.id}" }
        if (existingShare?.state in setOf(
                com.eggplant.detector.domain.model.SyncOutboxState.PENDING,
                com.eggplant.detector.domain.model.SyncOutboxState.UPLOADING,
                com.eggplant.detector.domain.model.SyncOutboxState.RETRY,
                com.eggplant.detector.domain.model.SyncOutboxState.COMPLETED,
            )
        ) return
        _cloudActionState.value = CloudActionState.Working
        armShareSuccessAnimation(result.id)
        val sharingWasEnabled = _globalSharingEnabled.value
        val sharingEnabledForShare = sharingWasEnabled || allowSharingConsent
        val previousSettings = persistedSettings
        viewModelScope.launch {
            runCatching { localRepository.enqueueGlobalShare(result, sharingEnabledForShare) }
                .onSuccess { eligibility ->
                    if (eligibility == ShareEligibility.Eligible && allowSharingConsent && !sharingWasEnabled) {
                        _globalSharingEnabled.value = true
                        persistedSettings = previousSettings.copy(
                            globalSharingEnabled = true,
                            sharingConsentVersion = 1,
                            sharingConsentedAt = java.time.Instant.now().toString(),
                        )
                        persistSettings()
                    }
                    _cloudActionState.value = shareActionState(eligibility)
                }
                .onFailure {
                    _cloudActionState.value = CloudActionState.Error(it.message ?: "Could not queue share")
                }
        }
    }

    fun beginDiseaseRequest() {
        val result = _currentResult.value
        val existingRequest = result?.let { current ->
            _syncOutboxEvents.value.firstOrNull { it.idempotencyKey == "request:${diseaseRequestClientId(current.id)}" }
        }
        if (existingRequest != null) {
            _diseaseRequestDraft.value = DiseaseRequestDraftState(
                error = cloudMessage(
                    "This disease request has already been submitted for this scan.",
                    "Naipasa na ang disease request para sa scan na ito.",
                ),
            )
            return
        }
        val photoPath = result?.imagePath
        val source = result?.source
        _diseaseRequestDraft.value = if (
            photoPath != null && java.io.File(photoPath).isFile && source in CAMERA_REQUEST_SOURCES
        ) {
            DiseaseRequestDraftState(photoPaths = listOf(photoPath), photoSources = listOf(requireNotNull(source)))
        } else {
            DiseaseRequestDraftState(error = cloudMessage("Choose or capture a real plant photo before requesting a disease.", "Pumili o kumuha muna ng tunay na larawan ng halaman bago humiling ng disease."))
        }
    }

    fun cancelDiseaseRequestDraft() {
        _diseaseRequestDraft.value = DiseaseRequestDraftState()
    }

    fun submitDiseaseRequest(
        requestedName: String?,
        notes: String?,
        rightsConsent: Boolean,
        onComplete: (Boolean) -> Unit = {},
    ) {
        if (_cloudActionState.value == CloudActionState.Working) return
        val localRepository = repository ?: return
        if (!localRepository.isCloudConfigured) {
            _cloudActionState.value = CloudActionState.Error(cloudMessage("Cloud is unavailable in this build.", "Hindi available ang cloud sa build na ito."))
            onComplete(false)
            return
        }
        val draft = _diseaseRequestDraft.value
        if (draft.photoPaths.isEmpty()) {
            _cloudActionState.value = CloudActionState.Error(cloudMessage("A real plant photo is required.", "Kailangan ang tunay na larawan ng halaman."))
            onComplete(false)
            return
        }
        if (!rightsConsent) {
            _cloudActionState.value = CloudActionState.Error(cloudMessage("Photo permission is required.", "Kailangan ang pahintulot para sa larawan."))
            onComplete(false)
            return
        }
        if (notes?.length ?: 0 > DISEASE_REQUEST_NOTES_MAX_LENGTH) {
            _cloudActionState.value = CloudActionState.Error(cloudMessage("Notes must be 200 characters or fewer.", "Hanggang 200 character lamang ang notes."))
            onComplete(false)
            return
        }
        if (draft.photoSources.size != draft.photoPaths.size || draft.photoSources.any { it !in CAMERA_REQUEST_SOURCES }) {
            _cloudActionState.value = CloudActionState.Error(cloudMessage("Only real plant photos from the camera or gallery can be submitted.", "Tanging tunay na larawan ng halaman mula sa camera o gallery ang maaaring isumite."))
            onComplete(false)
            return
        }
        val result = _currentResult.value
        val existingRequest = result?.let { current ->
            _syncOutboxEvents.value.firstOrNull { it.idempotencyKey == "request:${diseaseRequestClientId(current.id)}" }
        }
        if (existingRequest != null) {
            _cloudActionState.value = CloudActionState.Error(
                cloudMessage(
                    "This disease request has already been submitted for this scan.",
                    "Naipasa na ang disease request para sa scan na ito.",
                ),
            )
            onComplete(false)
            return
        }
        _cloudActionState.value = CloudActionState.Working
        viewModelScope.launch {
            runCatching {
                localRepository.enqueueDiseaseRequest(
                    requestedName,
                    notes,
                    draft.photoPaths,
                    draft.photoSources,
                    rightsConsent,
                    clientRequestId = result?.let { diseaseRequestClientId(it.id) },
                )
            }
                .onSuccess {
                    _diseaseRequestDraft.value = DiseaseRequestDraftState()
                    _cloudActionState.value = CloudActionState.Queued("Disease request queued")
                    onComplete(true)
                }
                .onFailure {
                    _cloudActionState.value = CloudActionState.Error(it.message ?: "Could not queue disease request")
                    onComplete(false)
                }
        }
    }

    fun retryDiseaseRequest(clientRequestId: String) {
        val localRepository = repository ?: return
        if (!localRepository.isCloudConfigured) {
            _cloudActionState.value = CloudActionState.Error(cloudMessage("Cloud is unavailable in this build.", "Hindi available ang cloud sa build na ito."))
            return
        }
        viewModelScope.launch {
            runCatching { localRepository.retryDiseaseRequest(clientRequestId) }
                .onFailure { _cloudActionState.value = CloudActionState.Error(it.message ?: "Could not retry request") }
        }
    }

    fun cancelDiseaseRequest(clientRequestId: String) {
        val localRepository = repository ?: return
        viewModelScope.launch {
            runCatching { localRepository.cancelDiseaseRequest(clientRequestId) }
                .onFailure { _cloudActionState.value = CloudActionState.Error(it.message ?: "Could not cancel request") }
        }
    }

    fun retryOutboxEvent(eventId: String) {
        val localRepository = repository ?: return
        if (!localRepository.isCloudConfigured) {
            _cloudActionState.value = CloudActionState.Error(cloudMessage("Cloud is unavailable in this build.", "Hindi available ang cloud sa build na ito."))
            return
        }
        viewModelScope.launch {
            runCatching { localRepository.retryOutboxEvent(eventId) }
                .onFailure { _cloudActionState.value = CloudActionState.Error(it.message ?: "Could not retry cloud work") }
        }
    }

    fun clearCloudActionState() { _cloudActionState.value = CloudActionState.Idle }

    fun consumeShareSuccessAnimation(resultId: String): Boolean = synchronized(shareCelebrationLock) {
        armedShareCelebrations.remove(resultId)
    }

    fun abandonShareSuccessAnimation(resultId: String) = synchronized(shareCelebrationLock) {
        armedShareCelebrations.remove(resultId)
    }

    private fun armShareSuccessAnimation(resultId: String) = synchronized(shareCelebrationLock) {
        armedShareCelebrations += resultId
    }

    fun reportGlobalScan(scanId: String) {
        val localRepository = repository ?: run {
            val state = CloudActionState.Error(cloudMessage("Reporting is unavailable in this build.", "Hindi available ang pag-report sa build na ito."))
            _cloudActionState.value = state
            _reportActionState.value = ReportActionState(scanId, state)
            return
        }
        if (!localRepository.isCloudConfigured) {
            val state = CloudActionState.Error(cloudMessage("Cloud is unavailable in this build.", "Hindi available ang cloud sa build na ito."))
            _cloudActionState.value = state
            _reportActionState.value = ReportActionState(scanId, state)
            return
        }
        _cloudActionState.value = CloudActionState.Working
        _reportActionState.value = ReportActionState(scanId, CloudActionState.Working)
        viewModelScope.launch {
            runCatching { localRepository.enqueueContentReport(scanId, "incorrect_result") }
                .onSuccess {
                    val state = CloudActionState.Queued("Report saved. It will send when you’re online.")
                    _cloudActionState.value = state
                    _reportActionState.value = ReportActionState(scanId, state)
                }
                .onFailure {
                    val state = CloudActionState.Error(it.message ?: "Could not report scan")
                    _cloudActionState.value = state
                    _reportActionState.value = ReportActionState(scanId, state)
                }
        }
    }

    fun deleteSharedCloudData() {
        val localRepository = repository ?: return
        if (!localRepository.isCloudConfigured) {
            _cloudActionState.value = CloudActionState.Error(cloudMessage("Cloud is unavailable in this build.", "Hindi available ang cloud sa build na ito."))
            return
        }
        _cloudActionState.value = CloudActionState.Working
        viewModelScope.launch {
            runCatching { localRepository.enqueueCloudDeletion() }
                .onSuccess { _cloudActionState.value = CloudActionState.Queued("Cloud deletion queued") }
                .onFailure { _cloudActionState.value = CloudActionState.Error(it.message ?: "Could not queue deletion") }
        }
    }

    private fun completeLiveFinalization(finalizationId: String, onReady: (() -> Unit)? = null) {
        if (liveFinalizationId != finalizationId) return
        liveFinalizationId = null
        onReady?.invoke()
    }

    fun markNotificationRead(key: String) {
        _readNotificationKeys.value = _readNotificationKeys.value + key
        repository?.let { localRepository ->
            viewModelScope.launch { localRepository.markNotificationRead(key) }
        }
    }

    fun markAllNotificationsRead(keys: Collection<String>) {
        keys.forEach(::markNotificationRead)
    }

    private fun cloudMessage(english: String, filipino: String): String =
        if (_languagePreference.value == LanguagePreference.FILIPINO) filipino else english

    private fun persistSettings() {
        repository?.let { localRepository ->
            val settings = settingsSnapshot()
            persistedSettings = settings
            viewModelScope.launch { localRepository.saveSettings(settings) }
        }
    }

    private fun settingsSnapshot(): AppSettingsEntity = persistedSettings.copy(
        languageTag = _languagePreference.value.languageTag,
        theme = _themePreference.value.name,
        unitSystem = legacyUnitSystem,
        detectHealthyLeafEnabled = _detectHealthyLeafEnabled.value,
        detectHealthyPlantEnabled = _detectHealthyPlantEnabled.value,
        globalSharingEnabled = _globalSharingEnabled.value,
        motionPreference = _motionPreference.value.name,
    )

    companion object {
        fun factory(repository: EggplantRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EggplantAppViewModel(initialHistory = emptyList(), repository = repository) as T
            }
    }
}

private const val LOG_TAG = "EggplantDetection"
private const val DISEASE_REQUEST_NOTES_MAX_LENGTH = 200
private val CAMERA_REQUEST_SOURCES = setOf("live", "capture", "gallery")

internal fun diseaseRequestClientId(scanId: String): String = runCatching {
    UUID.fromString(scanId).toString()
}.getOrElse {
    UUID.nameUUIDFromBytes("disease-request:$scanId".toByteArray(Charsets.UTF_8)).toString()
}

private fun automaticSaveMode(source: String): String = when (source.lowercase()) {
    "live" -> "LIVE"
    "capture" -> "CAPTURE"
    "gallery" -> "GALLERY"
    else -> source.uppercase()
}

private fun CameraScene.toScanResult(
    primary: DetectionBox,
    imagePath: String?,
    catalog: List<com.eggplant.detector.domain.model.Disease>,
    timestamp: LocalDateTime,
    languageTag: String,
): ScanResult {
    val displayDetections = stability.confirmedDetections.ifEmpty {
        detectionFrame.detections
    }
    val primaryDiseaseId = primary.modelClass.diseaseId
    val primaryDisease = primaryDiseaseId?.let { diseaseId ->
        DiseaseContentResolver.resolve(diseaseId, catalog, languageTag)
    }
    val primaryName = primaryDisease?.name ?: if (primary.modelClass.isHealthy) {
        healthyDisplayName(primary.modelClass.index, languageTag)
    } else {
        primary.modelClass.modelLabel.replace('_', ' ').replace('-', ' ')
    }
    return ScanResult(
        id = UUID.randomUUID().toString(),
        name = primaryName,
        category = when (primaryDisease?.type) {
            DiseaseType.LEAF_DISEASE -> ScanCategory.LEAF_DISEASE
            DiseaseType.FRUIT_DISEASE -> ScanCategory.FRUIT_DISEASE
            null -> ScanCategory.NO_DISEASE_DETECTED
        },
        outcome = if (primary.modelClass.isHealthy) ScanOutcome.HEALTHY_CONFIRMED else ScanOutcome.DISEASE,
        confidence = (primary.confidence * 100).roundToInt().coerceIn(0, 100),
        scannedAt = timestamp,
        signs = primaryDisease?.signs.orEmpty(),
        treatment = primaryDisease?.treatment.orEmpty(),
        diseaseId = primaryDiseaseId ?: healthyClassId(primary.modelClass.index),
        source = rgbFrame.source.name.lowercase(),
        modelVersion = ModelMetadata.EGGPLANT_YOLO26M.modelVersion,
        imagePath = imagePath,
        detections = displayDetections.mapIndexed { index, detection ->
            val diseaseId = detection.modelClass.diseaseId ?: healthyClassId(detection.modelClass.index)
            ScanDetectionResult(
                id = "detection-$index-${UUID.randomUUID()}",
                diseaseId = diseaseId,
                name = DiseaseContentResolver.resolve(diseaseId, catalog, languageTag)?.name
                    ?: if (detection.modelClass.isHealthy) {
                        healthyDisplayName(detection.modelClass.index, languageTag)
                    } else {
                        detection.modelClass.modelLabel.replace('_', ' ').replace('-', ' ')
                    },
                modelClassIndex = detection.modelClass.index,
                modelLabel = detection.modelClass.modelLabel,
                confidence = (detection.confidence * 100).roundToInt().coerceIn(0, 100),
                bounds = detection.bounds,
            )
        },
    )
}

private fun CameraScene.toNoMatchScanResult(
    imagePath: String?,
    timestamp: LocalDateTime,
    languageTag: String,
): ScanResult = ScanResult(
    id = UUID.randomUUID().toString(),
    name = if (languageTag in setOf("fil", "tl")) {
        "Walang suportadong sakit na natukoy"
    } else {
        "No supported disease detected"
    },
    category = ScanCategory.NO_DISEASE_DETECTED,
    outcome = ScanOutcome.NO_MATCH,
    confidence = 0,
    scannedAt = timestamp,
    signs = emptyList(),
    treatment = "",
    diseaseId = "no-match",
    source = rgbFrame.source.name.lowercase(),
    modelVersion = ModelMetadata.EGGPLANT_YOLO26M.modelVersion,
    imagePath = imagePath,
    detections = emptyList(),
)

private fun healthyClassId(classIndex: Int): String = when (classIndex) {
    ModelMetadata.HEALTHY_LEAF_CLASS_INDEX -> "healthy-leaf"
    ModelMetadata.HEALTHY_PLANT_CLASS_INDEX -> "healthy-plant"
    else -> error("Unknown healthy model class index: $classIndex")
}

private fun healthyDisplayName(classIndex: Int, languageTag: String): String {
    val filipino = languageTag in setOf("fil", "tl")
    return when (classIndex) {
        ModelMetadata.HEALTHY_LEAF_CLASS_INDEX -> if (filipino) "Malusog na Dahon" else "Healthy Leaf"
        ModelMetadata.HEALTHY_PLANT_CLASS_INDEX -> if (filipino) "Malusog na Halaman" else "Healthy Plant"
        else -> error("Unknown healthy model class index: $classIndex")
    }
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: default
