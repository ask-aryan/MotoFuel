package com.example.fueltracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.fueltracker.R

val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val SairaFamily: FontFamily = FontFamily(
    Font(GoogleFont("Saira"), GoogleFontProvider, FontWeight.Normal),
    Font(GoogleFont("Saira"), GoogleFontProvider, FontWeight.Medium),
    Font(GoogleFont("Saira"), GoogleFontProvider, FontWeight.SemiBold),
    Font(GoogleFont("Saira"), GoogleFontProvider, FontWeight.Bold),
    Font(GoogleFont("Saira"), GoogleFontProvider, FontWeight.ExtraBold),
)

val SairaCondensedFamily: FontFamily = FontFamily(
    Font(GoogleFont("Saira Condensed"), GoogleFontProvider, FontWeight.Bold),
    Font(GoogleFont("Saira Condensed"), GoogleFontProvider, FontWeight.ExtraBold),
)

val PlusJakartaSansFamily: FontFamily = FontFamily(
    Font(GoogleFont("Plus Jakarta Sans"), GoogleFontProvider, FontWeight.Normal),
    Font(GoogleFont("Plus Jakarta Sans"), GoogleFontProvider, FontWeight.Medium),
    Font(GoogleFont("Plus Jakarta Sans"), GoogleFontProvider, FontWeight.SemiBold),
    Font(GoogleFont("Plus Jakarta Sans"), GoogleFontProvider, FontWeight.Bold),
    Font(GoogleFont("Plus Jakarta Sans"), GoogleFontProvider, FontWeight.ExtraBold),
)

fun makeTypography(family: FontFamily) = Typography(
    displayLarge  = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 45.sp),
    displaySmall  = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp),
    headlineLarge  = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp),
    headlineSmall  = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp),
    titleLarge  = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall  = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge   = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium  = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall   = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge  = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall  = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

// Kept for any composable that still references the old `Typography` singleton
val Typography = makeTypography(SairaFamily)
