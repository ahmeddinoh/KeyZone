package com.ciphervault.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Key Zone "Noir" palette: a near-black ground, brass gold as the single accent, and warm cream
 * text. It matches the welcome screen, so the whole app reads as one place.
 */
object VaultColors {
    // Grounds, from the page itself up to things that sit on top of it
    val Ground = Color(0xFF0E0F14)
    val Surface = Color(0xFF161820)
    val SurfaceRaised = Color(0xFF20222B)
    val SurfaceHighest = Color(0xFF2A2D37)

    // A faint warm line, the same cream as the text so it never looks grey
    val Hairline = Color(0xFFF3EDE2).copy(alpha = 0.08f)

    val TextPrimary = Color(0xFFF3EDE2)
    val TextSecondary = Color(0xFFB3AFA5)

    // Brass gold
    val Accent = Color(0xFFD4B26A)
    val OnAccent = Color(0xFF17130A)
    val AccentTint = Accent.copy(alpha = 0.12f)
    val AccentContainer = Color(0xFF2B2516) // a dark, gold-tinted fill for selected states

    // Nav pill glass
    val GlassFill = Color(0xFF1A1C24).copy(alpha = 0.55f)
    val GlassFallback = Color(0xFF1A1C24).copy(alpha = 0.94f) // API < 31, no blur
    val GlassBorder = Accent.copy(alpha = 0.18f) // a thin gold edge, like the portal's arches

    // Semantic colors. Warning is orange rather than amber so it never reads as the gold accent.
    val ErrorRed = Color(0xFFF43F5E)
    val WarningAmber = Color(0xFFE8833A)
    val SuccessGreen = Color(0xFF43B77C)
}