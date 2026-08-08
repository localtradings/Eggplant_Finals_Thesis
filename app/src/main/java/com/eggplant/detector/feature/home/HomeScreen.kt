package com.eggplant.detector.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggplant.detector.R
import com.eggplant.detector.app.EggplantAppViewModel
import com.eggplant.detector.core.ui.components.LastScanCard
import com.eggplant.detector.core.ui.components.ResponsiveContent

@Composable
fun HomeScreen(
    viewModel: EggplantAppViewModel,
    onScan: () -> Unit,
    onLibrary: () -> Unit,
    onHistory: () -> Unit,
    onNotifications: () -> Unit,
    onCareGuide: () -> Unit,
    onOfflineUse: () -> Unit,
    onLastScan: (String) -> Unit,
    listState: LazyListState,
) {
    val lastScan by viewModel.lastScan.collectAsState()
    val homeDescription = stringResource(R.string.home_content)

    ResponsiveContent {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .semantics { contentDescription = homeDescription },
            state = listState,
            contentPadding = PaddingValues(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { HomeHeader(onNotifications) }
            item { HeroCard(onScan) }
            item { QuickActions(onLibrary, onHistory, onCareGuide, onOfflineUse) }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.last_scan),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onHistory) {
                        Text(
                            stringResource(R.string.view_all),
                            color = homeAccentColor(),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
            lastScan?.let { result ->
                item { LastScanCard(result = result, onClick = { onLastScan(result.id) }) }
            } ?: run {
                item { EmptyRecentScans(onScan) }
            }
            item { ScanTip() }
        }
    }
}

@Composable
private fun HomeHeader(onNotifications: () -> Unit) {
    val accent = homeAccentColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.planta_logo),
            contentDescription = stringResource(R.string.home_logo_description),
            modifier = Modifier.size(50.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.home_eggplant),
                color = accent,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 26.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                stringResource(R.string.home_detector),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, lineHeight = 18.sp),
            )
        }
        Box {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f),
                shadowElevation = 0.dp,
            ) {
                IconButton(onClick = onNotifications, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Outlined.NotificationsNone,
                        contentDescription = stringResource(R.string.open_notifications),
                        tint = accent,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 5.dp, end = 5.dp)
                    .size(9.dp)
                    .background(accent, CircleShape),
            )
        }
    }
}

@Composable
private fun HeroCard(onScan: () -> Unit) {
    val headline = stringResource(R.string.home_headline)
    val accent = homeAccentColor()
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 400.dp
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (compact) Modifier.heightIn(min = 204.dp, max = 226.dp)
                    else Modifier.aspectRatio(1.58f),
                ),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (darkTheme) Color(0xFF1C2A20) else Color(0xFFEAF4E8),
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Box(Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(R.drawable.home_hero_eggplant_v2),
                    contentDescription = stringResource(R.string.hero_description),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                if (darkTheme) {
                                    listOf(
                                        Color(0xFF121018).copy(alpha = 0.94f),
                                        Color(0xFF121018).copy(alpha = 0.72f),
                                        Color.Transparent,
                                    )
                                } else {
                                    listOf(
                                        Color(0xFFEAF4E8).copy(alpha = 0.92f),
                                        Color(0xFFEAF4E8).copy(alpha = 0.66f),
                                        Color.Transparent,
                                    )
                                },
                            ),
                        ),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth(if (compact) 0.72f else 0.66f)
                        .padding(
                            start = if (compact) 18.dp else 24.dp,
                            top = if (compact) 16.dp else 22.dp,
                            end = 4.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 11.dp),
                ) {
                    Text(
                        buildAnnotatedString {
                            append(headline.substringBeforeLast('\n'))
                            append('\n')
                            withStyle(
                                SpanStyle(
                                    color = accent,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            ) {
                                append(headline.substringAfterLast('\n'))
                            }
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = if (compact) 20.sp else 23.sp,
                            lineHeight = if (compact) 25.sp else 29.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        stringResource(R.string.home_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = if (compact) 12.sp else 13.sp,
                            lineHeight = if (compact) 16.sp else 18.sp,
                        ),
                    )
                    Button(
                        onClick = onScan,
                        modifier = Modifier
                            .widthIn(min = 145.dp, max = 172.dp)
                            .heightIn(min = 46.dp)
                            .offset(y = 4.dp),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = if (compact) 11.dp else 15.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = if (darkTheme) MaterialTheme.colorScheme.onSecondary else Color.White,
                        ),
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(23.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.home_scan_cta),
                            fontSize = if (compact) 12.sp else 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActions(
    onLibrary: () -> Unit,
    onHistory: () -> Unit,
    onCareGuide: () -> Unit,
    onOfflineUse: () -> Unit,
) {
    val accent = homeAccentColor()
    val actions = listOf(
        QuickActionItem(stringResource(R.string.learn_diseases), R.drawable.home_quick_learn, onLibrary),
        QuickActionItem(stringResource(R.string.home_scan_history), R.drawable.home_quick_history, onHistory),
        QuickActionItem(stringResource(R.string.care_guide), R.drawable.home_quick_care, onCareGuide),
        QuickActionItem(stringResource(R.string.home_offline_mode), R.drawable.home_quick_offline, onOfflineUse),
    )

    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.quick_access),
            color = accent,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            actions.forEach { action ->
                QuickAccessItem(action, Modifier.weight(1f))
            }
        }
    }
}

private data class QuickActionItem(
    val title: String,
    val iconRes: Int,
    val onClick: () -> Unit,
)

@Composable
private fun QuickAccessItem(item: QuickActionItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clickable(onClick = item.onClick)
            .semantics(mergeDescendants = true) { contentDescription = item.title }
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Image(
            painter = painterResource(item.iconRes),
            contentDescription = null,
            modifier = Modifier
                .size(58.dp)
                .offset(x = if (item.iconRes == R.drawable.home_quick_offline) (-14).dp else 0.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            item.title,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium,
            ),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun EmptyRecentScans(onScan: () -> Unit) {
    val accent = homeAccentColor()
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.home_empty_scans),
                contentDescription = null,
                modifier = Modifier.size(width = 148.dp, height = 98.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                stringResource(R.string.no_recent_scans_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                stringResource(R.string.no_recent_scans_body),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 17.sp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(13.dp))
            Button(
                onClick = onScan,
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = if (darkTheme) MaterialTheme.colorScheme.onSecondary else Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 17.dp),
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.home_scan_cta), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ScanTip() {
    val accent = homeAccentColor()
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (darkTheme) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF2F7EE),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 82.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.home_care_tip),
                contentDescription = null,
                modifier = Modifier.size(width = 88.dp, height = 72.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    stringResource(R.string.daily_plant_care_tip),
                    color = accent,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    stringResource(R.string.home_tip),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    maxLines = 3,
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                color = if (darkTheme) MaterialTheme.colorScheme.surface else Color(0xFFEAF2E5),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(29.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun homeAccentColor(): Color {
    return MaterialTheme.colorScheme.primary
}
