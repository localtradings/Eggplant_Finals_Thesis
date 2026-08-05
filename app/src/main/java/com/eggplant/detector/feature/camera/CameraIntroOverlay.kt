package com.eggplant.detector.feature.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.RenderMode
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.eggplant.detector.R
import com.eggplant.detector.core.ui.motion.LocalEggplantMotion
import kotlinx.coroutines.delay

private const val CAMERA_INTRO_DURATION_MILLIS = 5_000L

@Composable
internal fun CameraIntroOverlay(
    visible: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    val motion = LocalEggplantMotion.current
    val pulse = rememberInfiniteTransition(label = "cameraIntroPulse").animateFloat(
        initialValue = 0.94f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "cameraIntroPulseValue",
    ).value
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.camera_plant_scanning),
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
    )

    LaunchedEffect(visible, composition) {
        delay(CAMERA_INTRO_DURATION_MILLIS)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 116.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(224.dp),
                contentAlignment = Alignment.Center,
            ) {
                PlantAnimationFallback(
                    modifier = Modifier.fillMaxSize(),
                    tint = Color.White,
                )
                if (motion.spatialMovement) {
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        renderMode = RenderMode.SOFTWARE,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = 0.70f
                                scaleX = pulse
                                scaleY = pulse
                            },
                    )
                }
            }
            Surface(
                color = Color.Black.copy(alpha = 0.58f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.widthIn(max = 300.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier
                            .size(30.dp)
                            .graphicsLayer {
                                alpha = if (motion.spatialMovement) 0.82f + (pulse - 0.94f) else 0.90f
                            },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = stringResource(R.string.camera_intro_message),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
