package com.eggplant.detector.feature.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.camera_plant_scanning),
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
    )

    LaunchedEffect(visible) {
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
            if (motion.spatialMovement) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(188.dp),
                )
            }
            Surface(
                color = Color.Black.copy(alpha = 0.58f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.widthIn(max = 300.dp),
            ) {
                Text(
                    text = stringResource(R.string.camera_intro_message),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
