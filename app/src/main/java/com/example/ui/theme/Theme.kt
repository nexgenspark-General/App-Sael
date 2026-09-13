package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DonsaelCyan,
    onPrimary = Color(0xFF001F3F),
    primaryContainer = DonsaelBlue,
    onPrimaryContainer = Color.White,
    secondary = DonsaelTeal,
    onSecondary = Color(0xFF001F3F),
    tertiary = DonsaelAmber,
    onTertiary = Color.Black,
    background = DarkBg,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkBorder,
    error = DonsaelRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = DonsaelBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FD),
    onPrimaryContainer = DonsaelNavy,
    secondary = DonsaelTeal,
    onSecondary = Color.White,
    tertiary = DonsaelAmber,
    onTertiary = Color.Black,
    background = LightBg,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightBorder,
    error = DonsaelRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun DonsaelTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MyApplicationTheme(darkTheme = darkTheme, content = content)
}

