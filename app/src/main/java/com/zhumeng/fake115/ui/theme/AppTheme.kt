package com.zhumeng.fake115.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val appBackground: Color,
    val topBar: Color,
    val elevatedSurface: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceOverlay: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentText: Color,
    val danger: Color,
    val dangerSoft: Color,
    val borderSubtle: Color,
    val placeholderSurface: Color,
)

private val DarkAppColors = AppColors(
    appBackground = Color(0xFF0B1220),
    topBar = Color(0xFF111827),
    elevatedSurface = Color(0xD9121A2B),
    surface = Color(0xCC101826),
    surfaceVariant = Color(0xFF182133),
    surfaceOverlay = Color(0xB20B1220),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFE5ECF6),
    textTertiary = Color(0xFF98A6BE),
    accent = Color(0xFF4F46E5),
    accentSoft = Color(0xFF314A7A),
    accentText = Color(0xFFB9DBFF),
    danger = Color(0xFFB83A53),
    dangerSoft = Color(0xFF3B1E25),
    borderSubtle = Color(0x66111827),
    placeholderSurface = Color(0xFF24304A),
)

private val LightAppColors = AppColors(
    appBackground = Color(0xFFF4F7FB),
    topBar = Color(0xFFFFFFFF),
    elevatedSurface = Color(0xFFFDFEFF),
    surface = Color(0xFFF7FAFE),
    surfaceVariant = Color(0xFFE9EEF7),
    surfaceOverlay = Color(0x99E3EAF5),
    textPrimary = Color(0xFF162033),
    textSecondary = Color(0xFF2F425E),
    textTertiary = Color(0xFF667A98),
    accent = Color(0xFF4458D8),
    accentSoft = Color(0xFFDCE7FF),
    accentText = Color(0xFF294384),
    danger = Color(0xFFC44761),
    dangerSoft = Color(0xFFFFE1E7),
    borderSubtle = Color(0xFFE2E8F1),
    placeholderSurface = Color(0xFFD8E1F0),
)

internal val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

internal fun appColors(isDark: Boolean): AppColors {
    return if (isDark) DarkAppColors else LightAppColors
}

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}
