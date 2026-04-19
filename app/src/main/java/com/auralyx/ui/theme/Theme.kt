package com.auralyx.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    primary=PurpleAccent, onPrimary=Color.White, primaryContainer=Color(0xFF3B1F6B),
    secondary=PinkAccent, tertiary=CyanAccent,
    background=DeepBlack, surface=SurfaceDark, surfaceVariant=CardDark,
    onBackground=OnDark, onSurface=OnDark, onSurfaceVariant=OnDarkMuted,
    error=Color(0xFFCF6679)
)
private val Light = lightColorScheme(
    primary=LightAccent, onPrimary=Color.White, primaryContainer=Color(0xFFEDE9FE),
    secondary=PinkAccent, tertiary=CyanAccent,
    background=LightBg, surface=LightSurface, surfaceVariant=Color(0xFFF0F0FF),
    onBackground=OnLight, onSurface=OnLight, onSurfaceVariant=OnLightMuted
)

@Composable
fun AuralyxTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme=if(darkTheme) Dark else Light, typography=AuralyxTypography, content=content)
}
