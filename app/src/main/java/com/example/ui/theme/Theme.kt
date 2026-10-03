package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val LocalAppAccentColor = staticCompositionLocalOf { ElectricBlue }
val LocalAppBgStyle = staticCompositionLocalOf { "default" }

@Composable
fun SaveTrickTheme(
    themeMode: String = "dark", // "light", "dark", "amoled", "system"
    accentKey: String = "electric_blue",
    bgStyle: String = "default", // "default", "mesh_gradient", "cyber_glow", "amoled_pitch"
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isAmoled = themeMode.lowercase() == "amoled"
    val isDark = when (themeMode.lowercase()) {
        "dark", "amoled" -> true
        "light" -> false
        else -> isSystemDark
    }

    val selectedAccent = AvailableAccents.find { it.key == accentKey } ?: AvailableAccents.first()
    val primaryColor = selectedAccent.primary
    val secondaryColor = selectedAccent.secondary

    val colorScheme = if (isDark) {
        if (isAmoled || bgStyle == "amoled_pitch") {
            darkColorScheme(
                primary = primaryColor,
                onPrimary = Color.Black,
                primaryContainer = selectedAccent.containerDark,
                onPrimaryContainer = Color.White,
                secondary = secondaryColor,
                onSecondary = Color.Black,
                background = AmoledBlack,
                onBackground = Color.White,
                surface = AmoledCard,
                onSurface = Color.White,
                surfaceVariant = Color(0xFF141414),
                onSurfaceVariant = Color(0xFFA0A0A0),
                outline = AmoledBorder,
                error = ErrorRed,
                onError = Color.White
            )
        } else {
            darkColorScheme(
                primary = primaryColor,
                onPrimary = DeepBlack,
                primaryContainer = selectedAccent.containerDark,
                onPrimaryContainer = Color.White,
                secondary = secondaryColor,
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
        }
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = selectedAccent.containerLight,
            onPrimaryContainer = primaryColor,
            secondary = secondaryColor,
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
    }

    CompositionLocalProvider(
        LocalAppAccentColor provides primaryColor,
        LocalAppBgStyle provides bgStyle
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun AppBackgroundContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bgStyle = LocalAppBgStyle.current
    val accent = LocalAppAccentColor.current
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val backgroundModifier = when (bgStyle) {
        "mesh_gradient" -> {
            if (isDark) {
                Modifier.background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.22f),
                            Color(0xFF0F172A),
                            Color(0xFF060B12)
                        ),
                        radius = 1600f
                    )
                )
            } else {
                Modifier.background(
                    Brush.linearGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.08f),
                            Color(0xFFF8FAFC),
                            Color(0xFFFFFFFF)
                        )
                    )
                )
            }
        }
        "cyber_glow" -> {
            Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.18f),
                        Color(0xFF070B14),
                        Color(0xFF020408)
                    )
                )
            )
        }
        "amoled_pitch" -> {
            Modifier.background(AmoledBlack)
        }
        else -> {
            Modifier.background(MaterialTheme.colorScheme.background)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(backgroundModifier)
    ) {
        content()
    }
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
