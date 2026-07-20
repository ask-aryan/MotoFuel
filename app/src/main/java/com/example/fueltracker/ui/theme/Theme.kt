package com.example.fueltracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalAppTheme = compositionLocalOf { AppTheme.PIT_LANE }

enum class AppTheme(val displayName: String, val description: String) {
    PIT_LANE("Pit Lane", "Sporty · carbon black & electric lime"),
    DAYLIGHT("Daylight", "Friendly · warm cream & deep violet")
}

private val PitLaneColorScheme = darkColorScheme(
    primary              = PL_Primary,
    onPrimary            = PL_OnPrimary,
    primaryContainer     = Color(0xFF1A2406),
    onPrimaryContainer   = PL_Primary,
    secondary            = PL_PrimaryDim,
    onSecondary          = PL_OnPrimary,
    secondaryContainer   = Color(0xFF1A2406),
    onSecondaryContainer = PL_Primary,
    tertiary             = Color(0xFF1FAE93),
    onTertiary           = PL_OnPrimary,
    tertiaryContainer    = Color(0xFF0F3D33),
    onTertiaryContainer  = Color(0xFF4DD4B4),
    background           = PL_Background,
    onBackground         = PL_TextPrimary,
    surface              = PL_Surface,
    onSurface            = PL_TextPrimary,
    surfaceVariant       = PL_SurfaceAlt,
    onSurfaceVariant     = PL_TextSecondary,
    outline              = PL_TextMuted,
    outlineVariant       = PL_Border,
    error                = Color(0xFFFF5449),
    onError              = PL_OnPrimary,
    errorContainer       = Color(0xFF3B0D0C),
    onErrorContainer     = Color(0xFFFF897D),
)

private val DaylightLightColorScheme = lightColorScheme(
    primary              = DL_Primary,
    onPrimary            = DL_OnPrimary,
    primaryContainer     = DL_PrimaryContainer,
    onPrimaryContainer   = DL_OnPrimaryContainer,
    secondary            = Color(0xFF7A7287),
    onSecondary          = DL_OnPrimary,
    secondaryContainer   = DL_PrimaryContainer,
    onSecondaryContainer = DL_Primary,
    tertiary             = Color(0xFF1FAE93),
    onTertiary           = DL_OnPrimary,
    tertiaryContainer    = Color(0xFFE7F7F1),
    onTertiaryContainer  = Color(0xFF0F7A64),
    background           = DL_Background_L,
    onBackground         = DL_TextPrimary_L,
    surface              = DL_Surface_L,
    onSurface            = DL_TextPrimary_L,
    surfaceVariant       = DL_SurfaceAlt_L,
    onSurfaceVariant     = DL_TextSecondary_L,
    outline              = DL_TextSecondary_L,
    outlineVariant       = DL_Border_L,
    error                = Color(0xFFB3261E),
    onError              = DL_OnPrimary,
    errorContainer       = Color(0xFFF9DEDC),
    onErrorContainer     = Color(0xFF410E0B),
)

private val DaylightDarkColorScheme = darkColorScheme(
    primary              = DL_Primary_D,
    onPrimary            = DL_Background_D,
    primaryContainer     = Color(0xFF322846),
    onPrimaryContainer   = DL_Primary_D,
    secondary            = Color(0xFF9A8FB0),
    onSecondary          = DL_Background_D,
    secondaryContainer   = Color(0xFF2A2238),
    onSecondaryContainer = DL_Primary_D,
    tertiary             = Color(0xFF4DD4B4),
    onTertiary           = DL_Background_D,
    tertiaryContainer    = Color(0xFF0F3D33),
    onTertiaryContainer  = Color(0xFF4DD4B4),
    background           = DL_Background_D,
    onBackground         = DL_TextPrimary_D,
    surface              = DL_Surface_D,
    onSurface            = DL_TextPrimary_D,
    surfaceVariant       = DL_SurfaceAlt_D,
    onSurfaceVariant     = DL_TextSecondary_D,
    outline              = DL_TextSecondary_D,
    outlineVariant       = DL_Border_D,
    error                = Color(0xFFFF5449),
    onError              = DL_Background_D,
    errorContainer       = Color(0xFF3B0D0C),
    onErrorContainer     = Color(0xFFFF897D),
)

@Composable
fun FuelTrackerTheme(
    appTheme: AppTheme = AppTheme.PIT_LANE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.PIT_LANE -> PitLaneColorScheme
        AppTheme.DAYLIGHT -> if (darkTheme) DaylightDarkColorScheme else DaylightLightColorScheme
    }

    val typography = when (appTheme) {
        AppTheme.PIT_LANE -> makeTypography(SairaFamily)
        AppTheme.DAYLIGHT -> makeTypography(PlusJakartaSansFamily)
    }

    CompositionLocalProvider(LocalAppTheme provides appTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
