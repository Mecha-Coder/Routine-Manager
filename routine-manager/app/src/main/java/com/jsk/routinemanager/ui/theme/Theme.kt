package com.jsk.routinemanager.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalRmColors = staticCompositionLocalOf { LightColors }

object RmTheme {
    val colors: RmColors
        @Composable @ReadOnlyComposable get() = LocalRmColors.current
}

@Composable
fun RoutineManagerTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    val scheme = remember(colors) { colors.toMaterialScheme() }
    CompositionLocalProvider(LocalRmColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = MaterialTypography) {
            CompositionLocalProvider(
                LocalContentColor provides colors.textPrimary,
                LocalTextStyle provides RmType.body,
                content = content,
            )
        }
    }
}

/**
 * Every Material slot gets a token, so stock components never bring Material's own colours
 * (ARCHITECTURE §9). All slots are given, so it doesn't matter that this is lightColorScheme.
 */
private fun RmColors.toMaterialScheme(): ColorScheme = lightColorScheme(
    primary = textPrimary,
    onPrimary = onPrimary,
    primaryContainer = surface,
    onPrimaryContainer = textPrimary,
    inversePrimary = onPrimary,
    secondary = textSecondary,
    onSecondary = onPrimary,
    secondaryContainer = surface,
    onSecondaryContainer = textPrimary,
    tertiary = accent,
    onTertiary = onAccent,
    tertiaryContainer = surface,
    onTertiaryContainer = textPrimary,
    background = bg,
    onBackground = textPrimary,
    surface = bg,
    onSurface = textPrimary,
    surfaceVariant = surface,
    onSurfaceVariant = textSecondary,
    surfaceTint = bg,
    inverseSurface = textPrimary,
    inverseOnSurface = onPrimary,
    error = accent,
    onError = onAccent,
    errorContainer = surface,
    onErrorContainer = textPrimary,
    outline = outline,
    outlineVariant = outlineSoft,
    scrim = scrim,
    surfaceBright = bg,
    surfaceContainer = surface,
    surfaceContainerHigh = surface,
    surfaceContainerHighest = surface,
    surfaceContainerLow = surface,
    surfaceContainerLowest = bg,
    surfaceDim = bg,
    primaryFixed = surface,
    primaryFixedDim = surfacePressed,
    onPrimaryFixed = textPrimary,
    onPrimaryFixedVariant = textSecondary,
    secondaryFixed = surface,
    secondaryFixedDim = surfacePressed,
    onSecondaryFixed = textPrimary,
    onSecondaryFixedVariant = textSecondary,
    tertiaryFixed = surface,
    tertiaryFixedDim = surfacePressed,
    onTertiaryFixed = textPrimary,
    onTertiaryFixedVariant = textSecondary,
)
