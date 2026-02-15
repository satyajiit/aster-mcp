package com.aster.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Immutable
data class AsterColorScheme(
    val bg: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val text: Color,
    val textSubtle: Color,
    val textMuted: Color,
    val primary: Color,
    val accent: Color,
    val border: Color,
    val borderBright: Color,
    val success: Color,
    val successDim: Color,
    val successBright: Color,
    val warning: Color,
    val warningDim: Color,
    val warningBright: Color,
    val error: Color,
    val errorDim: Color,
    val errorBright: Color,
    val info: Color,
    val infoDim: Color,
    val infoBright: Color,
    val isDark: Boolean,
)

val DarkAsterColorScheme = AsterColorScheme(
    bg = AsterDarkColors.bg,
    surface1 = AsterDarkColors.surface1,
    surface2 = AsterDarkColors.surface2,
    surface3 = AsterDarkColors.surface3,
    text = AsterDarkColors.text,
    textSubtle = AsterDarkColors.textSubtle,
    textMuted = AsterDarkColors.textMuted,
    primary = AsterDarkColors.primary,
    accent = AsterDarkColors.accent,
    border = AsterDarkColors.border,
    borderBright = AsterDarkColors.borderBright,
    success = SemanticColors.success,
    successDim = SemanticColors.successDim,
    successBright = SemanticColors.successBright,
    warning = SemanticColors.warning,
    warningDim = SemanticColors.warningDim,
    warningBright = SemanticColors.warningBright,
    error = SemanticColors.error,
    errorDim = SemanticColors.errorDim,
    errorBright = SemanticColors.errorBright,
    info = SemanticColors.info,
    infoDim = SemanticColors.infoDim,
    infoBright = SemanticColors.infoBright,
    isDark = true,
)

val LightAsterColorScheme = AsterColorScheme(
    bg = AsterLightColors.bg,
    surface1 = AsterLightColors.surface1,
    surface2 = AsterLightColors.surface2,
    surface3 = AsterLightColors.surface3,
    text = AsterLightColors.text,
    textSubtle = AsterLightColors.textSubtle,
    textMuted = AsterLightColors.textMuted,
    primary = AsterLightColors.primary,
    accent = AsterLightColors.accent,
    border = AsterLightColors.border,
    borderBright = AsterLightColors.borderBright,
    success = SemanticColors.success,
    successDim = SemanticColors.successDim,
    successBright = SemanticColors.successBright,
    warning = SemanticColors.warning,
    warningDim = SemanticColors.warningDim,
    warningBright = SemanticColors.warningBright,
    error = SemanticColors.error,
    errorDim = SemanticColors.errorDim,
    errorBright = SemanticColors.errorBright,
    info = SemanticColors.info,
    infoDim = SemanticColors.infoDim,
    infoBright = SemanticColors.infoBright,
    isDark = false,
)

val LocalAsterColors = staticCompositionLocalOf { DarkAsterColorScheme }

private val DarkMaterialScheme = darkColorScheme(
    primary = AsterDarkColors.primary,
    onPrimary = AsterDarkColors.bg,
    primaryContainer = Color(0xFF0A3A35),
    onPrimaryContainer = AsterDarkColors.primary,
    secondary = AsterDarkColors.accent,
    onSecondary = AsterDarkColors.bg,
    secondaryContainer = AsterDarkColors.surface3,
    onSecondaryContainer = AsterDarkColors.accent,
    tertiary = SemanticColors.info,
    onTertiary = AsterDarkColors.bg,
    error = SemanticColors.error,
    onError = AsterDarkColors.bg,
    errorContainer = Color(0xFF3D0A14),
    onErrorContainer = SemanticColors.errorBright,
    background = AsterDarkColors.bg,
    onBackground = AsterDarkColors.text,
    surface = AsterDarkColors.surface1,
    onSurface = AsterDarkColors.text,
    surfaceVariant = AsterDarkColors.surface2,
    onSurfaceVariant = AsterDarkColors.textSubtle,
    outline = AsterDarkColors.border,
    outlineVariant = AsterDarkColors.borderBright,
    inverseSurface = AsterDarkColors.text,
    inverseOnSurface = AsterDarkColors.bg,
    inversePrimary = AsterLightColors.primary,
    surfaceTint = AsterDarkColors.primary,
    scrim = Color.Black,
)

private val LightMaterialScheme = lightColorScheme(
    primary = AsterLightColors.primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2F5EA),
    onPrimaryContainer = Color(0xFF042F2E),
    secondary = AsterLightColors.accent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF022C22),
    tertiary = SemanticColors.info,
    onTertiary = Color.White,
    error = SemanticColors.error,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E8),
    onErrorContainer = SemanticColors.errorDim,
    background = AsterLightColors.bg,
    onBackground = AsterLightColors.text,
    surface = AsterLightColors.surface1,
    onSurface = AsterLightColors.text,
    surfaceVariant = AsterLightColors.surface2,
    onSurfaceVariant = AsterLightColors.textSubtle,
    outline = AsterLightColors.border,
    outlineVariant = AsterLightColors.borderBright,
    inverseSurface = AsterLightColors.text,
    inverseOnSurface = AsterLightColors.bg,
    inversePrimary = AsterDarkColors.primary,
    surfaceTint = AsterLightColors.primary,
    scrim = Color.Black,
)

@Composable
fun AsterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val materialScheme = if (darkTheme) DarkMaterialScheme else LightMaterialScheme
    val asterColors = if (darkTheme) DarkAsterColorScheme else LightAsterColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = asterColors.bg.toArgb()
            window.navigationBarColor = asterColors.bg.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalAsterColors provides asterColors
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = AsterTypography,
            content = content
        )
    }
}

object AsterTheme {
    val colors: AsterColorScheme
        @Composable
        get() = LocalAsterColors.current
}
