package com.eggplant.detector.app.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggplant.detector.R
import com.eggplant.detector.core.ui.motion.LocalEggplantMotion
import com.eggplant.detector.domain.model.NavigationItem

private val NavigationItemShape = RoundedCornerShape(16.dp)

@Composable
fun BottomNavigationBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    val motion = LocalEggplantMotion.current
    val items = listOf(
        NavigationItem(Routes.HOME, stringResource(R.string.nav_home), Icons.Outlined.Home),
        NavigationItem(Routes.LIBRARY, stringResource(R.string.nav_library), Icons.AutoMirrored.Outlined.MenuBook),
        NavigationItem(Routes.CAMERA, stringResource(R.string.nav_camera), Icons.Filled.CameraAlt),
        NavigationItem(Routes.HISTORY, stringResource(R.string.nav_history), Icons.Outlined.CropFree),
        NavigationItem(Routes.SETTINGS, stringResource(R.string.nav_settings), Icons.Outlined.Settings),
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(76.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                if (item.route == Routes.CAMERA) {
                    CameraNavigationItem(
                        selected = currentRoute == item.route,
                        animationMillis = motion.fastMillis,
                        onClick = { onNavigate(Routes.CAMERA) },
                    )
                } else {
                    BottomNavigationItem(
                        item = item,
                        selected = currentRoute == item.route,
                        animationMillis = motion.fastMillis,
                        onClick = { onNavigate(item.route) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.BottomNavigationItem(
    item: NavigationItem,
    selected: Boolean,
    animationMillis: Int,
    onClick: () -> Unit,
) {
    val description = stringResource(R.string.navigate_to, item.label)
    val iconTint by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f)
        },
        animationSpec = tween(animationMillis),
        label = "bottomNavIconTint",
    )
    val labelTint by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f)
        },
        animationSpec = tween(animationMillis),
        label = "bottomNavLabelTint",
    )
    val selectionColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(animationMillis),
        label = "bottomNavSelectionColor",
    )
    val iconSize by animateDpAsState(
        targetValue = if (selected) 32.dp else 30.dp,
        animationSpec = tween(animationMillis),
        label = "bottomNavIconSize",
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = description
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(width = 64.dp, height = 42.dp),
            shape = NavigationItemShape,
            color = selectionColor,
            tonalElevation = 0.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = item.label,
            color = labelTint,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.CameraNavigationItem(
    selected: Boolean,
    animationMillis: Int,
    onClick: () -> Unit,
) {
    val cameraSize by animateDpAsState(
        targetValue = if (selected) 70.dp else 66.dp,
        animationSpec = tween(animationMillis),
        label = "bottomNavCameraSize",
    )
    val description = stringResource(R.string.open_camera)

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                role = Role.Button,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(74.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 0.dp,
            shadowElevation = 2.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(cameraSize),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    tonalElevation = 0.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
        }
    }
}
