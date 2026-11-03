package com.ciphervault.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font
import com.ciphervault.app.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.my_font_certs
)

// The Noir pairing: Cormorant Garamond for headlines, Manrope for everything you read or tap.
val fontNameCormorant = GoogleFont("Cormorant Garamond")
val fontNameManrope = GoogleFont("Manrope")

val CormorantGaramond = FontFamily(
    Font(googleFont = fontNameCormorant, fontProvider = provider, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(googleFont = fontNameCormorant, fontProvider = provider, weight = FontWeight.Medium, style = FontStyle.Italic),
    Font(googleFont = fontNameCormorant, fontProvider = provider, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(googleFont = fontNameCormorant, fontProvider = provider, weight = FontWeight.SemiBold, style = FontStyle.Italic)
)

val Manrope = FontFamily(
    Font(googleFont = fontNameManrope, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = fontNameManrope, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = fontNameManrope, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = fontNameManrope, fontProvider = provider, weight = FontWeight.Bold)
)

// Old names, kept so any screen that still refers to them compiles (and picks up the new fonts).
val InstrumentSerif = CormorantGaramond
val Inter = Manrope

// Cormorant has a small x-height, so its sizes run a little larger than the serif it replaces.
object VaultType {
    val PageTitle = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.Medium, fontSize = 46.sp, lineHeight = 48.sp)
    val Headline = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.Medium, fontSize = 40.sp, lineHeight = 42.sp)
    val SectionTitle = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 32.sp)
    val Status = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.Medium, fontSize = 34.sp, lineHeight = 37.sp)
    val ItemTitle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp)
    val Meta = TextStyle(fontFamily = Manrope, fontSize = 13.sp, lineHeight = 18.sp)
    val Button = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    val Chip = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    val GroupLabel = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.2.sp) // render text uppercase()
    val Wordmark = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 3.sp) // render text uppercase()
}

// Every role is set, so default components (menus, dialogs, text fields, app bars) use the
// brand fonts too instead of the system font.
val CipherVaultTypography = Typography(
    displayLarge = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.Medium, fontSize = 56.sp, lineHeight = 58.sp),
    displayMedium = VaultType.PageTitle,
    displaySmall = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.Medium, fontSize = 36.sp, lineHeight = 40.sp),

    headlineLarge = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.Medium, fontSize = 34.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 34.sp),
    headlineSmall = VaultType.SectionTitle,

    titleLarge = TextStyle(fontFamily = CormorantGaramond, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 28.sp),
    titleMedium = VaultType.ItemTitle,
    titleSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),

    bodyLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = VaultType.Meta,
    bodySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),

    labelLarge = VaultType.Button,
    labelMedium = VaultType.Chip,
    labelSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

// Legacy objects kept around temporarily for non-migrated screens
val MonoStyles = object {
    val titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
}
val SemanticTextStyles = object {
    val inputLabel = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    val inputText = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    )
    val inputPlaceholder = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    )
    val statusError = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
}