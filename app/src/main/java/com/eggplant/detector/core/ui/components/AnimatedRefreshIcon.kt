package com.eggplant.detector.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedRefreshIcon(
    isRefreshing: Boolean,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val rotation by rememberInfiniteTransition(label = "refresh-icon").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(850, easing = LinearEasing)),
        label = "refresh-rotation",
    )
    Icon(
        imageVector = Icons.Outlined.Refresh,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(22.dp).rotate(if (isRefreshing) rotation else 0f),
    )
}
