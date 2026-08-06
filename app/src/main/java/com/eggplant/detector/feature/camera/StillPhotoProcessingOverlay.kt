package com.eggplant.detector.feature.camera

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.eggplant.detector.R
import com.eggplant.detector.core.ui.motion.LocalEggplantMotion

private const val PHOTO_PROCESSING_DURATION_MILLIS = 2_000L

@Composable
internal fun StillPhotoProcessingOverlay(
    previewBitmap: Bitmap?,
    modifier: Modifier = Modifier,
) {
    val motion = LocalEggplantMotion.current
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                factory = { context ->
                    StillPhotoScanningLottieView(
                        context = context,
                        motionEnabled = motion.spatialMovement,
                    )
                },
                update = { view -> view.setMotionEnabled(motion.spatialMovement) },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.82f },
            )
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp),
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

/**
 * The supplied scan animation contains embedded bitmap assets. A real Android View is
 * used as the drawable callback so Lottie can resolve those assets with a Context.
 */
private class StillPhotoScanningLottieView(
    context: Context,
    motionEnabled: Boolean,
) : View(context) {
    private var motionEnabledState = motionEnabled
    private val drawable = LottieDrawable()
    private val frameAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = PHOTO_PROCESSING_DURATION_MILLIS
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { animator ->
            drawable.progress = animator.animatedValue as Float
            invalidate()
        }
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        drawable.callback = this
        drawable.composition = LottieCompositionFactory
            .fromRawResSync(context, R.raw.untitled_file)
            .value
        drawable.progress = if (motionEnabled) 0f else 0.5f
    }

    fun setMotionEnabled(enabled: Boolean) {
        motionEnabledState = enabled
        if (enabled) {
            if (isAttachedToWindow && !frameAnimator.isStarted) frameAnimator.start()
        } else {
            frameAnimator.cancel()
            drawable.progress = 0.5f
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        drawable.callback = this
        setMotionEnabled(motionEnabledState)
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
