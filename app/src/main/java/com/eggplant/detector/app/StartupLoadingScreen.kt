package com.eggplant.detector.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.view.View
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
import androidx.compose.foundation.Image
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.annotation.RawRes
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.eggplant.detector.R
import com.eggplant.detector.core.ui.motion.LocalEggplantMotion
import com.eggplant.detector.feature.camera.PlantAnimationFallback
import kotlinx.coroutines.delay

internal const val STARTUP_ANIMATION_DURATION_MILLIS = 5_000L
internal const val STARTUP_BRAND_DURATION_MILLIS = 1_000L

private enum class StartupPhase {
    BRAND,
    PREPARING,
}

@Composable
internal fun StartupLoadingScreen(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {},
) {
    val motion = LocalEggplantMotion.current
    var phase by remember { mutableStateOf(StartupPhase.BRAND) }
    val loadingDescription = stringResource(R.string.startup_loading_content_description)

    LaunchedEffect(Unit) {
        delay(STARTUP_BRAND_DURATION_MILLIS)
        phase = StartupPhase.PREPARING
    }

    Box(
        modifier = modifier
            .background(Color.White)
            .semantics {
                contentDescription = loadingDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        when (phase) {
            StartupPhase.BRAND -> BrandLoadingScreen()
            StartupPhase.PREPARING -> PreparingPlantsLoadingScreen(
                motionEnabled = motion.spatialMovement,
                onFinished = onFinished,
            )
        }
    }
}

@Composable
private fun BrandLoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.planta_logo),
            contentDescription = stringResource(R.string.logo_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(112.dp),
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF17152B),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
        PlantAnimationFallback(
            modifier = Modifier
                .padding(top = 18.dp)
                .size(152.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun PreparingPlantsLoadingScreen(
    motionEnabled: Boolean,
    onFinished: () -> Unit,
) {
    LaunchedEffect(Unit) {
        delay(STARTUP_ANIMATION_DURATION_MILLIS)
        onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(248.dp),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                factory = { context ->
                    StartupPlantLottieView(
                        context = context,
                        animationResId = R.raw.startup_preparing_plants,
                        motionEnabled = motionEnabled,
                    )
                },
                modifier = Modifier.fillMaxSize(),
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

private class StartupPlantLottieView(
    context: Context,
    @RawRes animationResId: Int,
    private val motionEnabled: Boolean,
) : View(context) {
    private val drawable = LottieDrawable()
    private val frameAnimator = ValueAnimator.ofFloat(0f, 0.65f).apply {
        duration = 3_500L
        repeatMode = ValueAnimator.REVERSE
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { animator ->
            drawable.progress = animator.animatedValue as Float
            invalidate()
        }
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        drawable.callback = this
        drawable.composition = LottieCompositionFactory.fromRawResSync(context, animationResId).value
        drawable.progress = if (motionEnabled) 0f else 0.5f
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        drawable.callback = this
        if (motionEnabled) frameAnimator.start()
    }

    override fun onDetachedFromWindow() {
        frameAnimator.cancel()
        drawable.callback = null
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        drawable.setBounds(0, 0, width, height)
    }

    override fun onDraw(canvas: Canvas) {
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
    }
}
