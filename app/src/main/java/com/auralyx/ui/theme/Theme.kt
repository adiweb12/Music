package com.auralyx.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary          = PurpleAccent,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFF3B1F6B),
    secondary        = PinkAccent,
    tertiary         = CyanAccent,
    background       = DeepBlack,
    surface          = SurfaceDark,
    surfaceVariant   = SurfaceVariant,
    onBackground     = OnSurfaceDark,
    onSurface        = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceMuted,
    error            = Color(0xFFCF6679)
)

private val LightColorScheme = lightColorScheme(
    primary          = LightAccent,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    secondary        = PinkAccent,
    tertiary         = CyanAccent,
    background       = LightBackground,
    surface          = LightSurface,
    surfaceVariant   = LightCard,
    onBackground     = OnSurfaceLight,
    onSurface        = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceMutedL
)

@Composable
fun AuralyxTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography  = AuralyxTypography,
        content     = content
    )
}
