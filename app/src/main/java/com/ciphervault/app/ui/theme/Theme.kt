package com.ciphervault.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val VaultShapes = Shapes(
    small = RoundedCornerShape(14.dp),  // icon tiles
    medium = RoundedCornerShape(22.dp), // cards
    large = RoundedCornerShape(28.dp),  // drop zone
)

private val VaultColorScheme = darkColorScheme(
    primary = VaultColors.Accent,
    onPrimary = VaultColors.OnAccent,
    background = VaultColors.Ground,
    onBackground = VaultColors.TextPrimary,
    surface = VaultColors.Surface,
    onSurface = VaultColors.TextPrimary,
    surfaceVariant = VaultColors.SurfaceRaised,
    onSurfaceVariant = VaultColors.TextSecondary,
    outline = VaultColors.Hairline,
    // Add these for legacy compatibility
    error = VaultColors.ErrorRed,
    secondary = VaultColors.Accent,
    tertiary = VaultColors.Accent
)

@Composable
fun CipherVaultTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = VaultColors.Ground.toArgb()
            window.navigationBarColor = VaultColors.Ground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = VaultColorScheme,
        typography = CipherVaultTypography,
        shapes = VaultShapes,
        content = content,
    )
}
