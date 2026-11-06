package com.ciphervault.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font as GFont
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
    GFont(googleFont = fontNameCormorant, fontProvider = provider, weight = FontWeight.Medium, style = FontStyle.Normal),
    GFont(googleFont = fontNameCormorant, fontProvider = provider, weight = FontWeight.Medium, style = FontStyle.Italic),
    GFont(googleFont = fontNameCormorant, fontProvider = provider, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    GFont(googleFont = fontNameCormorant, fontProvider = provider, weight = FontWeight.SemiBold, style = FontStyle.Italic)
)

val ThmanyahSerif = FontFamily(
    Font(resId = R.font.thmanyah_serif_display_medium, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(resId = R.font.thmanyah_serif_display_medium, weight = FontWeight.Medium, style = FontStyle.Italic),
    Font(resId = R.font.thmanyah_serif_display_bold, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(resId = R.font.thmanyah_serif_display_bold, weight = FontWeight.SemiBold, style = FontStyle.Italic)
)

val Manrope = FontFamily(
    GFont(googleFont = fontNameManrope, fontProvider = provider, weight = FontWeight.Normal, style = FontStyle.Normal),
    GFont(googleFont = fontNameManrope, fontProvider = provider, weight = FontWeight.Normal, style = FontStyle.Italic),
    GFont(googleFont = fontNameManrope, fontProvider = provider, weight = FontWeight.Medium, style = FontStyle.Normal),
    GFont(googleFont = fontNameManrope, fontProvider = provider, weight = FontWeight.Medium, style = FontStyle.Italic),
    GFont(googleFont = fontNameManrope, fontProvider = provider, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    GFont(googleFont = fontNameManrope, fontProvider = provider, weight = FontWeight.SemiBold, style = FontStyle.Italic),
    GFont(googleFont = fontNameManrope, fontProvider = provider, weight = FontWeight.Bold, style = FontStyle.Normal),
    GFont(googleFont = fontNameManrope, fontProvider = provider, weight = FontWeight.Bold, style = FontStyle.Italic)
)

val ThmanyahSans = FontFamily(
    Font(resId = R.font.thmanyah_sans_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(resId = R.font.thmanyah_sans_regular, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(resId = R.font.thmanyah_sans_medium, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(resId = R.font.thmanyah_sans_medium, weight = FontWeight.Medium, style = FontStyle.Italic),
    Font(resId = R.font.thmanyah_sans_bold, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(resId = R.font.thmanyah_sans_bold, weight = FontWeight.SemiBold, style = FontStyle.Italic),
    Font(resId = R.font.thmanyah_sans_bold, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(resId = R.font.thmanyah_sans_bold, weight = FontWeight.Bold, style = FontStyle.Italic)
)

private val appSerifFont: FontFamily
    get() = if (java.util.Locale.getDefault().language == "ar") ThmanyahSerif else CormorantGaramond

private val appSansFont: FontFamily
    get() = if (java.util.Locale.getDefault().language == "ar") ThmanyahSans else Manrope

// Old names, kept so any screen that still refers to them compiles (and picks up the new fonts).
val InstrumentSerif = CormorantGaramond
val Inter = Manrope

// Cormorant has a small x-height, so its sizes run a little larger than the serif it replaces.
object VaultType {
    val PageTitle get() = TextStyle(fontFamily = appSerifFont, fontWeight = FontWeight.Medium, fontSize = 46.sp, lineHeight = 48.sp)
    val Headline get() = TextStyle(fontFamily = appSerifFont, fontWeight = FontWeight.Medium, fontSize = 40.sp, lineHeight = 42.sp)
    val SectionTitle get() = TextStyle(fontFamily = appSerifFont, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 32.sp)
    val Status get() = TextStyle(fontFamily = appSerifFont, fontWeight = FontWeight.Medium, fontSize = 34.sp, lineHeight = 37.sp)
    val ItemTitle get() = TextStyle(fontFamily = appSansFont, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp)
    val Meta get() = TextStyle(fontFamily = appSansFont, fontSize = 13.sp, lineHeight = 18.sp)
    val Button get() = TextStyle(fontFamily = appSansFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    val Chip get() = TextStyle(fontFamily = appSansFont, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    val GroupLabel get() = TextStyle(fontFamily = appSansFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.2.sp) // render text uppercase()
    val Wordmark get() = TextStyle(fontFamily = appSansFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 3.sp) // render text uppercase()
}

// Every role is set, so default components (menus, dialogs, text fields, app bars) use the
// brand fonts too instead of the system font.
val CipherVaultTypography: Typography
    get() = Typography(
        displayLarge = TextStyle(fontFamily = appSerifFont, fontWeight = FontWeight.Medium, fontSize = 56.sp, lineHeight = 58.sp),
        displayMedium = VaultType.PageTitle,
        displaySmall = TextStyle(fontFamily = appSerifFont, fontWeight = FontWeight.Medium, fontSize = 36.sp, lineHeight = 40.sp),

        headlineLarge = TextStyle(fontFamily = appSerifFont, fontWeight = FontWeight.Medium, fontSize = 34.sp, lineHeight = 38.sp),
        headlineMedium = TextStyle(fontFamily = appSerifFont, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 34.sp),
        headlineSmall = VaultType.SectionTitle,

        titleLarge = TextStyle(fontFamily = appSerifFont, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 28.sp),
        titleMedium = VaultType.ItemTitle,
        titleSmall = TextStyle(fontFamily = appSansFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),

        bodyLarge = TextStyle(fontFamily = appSansFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = VaultType.Meta,
        bodySmall = TextStyle(fontFamily = appSansFont, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),

        labelLarge = VaultType.Button,
        labelMedium = VaultType.Chip,
        labelSmall = TextStyle(fontFamily = appSansFont, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
    )

// Legacy objects kept around temporarily for non-migrated screens
val MonoStyles = object {
    val titleMedium get() = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
}
val SemanticTextStyles = object {
    val inputLabel get() = TextStyle(
        fontFamily = appSansFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    val inputText get() = TextStyle(
        fontFamily = appSansFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    )
    val inputPlaceholder get() = TextStyle(
        fontFamily = appSansFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    )
    val statusError get() = TextStyle(
        fontFamily = appSansFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
}