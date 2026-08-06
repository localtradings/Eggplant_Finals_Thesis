package com.eggplant.detector.feature.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.widget.ImageView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.Modifier
import androidx.test.platform.app.InstrumentationRegistry
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.eggplant.detector.core.ui.motion.EggplantMotion
import com.eggplant.detector.core.ui.motion.LocalEggplantMotion
import com.eggplant.detector.domain.model.MotionPreference
import com.eggplant.detector.R
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StillPhotoProcessingOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectedImageRemainsVisibleWhileScanningOverlayIsDisplayed() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val composition = LottieCompositionFactory
            .fromRawResSync(context, R.raw.untitled_file)
            .value
        assertNotNull("The still-image scanning Lottie must parse", composition)

        val lottieFrame = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val drawable = LottieDrawable().apply {
            this.composition = requireNotNull(composition)
            bounds = Rect(0, 0, lottieFrame.width, lottieFrame.height)
            callback = ImageView(context)
        }
        val visibleLottiePixels = (0..10).maxOf { step ->
            drawable.progress = step / 10f
            lottieFrame.eraseColor(Color.TRANSPARENT)
            drawable.draw(Canvas(lottieFrame))
            (0 until lottieFrame.width).sumOf { x ->
                (0 until lottieFrame.height).count { y -> Color.alpha(lottieFrame.getPixel(x, y)) > 0 }
            }
        }
        assertTrue("The still-image scanning Lottie must render visible pixels", visibleLottiePixels > 100)
        lottieFrame.recycle()

        val preview = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(220, 30, 60))
        }
        composeRule.setContent {
            CompositionLocalProvider(
                LocalEggplantMotion provides EggplantMotion.forPreference(MotionPreference.REDUCED),
            ) {
                StillPhotoProcessingOverlay(
                    previewBitmap = preview,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(R.string.analyzing)).assertIsDisplayed()

        val screenshot = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val sample = screenshot.getPixel(screenshot.width / 8, screenshot.height / 8)
        assertTrue(
            "The selected image must remain visible beneath the scan overlay",
            Color.red(sample) > 80 && Color.red(sample) > Color.green(sample) * 1.5f,
        )
    }
}
