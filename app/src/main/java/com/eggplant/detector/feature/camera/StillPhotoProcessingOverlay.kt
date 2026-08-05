package com.eggplant.detector.feature.camera

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.eggplant.detector.R
import com.eggplant.detector.core.ui.motion.LocalEggplantMotion

private const val PHOTO_PROCESSING_SPEED = 1.4285715f

@Composable
internal fun StillPhotoProcessingOverlay(
    previewBitmap: Bitmap?,
    modifier: Modifier = Modifier,
) {
    val motion = LocalEggplantMotion.current
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.untitled_file),
    )
    val imageBitmap = remember(previewBitmap) { previewBitmap?.asImageBitmap() }

    DisposableEffect(previewBitmap) {
        onDispose {
            if (previewBitmap != null && !previewBitmap.isRecycled) {
                previewBitmap.recycle()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        imageBitmap?.let { image ->
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f)),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (motion.spatialMovement) {
                val progress by animateLottieCompositionAsState(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    speed = PHOTO_PROCESSING_SPEED,
                )
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(260.dp),
                )
            } else {
                LottieAnimation(
                    composition = composition,
                    progress = { 0f },
                    modifier = Modifier.size(260.dp),
                )
            }
            Surface(
                color = Color.Black.copy(alpha = 0.58f),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.analyzing),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }
    }
}
