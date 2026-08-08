package com.eggplant.detector.feature.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eggplant.detector.app.EggplantAppViewModel
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.eggplant.detector.R
import com.eggplant.detector.core.ui.components.ResponsiveContent

private data class LocalNotice(val key: String, @param:StringRes val title: Int, @param:StringRes val body: Int)

private data class DisplayNotice(
    val key: String,
    val title: String,
    val body: String,
    val category: String? = null,
)

private val notices = listOf(
    LocalNotice("welcome", R.string.notice_ready_title, R.string.notice_ready_body),
    LocalNotice("model", R.string.notice_model_title, R.string.notice_model_body),
    LocalNotice("tip", R.string.notice_tip_title, R.string.notice_tip_body),
    LocalNotice("privacy", R.string.notice_privacy_title, R.string.notice_privacy_body),
)

@Composable
fun NotificationsScreen(viewModel: EggplantAppViewModel, onBack: () -> Unit) {
    val readKeys by viewModel.readNotificationKeys.collectAsState()
    val remoteNotifications by viewModel.remoteNotifications.collectAsState()
    val diseaseRequests by viewModel.diseaseRequests.collectAsState()
    val languageTag by viewModel.languagePreference.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.refreshCloudData()
    }
    val localItems = notices.map { notice ->
        DisplayNotice(
            key = notice.key,
            title = stringResource(notice.title),
            body = stringResource(notice.body),
        )
    }
    val notificationItems = remoteNotifications.map { notice ->
        DisplayNotice(
            key = notice.key,
            title = notice.title(languageTag.languageTag),
            body = notice.body(languageTag.languageTag),
            category = notice.category,
        )
    } + diseaseRequests.mapNotNull { request ->
        diseaseRequestNotice(request, languageTag.languageTag)
    } + localItems
    ResponsiveContent {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Text(stringResource(R.string.notifications), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = { viewModel.markAllNotificationsRead(notificationItems.map(DisplayNotice::key)) }) {
                    Text(stringResource(R.string.mark_all_read))
                }
            }
        }
        items(notificationItems, key = DisplayNotice::key) { notice ->
            val isRead = notice.key in readKeys
            Card(
                onClick = { viewModel.markNotificationRead(notice.key) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    notice.category?.let { category ->
                        Text(category.replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(notice.title, fontWeight = FontWeight.SemiBold)
                    Text(notice.body, style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(if (isRead) R.string.read else R.string.new_notice), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        }
    }
}

private fun diseaseRequestNotice(
    request: com.eggplant.detector.domain.model.DiseaseRequest,
    languageTag: String,
): DisplayNotice? {
    val name = request.requestedName?.trim()?.takeIf { it.isNotEmpty() } ?: "the requested disease"
    val status = request.status.uppercase()
    val noticeKey = "disease-request:${request.id}:${status.lowercase()}"
    val filipino = languageTag in setOf("fil", "tl")
    return when (status) {
        "QUEUED", "UPLOADING", "RETRY" -> DisplayNotice(
            key = noticeKey,
            title = if (filipino) "Natanggap ang disease request" else "Disease request received",
            body = if (filipino) {
                "Naka-save ang request para sa $name at ipapadala kapag may internet. Susuriin ito pagkatapos ma-upload."
            } else {
                "Your request for $name is saved and will be reviewed after it uploads."
            },
            category = "disease request",
        )
        "SUBMITTED", "UNDER_REVIEW" -> DisplayNotice(
            key = noticeKey,
            title = if (filipino) "Sinusuri ang disease request" else "Disease request under review",
            body = if (filipino) {
                "Sinusuri na ng admin ang request para sa $name."
            } else {
                "An admin is reviewing your request for $name."
            },
            category = "disease request",
        )
        "PLANNED" -> DisplayNotice(
            key = noticeKey,
            title = if (filipino) "Tapos na ang disease request" else "Disease request completed",
            body = request.adminNote?.takeIf { it.isNotBlank() } ?: if (filipino) {
                "Nakumpleto na ang review para sa $name."
            } else {
                "The review for $name is complete."
            },
            category = "disease request",
        )
        "NOT_SUPPORTED", "CLOSED" -> DisplayNotice(
            key = noticeKey,
            title = if (filipino) "Update sa disease request" else "Disease request update",
            body = request.adminNote?.takeIf { it.isNotBlank() } ?: if (filipino) {
                "May update sa request para sa $name. Buksan ang My Scans para sa status."
            } else {
                "There is an update for your request for $name. Open My Scans to see its status."
            },
            category = "disease request",
        )
        else -> null
    }
}
