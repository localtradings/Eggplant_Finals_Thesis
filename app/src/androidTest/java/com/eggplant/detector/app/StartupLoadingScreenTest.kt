package com.eggplant.detector.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.eggplant.detector.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StartupLoadingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingScreenShowsCopyProgressAndPackagedLottieCompositions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        listOf(
            R.raw.startup_preparing_plants,
            R.raw.camera_plant_scanning,
            R.raw.untitled_file,
        ).forEach { resourceId ->
            val composition = LottieCompositionFactory.fromRawResSync(context, resourceId).value
            assertNotNull("The packaged Lottie composition must parse: $resourceId", composition)
        }
        val startupComposition = LottieCompositionFactory
            .fromRawResSync(context, R.raw.startup_preparing_plants)
            .value
        requireNotNull(startupComposition)
        val renderedFrame = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val drawable = LottieDrawable().apply {
            composition = startupComposition
            bounds = Rect(0, 0, renderedFrame.width, renderedFrame.height)
        }
        drawable.progress = 0.5f
        renderedFrame.eraseColor(Color.TRANSPARENT)
        drawable.draw(Canvas(renderedFrame))
        val visiblePixels = renderedFrame.run {
            (0 until width).sumOf { x ->
                (0 until height).count { y -> Color.alpha(getPixel(x, y)) > 0 }
            }
        }
        assertTrue("The startup Lottie must render visible pixels", visiblePixels > 100)

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { StartupLoadingScreen(Modifier.fillMaxSize()) }

        composeRule.onNodeWithText(context.getString(R.string.app_name)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.logo_description))
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.startup_loading_content_description),
        ).assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(STARTUP_BRAND_DURATION_MILLIS)
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(R.string.startup_preparing_plants)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.startup_loading_message)).assertIsDisplayed()
    }

    @Test
    fun loadingScreenFinishesAfterTheConfiguredAnimationDuration() {
        var finished = false
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            StartupLoadingScreen(onFinished = { finished = true })
        }

        composeRule.waitForIdle()
        assertFalse(finished)
        composeRule.mainClock.advanceTimeBy(STARTUP_BRAND_DURATION_MILLIS - 1L)
        composeRule.waitForIdle()
        assertFalse(finished)
        composeRule.mainClock.advanceTimeBy(1L)
        composeRule.waitForIdle()
        assertFalse(finished)
        composeRule.mainClock.advanceTimeBy(STARTUP_ANIMATION_DURATION_MILLIS)
        composeRule.waitForIdle()
        assertTrue(finished)
    }
}
