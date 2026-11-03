package com.ciphervault.app.ui.theme

import androidx.compose.ui.graphics.Color

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
    
    // Semantic colors (reusing old colors for error/warning/success contexts where needed)
    val ErrorRed = Color(0xFFF43F5E)
    val WarningAmber = Color(0xFFF59E0B)
    val SuccessGreen = Color(0xFF22C55E)
}
