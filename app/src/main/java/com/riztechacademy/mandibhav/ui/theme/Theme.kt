package com.riztechacademy.mandibhav.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Green40,
    onPrimary = Color.White,
    primaryContainer = Green90,
    onPrimaryContainer = Green10,
    inversePrimary = Green80,
    secondary = Sage40,
    onSecondary = Color.White,
    secondaryContainer = Sage90,
    onSecondaryContainer = Sage10,
    tertiary = Turmeric40,
    onTertiary = Color.White,
    tertiaryContainer = Turmeric90,
    onTertiaryContainer = Turmeric10,
    background = NeutralLight,
    onBackground = OnNeutralLight,
    surface = NeutralLight,
    onSurface = OnNeutralLight,
    surfaceVariant = NeutralVariantLight,
    onSurfaceVariant = OnNeutralVariantLight,
    inverseSurface = OnNeutralLight,
    inverseOnSurface = ContainerLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceBright = NeutralLight,
    surfaceDim = SurfaceDimLight,
    surfaceContainerLowest = ContainerLowestLight,
    surfaceContainerLow = ContainerLowLight,
    surfaceContainer = ContainerLight,
    surfaceContainerHigh = ContainerHighLight,
    surfaceContainerHighest = ContainerHighestLight,
)

private val DarkColors = darkColorScheme(
    primary = Green80,
    onPrimary = Green20,
    primaryContainer = Green30,
    onPrimaryContainer = Green90,
    inversePrimary = Green40,
    secondary = Sage80,
    onSecondary = Sage20,
    secondaryContainer = Sage30,
    onSecondaryContainer = Sage90,
    tertiary = Turmeric80,
    onTertiary = Turmeric20,
    tertiaryContainer = Turmeric30,
    onTertiaryContainer = Turmeric90,
    background = NeutralDark,
    onBackground = OnNeutralDark,
    surface = NeutralDark,
    onSurface = OnNeutralDark,
    surfaceVariant = NeutralVariantDark,
    onSurfaceVariant = OnNeutralVariantDark,
    inverseSurface = OnNeutralDark,
    inverseOnSurface = ContainerHighDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceBright = SurfaceBrightDark,
    surfaceDim = NeutralDark,
    surfaceContainerLowest = ContainerLowestDark,
    surfaceContainerLow = ContainerLowDark,
    surfaceContainer = ContainerDark,
    surfaceContainerHigh = ContainerHighDark,
    surfaceContainerHighest = ContainerHighestDark,
)

/**
 * Mandi Bhav's theme. Follows the system's dark setting.
 *
 * Dynamic colour (Android 12+) is off by default: a farmer who knows the app
 * by its green should find the same green on every phone. Pass
 * dynamicColor = true to try the wallpaper-based scheme.
 */
@Composable
fun MandiBhavTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MandiBhavTypography,
        content = content,
    )
}
