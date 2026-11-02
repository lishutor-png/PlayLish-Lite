package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SportOrange,
    onPrimary = Color.Black,
    primaryContainer = SportOrangeDim,
    onPrimaryContainer = SportOrangeLight,
    secondary = SportCyan,
    onSecondary = Color.Black,
    secondaryContainer = SportCyanDim,
    onSecondaryContainer = SportCyan,
    tertiary = SportLime,
    onTertiary = Color.Black,
    background = SportCarbonBg,
    onBackground = TextWhite,
    surface = SportCardSurface,
    onSurface = TextWhite,
    surfaceVariant = SportCardSurfaceVariant,
    onSurfaceVariant = TextMuted,
    outline = SportCardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // PlayLish Lite uses an intentional, sporty dark AIMP aesthetic
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
