package com.ciphervault.app.ui.theme

import androidx.compose.ui.graphics.Color

// Primary - vibrant cybersecurity emerald
val VaultGreen = Color(0xFF00E676)
val VaultGreenDark = Color(0xFF00A854)
val VaultGreenLight = Color(0xFF69F0AE)
val VaultGreenSubtle = Color(0x1F00E676)
val VaultGreenGlow = Color(0x3300E676)

// Deep cybersecurity dark surfaces
val DarkBackground = Color(0xFF0B0E14)
val DarkSurface = Color(0xFF101520)
val DarkSurfaceVariant = Color(0xFF151D2A)
val DarkCard = Color(0xFF1B2436)
val DarkCardElevated = Color(0xFF232E44)
val CardBorder = Color(0xFF28364F)
val CardBorderSubtle = Color(0x334B6082)

// Accents
val CyanAccent = Color(0xFF00E5FF)
val CyanSubtle = Color(0x1F00E5FF)
val BlueAccent = Color(0xFF3B82F6)

// Semantic
val ErrorRed = Color(0xFFFF5252)
val ErrorSubtle = Color(0x1FFF5252)
val WarningAmber = Color(0xFFF59E0B)
val WarningSubtle = Color(0x1FF59E0B)
val SuccessGreen = Color(0xFF00E676)

// High-contrast refined typography
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextTertiary = Color(0xFF64748B)
val TextDisabled = Color(0xFF475569)

object VaultColors {
    val Ground = Color(0xFF0E1013)
    val Surface = Color(0xFF171A1F)
    val SurfaceRaised = Color(0xFF262A31)
    val Hairline = Color.White.copy(alpha = 0.06f)

    val TextPrimary = Color(0xFFF1EFEA)
    val TextSecondary = Color(0xFFA3A7AF)

    val Accent = Color(0xFFE6D5B0)
    val OnAccent = Color(0xFF1A1710)
    val AccentTint = Accent.copy(alpha = 0.10f)

    // Nav pill glass
    val GlassFill = Color(0xFF22252C).copy(alpha = 0.55f)
    val GlassFallback = Color(0xFF22252C).copy(alpha = 0.94f) // API < 31, no blur
    val GlassBorder = Color.White.copy(alpha = 0.10f)
    
    // Semantic colors
    val ErrorRed = Color(0xFFF43F5E)
    val WarningAmber = Color(0xFFF59E0B)
    val SuccessGreen = Color(0xFF22C55E)
}
