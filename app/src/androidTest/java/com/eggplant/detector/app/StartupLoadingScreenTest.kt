package com.eggplant.detector.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.airbnb.lottie.LottieCompositionFactory
import com.eggplant.detector.R
import org.junit.Assert.assertNotNull
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

        composeRule.setContent {
            StartupLoadingScreen(Modifier.fillMaxSize())
        }

        composeRule.onNodeWithText(
            context.getString(R.string.startup_preparing_plants),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.startup_loading_message),
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.startup_loading_content_description),
        ).assertIsDisplayed()

        composeRule.waitForIdle()
    }
}
