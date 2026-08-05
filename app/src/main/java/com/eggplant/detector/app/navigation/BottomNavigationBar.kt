package com.eggplant.detector.app.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

private val NavigationItemShape = RoundedCornerShape(14.dp)

@Composable
fun BottomNavigationBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    val motion = LocalEggplantMotion.current
    val items = listOf(
        NavigationItem(Routes.HOME, stringResource(R.string.nav_home), Icons.Rounded.Home),
        NavigationItem(Routes.LIBRARY, stringResource(R.string.nav_library), Icons.Rounded.Book),
        NavigationItem(Routes.CAMERA, stringResource(R.string.nav_camera), Icons.Rounded.CameraAlt),
        NavigationItem(Routes.HISTORY, stringResource(R.string.nav_history), Icons.Rounded.GridView),
        NavigationItem(Routes.SETTINGS, stringResource(R.string.nav_settings), Icons.Rounded.Tune),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(72.dp),
    ) {
        NavigationBarBackground()
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.Bottom,
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
        targetValue = if (selected) 26.dp else 24.dp,
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
            modifier = Modifier.size(width = 46.dp, height = 34.dp),
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
        Spacer(Modifier.height(1.dp))
        Text(
            text = item.label,
            color = labelTint,
            fontSize = 10.5.sp,
            lineHeight = 12.sp,
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
        targetValue = if (selected) 60.dp else 58.dp,
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
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            modifier = Modifier
                .offset(y = (-8).dp)
                .size(cameraSize),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            tonalElevation = 0.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
private fun NavigationBarBackground() {
    val color = MaterialTheme.colorScheme.surface
    Canvas(Modifier.fillMaxSize()) {
        val top = 16.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset.Zero.copy(y = top),
            size = Size(size.width, size.height - top),
            cornerRadius = CornerRadius(26.dp.toPx(), 26.dp.toPx()),
        )
        drawCircle(
            color = color,
            radius = 36.dp.toPx(),
            center = Offset(size.width / 2f, top),
        )
    }
}
