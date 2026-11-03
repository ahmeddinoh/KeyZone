package com.ciphervault.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val VaultShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp), // chips, small badges
    small = RoundedCornerShape(14.dp),      // icon tiles, text fields
    medium = RoundedCornerShape(18.dp),     // cards, grouped lists
    large = RoundedCornerShape(28.dp),      // dialogs, drop zones, pill buttons
    extraLarge = RoundedCornerShape(32.dp), // sheets, the nav pill
)

// Every Material role is set explicitly, so default components (menus, dialogs, sheets, switches)
// land on the Noir palette instead of falling back to Material's purple-tinted defaults.
private val VaultColorScheme = darkColorScheme(
    primary = VaultColors.Accent,
    onPrimary = VaultColors.OnAccent,
    primaryContainer = VaultColors.AccentContainer,
    onPrimaryContainer = VaultColors.Accent,
    inversePrimary = VaultColors.AccentContainer,

    secondary = VaultColors.Accent,
    onSecondary = VaultColors.OnAccent,
    secondaryContainer = VaultColors.SurfaceRaised,
    onSecondaryContainer = VaultColors.TextPrimary,

    tertiary = VaultColors.Accent,
    onTertiary = VaultColors.OnAccent,
    tertiaryContainer = VaultColors.AccentContainer,
    onTertiaryContainer = VaultColors.Accent,

    background = VaultColors.Ground,
    onBackground = VaultColors.TextPrimary,

    surface = VaultColors.Surface,
    onSurface = VaultColors.TextPrimary,
    surfaceVariant = VaultColors.SurfaceRaised,
    onSurfaceVariant = VaultColors.TextSecondary,
    surfaceTint = Color.Transparent, // no tonal overlay: elevation shows through colour, not tint

    surfaceContainerLowest = VaultColors.Ground,
    surfaceContainerLow = VaultColors.Surface,
    surfaceContainer = VaultColors.Surface,
    surfaceContainerHigh = VaultColors.SurfaceRaised,
    surfaceContainerHighest = VaultColors.SurfaceHighest,

    inverseSurface = VaultColors.TextPrimary,
    inverseOnSurface = VaultColors.Ground,

    outline = VaultColors.Hairline,
    outlineVariant = VaultColors.Hairline,

    error = VaultColors.ErrorRed,
    onError = Color.White,
    errorContainer = VaultColors.ErrorRed.copy(alpha = 0.16f),
    onErrorContainer = VaultColors.ErrorRed,

    scrim = Color.Black
)

@Composable
fun CipherVaultTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            window.statusBarColor = VaultColors.Ground.toArgb()
            window.navigationBarColor = VaultColors.Ground.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = VaultColorScheme,
        typography = CipherVaultTypography,
        shapes = VaultShapes,
        content = content,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}