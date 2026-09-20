package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = DeepBlack,
    primaryContainer = DeepCyan,
    onPrimaryContainer = Color.White,
    secondary = VividCyan,
    onSecondary = DeepBlack,
    background = DeepBlack,
    onBackground = Color(0xFFF1F5F9),
    surface = SurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = CardDark,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = BorderDark,
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = DeepCyan,
    secondary = VividCyan,
    onSecondary = Color.White,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = CardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun SaveTrickTheme(
    themeMode: String = "light", // "light", "dark", "system"
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode.lowercase()) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    SaveTrickTheme(
        themeMode = if (darkTheme) "dark" else "light",
        content = content
    )
}

