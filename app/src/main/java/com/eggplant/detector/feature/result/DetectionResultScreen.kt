package com.eggplant.detector.feature.result

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.eggplant.detector.app.EggplantAppViewModel
import com.eggplant.detector.R
import com.eggplant.detector.app.SaveState
import com.eggplant.detector.app.ResultWarning
import com.eggplant.detector.app.SnapshotState
import com.eggplant.detector.app.CloudActionState
import com.eggplant.detector.app.diseaseRequestClientId
import com.eggplant.detector.domain.model.SyncOutboxEvent
import com.eggplant.detector.domain.model.SyncOutboxState
import com.eggplant.detector.core.ui.components.ConfidenceDisplay
import com.eggplant.detector.core.ui.components.ResultArtwork
import com.eggplant.detector.core.ui.components.ResponsiveContent
import com.eggplant.detector.core.ui.components.ReportArtworkKind
import com.eggplant.detector.core.ui.components.ReportSectionArtwork
import com.eggplant.detector.domain.model.ScanOutcome
import com.eggplant.detector.domain.model.ScanResult
import com.eggplant.detector.domain.model.ScanCategory
import com.eggplant.detector.detection.api.DetectionBox
import com.eggplant.detector.detection.api.DetectionStatus
import com.eggplant.detector.detection.ncnn.ModelMetadata
import com.eggplant.detector.feature.camera.CameraAnalysisState
import com.eggplant.detector.feature.camera.DetectionOverlay
import com.eggplant.detector.feature.camera.OverlayContentScale
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.eggplant.detector.domain.model.Disease

@Composable
fun DetectionResultScreen(
    viewModel: EggplantAppViewModel,
    title: String,
    onBack: () -> Unit,
    onScanAgain: () -> Unit,
) {
    val result by viewModel.currentResult.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val resultWarning by viewModel.resultWarning.collectAsState()
    val snapshotState by viewModel.snapshotState.collectAsState()
    val cloudAction by viewModel.cloudActionState.collectAsState()
    val outboxEvents by viewModel.syncOutboxEvents.collectAsState()
    val requestDraft by viewModel.diseaseRequestDraft.collectAsState()
    val cloudConfigured by viewModel.cloudConfiguredState.collectAsState()
    var showShareDialog by remember { mutableStateOf(false) }
    var showRequestDialog by remember { mutableStateOf(false) }
    var requestedName by remember { mutableStateOf("") }
    var requestNotes by remember { mutableStateOf("") }
    var rightsConsent by remember { mutableStateOf(false) }
    val shareEvent = result?.let { current ->
        outboxEvents.firstOrNull { it.idempotencyKey == "global:${current.id}" }
    }
    val requestEvent = result?.let { current ->
        outboxEvents.firstOrNull { it.idempotencyKey == "request:${diseaseRequestClientId(current.id)}" }
    }
    var showShareCelebration by remember(result?.id) { mutableStateOf(false) }
    DisposableEffect(result?.id) {
        val resultId = result?.id
        onDispose { resultId?.let(viewModel::abandonShareSuccessAnimation) }
    }
    LaunchedEffect(result?.id, shareEvent?.id, shareEvent?.state) {
        val currentResult = result
        if (currentResult != null && shareEvent?.state == SyncOutboxState.COMPLETED &&
            viewModel.consumeShareSuccessAnimation(currentResult.id)
        ) {
            showShareCelebration = true
            delay(1_500)
            showShareCelebration = false
        }
    }
    Box(Modifier.fillMaxSize()) {
        ResultReport(
            result = result,
            disease = result?.let { current -> catalog.firstOrNull { it.id == current.diseaseId } },
            title = title,
            onBack = onBack,
            onToggleFavorite = { result?.let { current -> viewModel.toggleHistoryFavorite(current.id) } },
            actions = {
            if (snapshotState == SnapshotState.PREPARING) {
                Text(
                    localized("Preparing photo…", "Inihahanda ang larawan…"),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (resultWarning == ResultWarning.SNAPSHOT_UNAVAILABLE) {
                Text(
                    stringResource(R.string.snapshot_unavailable_warning),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (saveState == SaveState.SAVING) {
                Text(
                    stringResource(R.string.saving_scan_automatically),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (saveState == SaveState.FAILED) {
                Text(
                    stringResource(R.string.save_history_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (saveState == SaveState.SAVED) {
                Text(
                    stringResource(R.string.saved_scan_automatically),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (saveState == SaveState.ALREADY_SAVED) {
                Text(
                    stringResource(R.string.save_history_already_saved),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (result?.outcome == ScanOutcome.DISEASE) {
                if (!cloudConfigured) {
                    Text(
                        stringResource(R.string.cloud_unavailable_build),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedButton(
                    onClick = { showShareDialog = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    enabled = cloudConfigured && snapshotState == SnapshotState.READY && cloudAction != CloudActionState.Working && !shareEvent.isSubmitted(),
                ) { Text(stringResource(R.string.share_to_global)) }
                shareEvent?.let { event ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            shareStatusLabel(event),
                            color = if (event.state == SyncOutboxState.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (event.state in setOf(SyncOutboxState.FAILED, SyncOutboxState.RETRY)) {
                            TextButton(onClick = { viewModel.retryOutboxEvent(event.id) }) { Text(stringResource(R.string.retry)) }
                        }
                    }
                }
            }
            if (result?.outcome == ScanOutcome.NO_MATCH) {
                if (result?.source in setOf("live", "capture", "gallery")) {
                    if (requestEvent == null) {
                        OutlinedButton(
                            onClick = {
                                viewModel.beginDiseaseRequest()
                                showRequestDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(18.dp),
                            enabled = cloudConfigured && snapshotState == SnapshotState.READY && cloudAction != CloudActionState.Working,
                        ) { Text(stringResource(R.string.request_this_disease)) }
                    } else {
                        Text(
                            localized(
                                "This disease request has already been submitted for this scan.",
                                "Naipasa na ang disease request para sa scan na ito.",
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    Text(
                        stringResource(R.string.request_camera_only_photo),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            when (val action = cloudAction) {
                CloudActionState.Idle -> Unit
                CloudActionState.Working -> Text(localized("Working…", "Isinasagawa…"), color = MaterialTheme.colorScheme.primary)
                is CloudActionState.Queued -> Text(action.message, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                is CloudActionState.Error -> Text(action.message, color = MaterialTheme.colorScheme.error)
            }
            OutlinedButton(
                onClick = onScanAgain,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Text("  ${stringResource(R.string.scan_again)}")
            }
            },
        )
        if (showShareCelebration) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
                    .zIndex(10f),
                contentAlignment = Alignment.Center,
            ) {
                ShareSuccessCelebration(Modifier.padding(horizontal = 24.dp))
            }
        }
    }
    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text(stringResource(R.string.share_to_global)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    result?.let { SnapshotPreview(it, Modifier.fillMaxWidth().height(132.dp)) }
                    Text(localized("Publish this real scan photo anonymously? It will appear immediately after server validation and expire after 180 days.", "I-publish nang anonymous ang tunay na larawan ng scan na ito? Lalabas ito matapos ang server validation at mag-e-expire pagkalipas ng 180 araw."))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !shareEvent.isSubmitted(),
                    onClick = {
                    viewModel.shareCurrentResult(allowSharingConsent = true)
                    showShareDialog = false
                    },
                ) { Text(stringResource(R.string.share_to_global)) }
            },
            dismissButton = { TextButton(onClick = { showShareDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (showRequestDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.cancelDiseaseRequestDraft()
                showRequestDialog = false
            },
            title = { Text(stringResource(R.string.request_this_disease)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    result?.let { SnapshotPreview(it, Modifier.fillMaxWidth().height(132.dp)) }
                    Text(stringResource(R.string.real_photo_required))
                    Text(
                        stringResource(R.string.request_photo_count, requestDraft.photoPaths.size, 3),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.request_camera_only_photo),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    requestDraft.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    OutlinedTextField(
                        requestedName,
                        { requestedName = it.take(120) },
                        label = { Text(stringResource(R.string.requested_disease_name_optional)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        requestNotes,
                        { requestNotes = it.take(200) },
                        label = { Text(stringResource(R.string.optional_notes)) },
                        supportingText = { Text(stringResource(R.string.request_notes_count, requestNotes.length, 200)) },
                        minLines = 2,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(rightsConsent, { rightsConsent = it })
                        Text(localized("I own this photo or have permission to submit it.", "Ako ang may-ari ng larawan o may pahintulot akong isumite ito."))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = requestEvent == null && rightsConsent && requestDraft.photoPaths.isNotEmpty() &&
                        cloudAction != CloudActionState.Working,
                    onClick = {
                        viewModel.submitDiseaseRequest(
                            requestedName,
                            requestNotes.takeIf { it.isNotBlank() },
                            rightsConsent,
                        ) { success ->
                            if (success) {
                                showRequestDialog = false
                                requestedName = ""
                                requestNotes = ""
                                rightsConsent = false
                            }
                        }
                    },
                ) { Text(stringResource(R.string.submit_request)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.cancelDiseaseRequestDraft()
                    showRequestDialog = false
                }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun shareStatusLabel(event: SyncOutboxEvent): String = when (event.state) {
    SyncOutboxState.PENDING -> stringResource(R.string.share_status_queued)
    SyncOutboxState.UPLOADING -> stringResource(R.string.share_status_uploading)
    SyncOutboxState.RETRY -> stringResource(R.string.share_status_retrying)
    SyncOutboxState.COMPLETED -> stringResource(R.string.share_status_completed)
    SyncOutboxState.FAILED -> when (event.lastErrorCode) {
        "writes_paused", "sharing_consent_failed" -> stringResource(R.string.share_status_cloud_unavailable)
        else -> stringResource(R.string.share_status_failed)
    }
    SyncOutboxState.CANCELLED -> stringResource(R.string.share_status_cancelled)
}

private fun SyncOutboxEvent?.isSubmitted(): Boolean = this?.state in setOf(
    SyncOutboxState.PENDING,
    SyncOutboxState.UPLOADING,
    SyncOutboxState.RETRY,
    SyncOutboxState.COMPLETED,
)

@Composable
private fun ShareSuccessCelebration(modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            AndroidView(
                factory = ::ShareSuccessLottieView,
                modifier = Modifier.size(220.dp),
            )
            Text(
                localized(
                    "Uploaded to Global Scans successfully.",
                    "Matagumpay na na-upload sa Global Scans.",
                ),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private class ShareSuccessLottieView(context: Context) : View(context) {
    private val drawable = LottieDrawable()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        drawable.callback = this
        drawable.composition = LottieCompositionFactory
            .fromRawResSync(context, R.raw.global_share_success)
            .value
        drawable.addValueCallback(
            KeyPath("**", "White Solid 1"),
            LottieProperty.OPACITY,
            LottieValueCallback(0),
        )
        drawable.repeatCount = 0
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        drawable.callback = this
        drawable.playAnimation()
    }

    override fun onDetachedFromWindow() {
        drawable.cancelAnimation()
        drawable.callback = null
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        drawable.setBounds(0, 0, width, height)
    }

    override fun onDraw(canvas: Canvas) {
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
    }
}

@Composable
fun ResultReport(
    result: ScanResult?,
    disease: Disease? = null,
    title: String,
    onBack: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    if (result == null) {
        ResponsiveContent {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(localized("No scan result available", "Walang available na resulta ng scan"))
                OutlinedButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            }
        }
        return
    }

    var showDeleteConfirmation by remember(result.id) { mutableStateOf(false) }
    ResponsiveContent {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.close_detection_result))
                }
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (onToggleFavorite != null) {
                    val heartScale by animateFloatAsState(
                        targetValue = if (result.isFavorite) 1.16f else 1f,
                        label = "favorite-heart-scale",
                    )
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.scale(heartScale),
                    ) {
                        Icon(
                            imageVector = if (result.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(
                                if (result.isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites,
                            ),
                            tint = if (result.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.delete_history_scan),
                        )
                    }
                }
            }
            SnapshotPreview(result, Modifier.fillMaxWidth().aspectRatio(1.35f).heightIn(min = 180.dp, max = 320.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(localized("Detected result", "Natukoy na resulta"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(result.name, style = MaterialTheme.typography.headlineMedium)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                localizedCategory(result),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    ConfidenceDisplay(result.confidence)
                }
            }
            when (result.outcome) {
                ScanOutcome.DISEASE -> {
                    ReportSection(stringResource(R.string.description), disease?.description.orEmpty(), ReportArtworkKind.DESCRIPTION)
                    ReportSection(stringResource(R.string.symptoms), disease?.symptomPreview.orEmpty(), ReportArtworkKind.SYMPTOMS)
                    ReportSection(stringResource(R.string.signs_detected), (disease?.signs ?: result.signs).joinToString("\n") { "• $it" }, ReportArtworkKind.SIGNS)
                    val additional = result.detections
                        .filter { it.diseaseId != result.diseaseId }
                        .groupBy { it.diseaseId }
                        .mapNotNull { (_, detections) -> detections.maxByOrNull { it.confidence } }
                        .sortedByDescending { it.confidence }
                    if (additional.isNotEmpty()) {
                        ReportSection(
                            localized("Also detected", "Natukoy rin"),
                            additional.joinToString("\n") { "• ${it.name} — ${it.confidence}%" },
                        )
                    }
                    ReportSection(stringResource(R.string.causes), disease?.causes.orEmpty(), ReportArtworkKind.CAUSES)
                    ReportSection(stringResource(R.string.recommended_action), disease?.treatment ?: result.treatment, ReportArtworkKind.ACTIONS)
                    ReportSection(stringResource(R.string.prevention), disease?.prevention.orEmpty(), ReportArtworkKind.PREVENTION)
                    ReportSection(stringResource(R.string.guidance), disease?.guidance.orEmpty(), ReportArtworkKind.GUIDANCE)
                    ReportSection(stringResource(R.string.when_to_act), disease?.whenToAct.orEmpty(), ReportArtworkKind.WHEN_TO_ACT)
                    ReportSection(
                        stringResource(R.string.disclaimer),
                        disease?.disclaimer.orEmpty(),
                        ReportArtworkKind.DISCLAIMER,
                    )
                    ReportSection(
                        stringResource(R.string.references),
                        disease?.references.orEmpty().joinToString("\n") { reference ->
                            "${reference.publisher}: ${reference.title}\n${reference.url}"
                        },
                        ReportArtworkKind.REFERENCES,
                    )
                }
                ScanOutcome.HEALTHY_CONFIRMED -> {
                    ReportSection(
                        localized("Healthy result", "Malusog na resulta"),
                        localized(
                            "No supported disease was detected in this confirmed healthy area. This result is saved in My Scans automatically.",
                            "Walang suportadong sakit na nakita sa kumpirmadong malusog na bahaging ito. Awtomatikong sine-save ang resultang ito sa My Scans.",
                        ),
                    )
                }
                ScanOutcome.NO_MATCH -> {
                    ReportSection(
                        localized("No supported disease detected", "Walang suportadong sakit na natukoy"),
                        localized(
                            "The selected image loaded correctly, but the packaged detector did not confirm a supported eggplant disease. Try a closer, brighter, steadier photo.",
                            "Nabuksan nang tama ang napiling larawan, pero walang nakumpirmang suportadong sakit ng talong ang detector. Subukan ang mas malapit, mas maliwanag, at mas matatag na larawan.",
                        ),
                    )
                }
            }
            Text(
                localized("On-device model result for educational screening only. Confirm crop concerns with a qualified agricultural specialist.", "Resulta ng on-device model para lamang sa paunang pagsusuri. Kumpirmahin sa kwalipikadong espesyalista ang problema sa pananim."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            actions()
        }
        if (onDelete != null && showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text(stringResource(R.string.delete_history_scan)) },
                text = { Text(stringResource(R.string.delete_history_scan_confirmation)) },
                confirmButton = {
                    TextButton(onClick = { showDeleteConfirmation = false; onDelete() }) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun SnapshotPreview(result: ScanResult, modifier: Modifier = Modifier) {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, result.imagePath) {
        value = withContext(Dispatchers.IO) {
            result.imagePath?.let(::File)?.takeIf(File::isFile)?.let { BitmapFactory.decodeFile(it.absolutePath) }
        }
    }
    val snapshot = bitmap
    var viewerOpen by remember(result.imagePath) { mutableStateOf(false) }
    if (snapshot == null) {
        ResultArtwork(result.category, result.name, modifier, result.diseaseId)
        return
    }
    val openViewerDescription = stringResource(R.string.open_result_image_viewer)
    val overlayState = result.toOverlayState(snapshot.width, snapshot.height)
    val displayName = result.overlayDisplayName()
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
            .clickable(
                role = Role.Button,
                onClickLabel = openViewerDescription,
                onClick = { viewerOpen = true },
            )
            .semantics { contentDescription = openViewerDescription },
    ) {
        Image(
            bitmap = snapshot.asImageBitmap(),
            contentDescription = localized("Saved scan snapshot", "Naka-save na larawan ng scan"),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
        DetectionOverlay(
            state = overlayState,
            displayName = displayName,
            onDetectionClick = null,
            contentScale = OverlayContentScale.FIT,
        )
    }
    if (viewerOpen) {
        ZoomableResultImageDialog(
            snapshot = snapshot,
            overlayState = overlayState,
            displayName = displayName,
            onDismiss = { viewerOpen = false },
        )
    }
}

@Composable
private fun ZoomableResultImageDialog(
    snapshot: android.graphics.Bitmap,
    overlayState: CameraAnalysisState,
    displayName: (DetectionBox) -> String,
    onDismiss: () -> Unit,
) {
    var transform by remember(snapshot) { mutableStateOf(ZoomableImageTransform()) }
    var showBoxes by remember(snapshot) { mutableStateOf(true) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clipToBounds(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = transform.scale
                        scaleY = transform.scale
                        translationX = transform.offset.x
                        translationY = transform.offset.y
                    }
                    .pointerInput(snapshot) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            transform = transform.applyGesture(zoomChange = zoom, panChange = pan)
                        }
                    },
            ) {
                Image(
                    bitmap = snapshot.asImageBitmap(),
                    contentDescription = stringResource(R.string.saved_scan_snapshot),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
                if (showBoxes) {
                    DetectionOverlay(
                        state = overlayState,
                        displayName = displayName,
                        onDetectionClick = null,
                        contentScale = OverlayContentScale.FIT,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = .72f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.close_result_image_viewer),
                        tint = Color.White,
                    )
                }
                TextButton(onClick = { showBoxes = !showBoxes }) {
                    Text(
                        stringResource(if (showBoxes) R.string.hide_detection_boxes else R.string.show_detection_boxes),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

private fun ScanResult.toOverlayState(frameWidth: Int, frameHeight: Int): CameraAnalysisState {
    val overlayDetections = detections.mapNotNull { detection ->
        ModelMetadata.EGGPLANT_YOLO26M.classFor(detection.modelClassIndex)?.let { modelClass ->
            DetectionBox(modelClass, detection.confidence / 100f, detection.bounds)
        }
    }
    return CameraAnalysisState(
        status = when (outcome) {
            ScanOutcome.DISEASE -> DetectionStatus.DISEASE_DETECTED
            ScanOutcome.HEALTHY_CONFIRMED -> DetectionStatus.HEALTHY
            ScanOutcome.NO_MATCH -> DetectionStatus.SEARCHING
        },
        visibleDetections = overlayDetections,
        stableDetections = overlayDetections.filterNot { it.modelClass.isHealthy },
        confirmedDetections = overlayDetections,
        frameWidth = frameWidth,
        frameHeight = frameHeight,
    )
}

private fun ScanResult.overlayDisplayName(): (DetectionBox) -> String = { detection ->
    detections.firstOrNull { it.modelClassIndex == detection.modelClass.index }?.name
        ?: detection.modelClass.modelLabel.replace('_', ' ').replace('-', ' ')
}

@Composable
private fun localizedCategory(result: ScanResult): String = when (result.category) {
    ScanCategory.LEAF_DISEASE -> stringResource(R.string.leaf_disease)
    ScanCategory.FRUIT_DISEASE -> stringResource(R.string.fruit_disease)
    ScanCategory.NO_DISEASE_DETECTED -> when (result.outcome) {
        ScanOutcome.HEALTHY_CONFIRMED -> localized("Healthy", "Malusog")
        ScanOutcome.NO_MATCH -> localized("Unconfirmed", "Hindi kumpirmado")
        ScanOutcome.DISEASE -> localized("Unconfirmed", "Hindi kumpirmado")
    }
}

@Composable
private fun localized(english: String, filipino: String): String {
    val language = androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language
    return if (language == "fil" || language == "tl") filipino else english
}

@Composable
private fun ReportSection(title: String, body: String, artwork: ReportArtworkKind? = null) {
    if (body.isBlank()) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(body, style = MaterialTheme.typography.bodyLarge)
            }
            artwork?.let { ReportSectionArtwork(it, Modifier.size(86.dp)) }
        }
    }
}
