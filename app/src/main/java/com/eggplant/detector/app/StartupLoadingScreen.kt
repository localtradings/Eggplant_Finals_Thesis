package com.eggplant.detector.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.eggplant.detector.R
import com.eggplant.detector.core.ui.motion.LocalEggplantMotion

@Composable
internal fun StartupLoadingScreen(modifier: Modifier = Modifier) {
    val motion = LocalEggplantMotion.current
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.startup_preparing_plants),
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
    val loadingDescription = stringResource(R.string.startup_loading_content_description)

    Box(
        modifier = modifier
            .background(Color.White)
            .semantics {
                contentDescription = loadingDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (motion.spatialMovement) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(272.dp),
                )
            } else {
                LottieAnimation(
                    composition = composition,
                    progress = { 0f },
                    modifier = Modifier.size(272.dp),
                )
            }
            Text(
                text = stringResource(R.string.startup_preparing_plants),
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF17152B),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.startup_loading_message),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF68687C),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            LinearProgressIndicator(
                modifier = Modifier
                    .padding(top = 22.dp)
                    .width(176.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color(0xFFF1ECF8),
            )
        }
    }
}
