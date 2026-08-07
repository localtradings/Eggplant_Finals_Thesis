package com.eggplant.detector.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = LeafGreen,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    background = AppBackground,
    onBackground = Ink,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = Ink,
    surfaceVariant = PrimaryGreenSoft,
    onSurfaceVariant = MutedInk,
    outline = CardBorder,
)

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF8BCB8F),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF12351A),
    secondary = androidx.compose.ui.graphics.Color(0xFF91D49D),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF12351A),
    background = DarkBackground,
    onBackground = androidx.compose.ui.graphics.Color(0xFFF0F7F1),
    surface = DarkSurface,
    onSurface = androidx.compose.ui.graphics.Color(0xFFF0F7F1),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF253A29),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFC8D8CA),
)

@Composable
fun EggplantDetectorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
