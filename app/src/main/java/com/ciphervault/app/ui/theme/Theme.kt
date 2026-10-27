package com.ciphervault.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CipherVaultColorScheme = darkColorScheme(
    primary = VaultGreen,
    onPrimary = Color.Black,
    primaryContainer = VaultGreenDark,
    onPrimaryContainer = Color.White,
    secondary = CyanAccent,
    onSecondary = Color.Black,
    secondaryContainer = CyanAccent.copy(alpha = 0.2f),
    onSecondaryContainer = CyanAccent,
    tertiary = BlueAccent,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = Color.White,
    outline = TextDisabled
)

@Composable
fun CipherVaultTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = CipherVaultColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.toArgb()
            window.navigationBarColor = DarkSurface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CipherVaultTypography,
        content = content
    )
}
