package com.eggplant.detector.app

import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eggplant.detector.app.navigation.EggplantNavigation
import com.eggplant.detector.core.ui.theme.EggplantDetectorTheme
import com.eggplant.detector.core.ui.motion.EggplantMotion
import com.eggplant.detector.core.ui.motion.LocalEggplantMotion
import androidx.compose.runtime.CompositionLocalProvider

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { EggplantDetectorApp() }
    }

    override fun onStart() {
        super.onStart()
        (application as EggplantApplication).repository.refreshCloud()
    }
}

@Composable
fun EggplantDetectorApp(
    appViewModel: EggplantAppViewModel = viewModel(
        factory = EggplantAppViewModel.factory(
            (androidx.compose.ui.platform.LocalContext.current.applicationContext as EggplantApplication).repository,
        ),
    ),
) {
    val application = (LocalContext.current.applicationContext as EggplantApplication)
    val startupReady by application.startupReady.collectAsStateWithLifecycle()
    val homeAlpha by animateFloatAsState(
        targetValue = if (startupReady) 1f else 0f,
        animationSpec = tween(420),
        label = "startupHomeAlpha",
    )
    val theme by appViewModel.themePreference.collectAsStateWithLifecycle()
    val language by appViewModel.languagePreference.collectAsStateWithLifecycle()
    val motion by appViewModel.motionPreference.collectAsStateWithLifecycle()
    LaunchedEffect(language) {
        val locales = LocaleListCompat.forLanguageTags(language.languageTag)
        if (AppCompatDelegate.getApplicationLocales() != locales) {
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (theme) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> systemDark
    }
    EggplantDetectorTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(LocalEggplantMotion provides EggplantMotion.forPreference(motion)) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = homeAlpha },
                ) {
                    EggplantNavigation(viewModel = appViewModel)
                }
                AnimatedVisibility(
                    visible = !startupReady,
                    enter = fadeIn(animationSpec = tween(180)),
                    exit = fadeOut(animationSpec = tween(420)),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    StartupLoadingScreen(Modifier.fillMaxSize())
                }
            }
        }
    }
}
