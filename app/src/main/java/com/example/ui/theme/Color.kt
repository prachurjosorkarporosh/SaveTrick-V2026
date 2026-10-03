package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Default Brand Colors
val ElectricBlue = Color(0xFF007BFF)
val ElectricCyan = Color(0xFF00E5FF)
val VividCyan = Color(0xFF00B4D8)
val DeepCyan = Color(0xFF0077B6)
val DeepBlack = Color(0xFF060B12)
val SurfaceDark = Color(0xFF0F172A)
val CardDark = Color(0xFF1E293B)
val BorderDark = Color(0xFF334155)

// AMOLED Pitch Black
val AmoledBlack = Color(0xFF000000)
val AmoledCard = Color(0xFF0A0A0A)
val AmoledBorder = Color(0xFF1E1E1E)

// Light Theme Colors
val SurfaceLight = Color(0xFFF8FAFC)
val CardLight = Color(0xFFFFFFFF)
val BorderLight = Color(0xFFE2E8F0)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF64748B)

// Accents
val SuccessGreen = Color(0xFF10B981)
val WarningOrange = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)

// Custom Accent Color Options
val TikTokPink = Color(0xFFFE2C55)
val TikTokCyan = Color(0xFF25F4EE)
val EmeraldGreen = Color(0xFF00E676)
val CyberPurple = Color(0xFFA855F7)
val GoldenAmber = Color(0xFFF59E0B)
val CrimsonRed = Color(0xFFE11D48)

data class AccentThemeConfig(
    val key: String,
    val nameEn: String,
    val nameBn: String,
    val primary: Color,
    val secondary: Color,
    val containerLight: Color,
    val containerDark: Color
)

val AvailableAccents = listOf(
    AccentThemeConfig(
        key = "electric_blue",
        nameEn = "Electric Blue",
        nameBn = "ইলেকট্রিক ব্লু",
        primary = ElectricBlue,
        secondary = ElectricCyan,
        containerLight = Color(0xFFE0F2FE),
        containerDark = Color(0xFF003A70)
    ),
    AccentThemeConfig(
        key = "tiktok_pink",
        nameEn = "TikTok Rose",
        nameBn = "টিকটক রোজ",
        primary = TikTokPink,
        secondary = TikTokCyan,
        containerLight = Color(0xFFFFE4E8),
        containerDark = Color(0xFF6B0B1D)
    ),
    AccentThemeConfig(
        key = "neon_cyan",
        nameEn = "Neon Cyan",
        nameBn = "নিয়ন সায়ান",
        primary = ElectricCyan,
        secondary = ElectricBlue,
        containerLight = Color(0xFFE0F7FA),
        containerDark = Color(0xFF004D5A)
    ),
    AccentThemeConfig(
        key = "emerald_green",
        nameEn = "Emerald Mint",
        nameBn = "এমারেল্ড মিন্ট",
        primary = EmeraldGreen,
        secondary = Color(0xFF00B0FF),
        containerLight = Color(0xFFE8F8F0),
        containerDark = Color(0xFF004D26)
    ),
    AccentThemeConfig(
        key = "cyber_purple",
        nameEn = "Cyber Purple",
        nameBn = "সাইবার পার্পল",
        primary = CyberPurple,
        secondary = Color(0xFFEC4899),
        containerLight = Color(0xFFF3E8FF),
        containerDark = Color(0xFF4C1D95)
    ),
    AccentThemeConfig(
        key = "golden_amber",
        nameEn = "Golden Amber",
        nameBn = "গোল্ডেন অ্যাম্বার",
        primary = GoldenAmber,
        secondary = Color(0xFFEF4444),
        containerLight = Color(0xFFFEF3C7),
        containerDark = Color(0xFF78350F)
    ),
    AccentThemeConfig(
        key = "crimson_red",
        nameEn = "Crimson Flame",
        nameBn = "ক্রিমসন ফ্লেম",
        primary = CrimsonRed,
        secondary = Color(0xFFF97316),
        containerLight = Color(0xFFFFE4E6),
        containerDark = Color(0xFF881337)
    )
)
