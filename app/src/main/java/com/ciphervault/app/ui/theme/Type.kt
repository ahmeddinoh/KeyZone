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

val fontNameInstrumentSerif = GoogleFont("Instrument Serif")
val fontNameInter = GoogleFont("Inter")

val InstrumentSerif = FontFamily(
    Font(googleFont = fontNameInstrumentSerif, fontProvider = provider, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(googleFont = fontNameInstrumentSerif, fontProvider = provider, weight = FontWeight.Normal, style = FontStyle.Italic)
)

val Inter = FontFamily(
    Font(googleFont = fontNameInter, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = fontNameInter, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = fontNameInter, fontProvider = provider, weight = FontWeight.SemiBold)
)

object VaultType {
    val PageTitle = TextStyle(fontFamily = InstrumentSerif, fontSize = 42.sp, lineHeight = 44.sp, letterSpacing = (-0.5).sp)
    val SectionTitle = TextStyle(fontFamily = InstrumentSerif, fontSize = 24.sp, lineHeight = 28.sp)
    val Status = TextStyle(fontFamily = InstrumentSerif, fontSize = 30.sp, lineHeight = 33.sp)
    val ItemTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp)
    val Meta = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 18.sp)
    val Button = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    val Chip = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    val GroupLabel = TextStyle(fontFamily = Inter, fontSize = 12.sp, letterSpacing = 1.2.sp) // render text uppercase()
}

val CipherVaultTypography = Typography(
    displayMedium = VaultType.PageTitle,
    headlineSmall = VaultType.SectionTitle,
    titleMedium = VaultType.ItemTitle,
    bodyMedium = VaultType.Meta,
    labelLarge = VaultType.Button,
    labelMedium = VaultType.Chip,
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
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    val inputText = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    )
    val inputPlaceholder = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    )
    val statusError = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
}
