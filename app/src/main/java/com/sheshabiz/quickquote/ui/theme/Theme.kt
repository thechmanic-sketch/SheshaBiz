package com.sheshabiz.quickquote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = OnPrimary,
    secondary = BrandPrimaryDark,
    onSecondary = OnPrimary,
    background = BgLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextMuted,
    error = ErrorRed,
    onError = OnPrimary,
    outline = DividerLight
)

private val DarkColors = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = OnPrimary,
    secondary = BrandPrimaryDark,
    onSecondary = OnPrimary,
    background = BgDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextMuted,
    error = ErrorRed,
    onError = OnPrimary,
    outline = DividerDark
)

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun QuickQuoteTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QuickQuoteTypography,
        content = content
    )
}
