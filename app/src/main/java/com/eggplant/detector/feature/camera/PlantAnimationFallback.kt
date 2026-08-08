package com.eggplant.detector.feature.camera

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.eggplant.detector.core.ui.motion.LocalEggplantMotion

/**
 * A small renderer-safe visual cue that stays visible if a supplied Lottie
 * composition contains unsupported precomposition layers on a device.
 */
@Composable
internal fun PlantAnimationFallback(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    val motion = LocalEggplantMotion.current
    val transition = rememberInfiniteTransition(label = "plantAnimationFallback")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "plantAnimationFallbackPulse",
    )
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1800)),
        label = "plantAnimationFallbackRotation",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(136.dp)
                .graphicsLayer {
                    alpha = if (motion.spatialMovement) 0.74f else 0.52f
                    rotationZ = if (motion.spatialMovement) rotation else 0f
                },
            color = tint.copy(alpha = 0.42f),
            trackColor = tint.copy(alpha = 0.12f),
            strokeWidth = 5.dp,
        )
        Icon(
            imageVector = Icons.Outlined.Eco,
            contentDescription = null,
            tint = tint.copy(alpha = 0.86f),
            modifier = Modifier
                .size(62.dp)
                .graphicsLayer {
                    val scale = if (motion.spatialMovement) pulse else 1f
                    scaleX = scale
                    scaleY = scale
                },
        )
    }
}
