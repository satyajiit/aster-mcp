package com.aster.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Extended color scheme for terminal-specific colors.
 * Provides access to the full dashboard color palette.
 */
data class AsterExtendedColors(
    // Primary accent (Cyan)
    val primary: Color = AsterColors.Primary,
    val primaryDim: Color = AsterColors.PrimaryDim,
    val primaryBright: Color = AsterColors.PrimaryBright,
    val primaryGlow: Color = AsterColors.PrimaryGlow,

    // Semantic - Emerald (Success)
    val emerald: Color = AsterColors.Emerald,
    val emeraldDim: Color = AsterColors.EmeraldDim,
    val emeraldBright: Color = AsterColors.EmeraldBright,
    val emeraldGlow: Color = AsterColors.EmeraldGlow,

    // Semantic - Amber (Warning)
    val amber: Color = AsterColors.Amber,
    val amberDim: Color = AsterColors.AmberDim,
    val amberBright: Color = AsterColors.AmberBright,
    val amberGlow: Color = AsterColors.AmberGlow,

    // Semantic - Rose (Error/Danger)
    val rose: Color = AsterColors.Rose,
    val roseDim: Color = AsterColors.RoseDim,
    val roseBright: Color = AsterColors.RoseBright,
    val roseGlow: Color = AsterColors.RoseGlow,

    // Semantic - Violet (Info/Special)
    val violet: Color = AsterColors.Violet,
    val violetDim: Color = AsterColors.VioletDim,
    val violetBright: Color = AsterColors.VioletBright,
    val violetGlow: Color = AsterColors.VioletGlow,

    // Semantic - Blue
    val blue: Color = AsterColors.Blue,
    val blueDim: Color = AsterColors.BlueDim,
    val blueBright: Color = AsterColors.BlueBright,
    val blueGlow: Color = AsterColors.BlueGlow,

    // Semantic - Indigo
    val indigo: Color = AsterColors.Indigo,
    val indigoDim: Color = AsterColors.IndigoDim,
    val indigoBright: Color = AsterColors.IndigoBright,
    val indigoGlow: Color = AsterColors.IndigoGlow,

    // Backgrounds
    val terminalBg: Color = AsterColors.TerminalBg,
    val terminalSurface: Color = AsterColors.TerminalSurface,
    val terminalSurfaceElevated: Color = AsterColors.TerminalSurfaceElevated,
    val terminalHover: Color = AsterColors.TerminalHover,

    // Borders
    val terminalBorder: Color = AsterColors.TerminalBorder,
    val terminalBorderBright: Color = AsterColors.TerminalBorderBright,

    // Text
    val terminalText: Color = AsterColors.TerminalText,
    val terminalTextBright: Color = AsterColors.TerminalTextBright,
    val terminalMuted: Color = AsterColors.TerminalMuted,
    val terminalDim: Color = AsterColors.TerminalDim,

    // Status shorthand
    val success: Color = AsterColors.Success,
    val warning: Color = AsterColors.Warning,
    val error: Color = AsterColors.Error,
    val info: Color = AsterColors.Info,

    // Window dots
    val dotClose: Color = AsterColors.DotClose,
    val dotMinimize: Color = AsterColors.DotMinimize,
    val dotMaximize: Color = AsterColors.DotMaximize,

    // Effects
    val scanline: Color = AsterColors.Scanline,
)

val LocalAsterColors = staticCompositionLocalOf { AsterExtendedColors() }

private val DarkColorScheme = darkColorScheme(
    // Primary - Cyan accent
    primary = AsterColors.Primary,
    onPrimary = AsterColors.TerminalBg,
    primaryContainer = AsterColors.PrimaryDim,
    onPrimaryContainer = AsterColors.TerminalTextBright,

    // Secondary - Emerald
    secondary = AsterColors.Emerald,
    onSecondary = AsterColors.TerminalBg,
    secondaryContainer = AsterColors.TerminalSurfaceElevated,
    onSecondaryContainer = AsterColors.Emerald,

    // Tertiary - Violet
    tertiary = AsterColors.Violet,
    onTertiary = AsterColors.TerminalBg,
    tertiaryContainer = AsterColors.TerminalSurfaceElevated,
    onTertiaryContainer = AsterColors.Violet,

    // Error - Rose
    error = AsterColors.Rose,
    onError = AsterColors.TerminalBg,
    errorContainer = Color(0xFF3D0A14),
    onErrorContainer = AsterColors.RoseBright,

    // Background layers
    background = AsterColors.TerminalBg,
    onBackground = AsterColors.TerminalText,

    // Surface layers
    surface = AsterColors.TerminalSurface,
    onSurface = AsterColors.TerminalText,
    surfaceVariant = AsterColors.TerminalSurfaceElevated,
    onSurfaceVariant = AsterColors.TerminalMuted,

    // Borders
    outline = AsterColors.TerminalBorder,
    outlineVariant = AsterColors.TerminalBorderBright,

    // Inverse colors
    inverseSurface = AsterColors.TerminalTextBright,
    inverseOnSurface = AsterColors.TerminalBg,
    inversePrimary = AsterColors.PrimaryDim,

    // Tint and scrim
    surfaceTint = AsterColors.Primary,
    scrim = Color.Black,
)

@Composable
fun AsterTheme(
    darkTheme: Boolean = true, // Always dark - terminal aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val asterColors = AsterExtendedColors()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AsterColors.TerminalBg.toArgb()
            window.navigationBarColor = AsterColors.TerminalBg.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(
        LocalAsterColors provides asterColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AsterTypography,
            content = content
        )
    }
}

/**
 * Access extended terminal colors from anywhere in the composition.
 * Usage: AsterTheme.colors.primary, AsterTheme.colors.emerald, etc.
 */
object AsterTheme {
    val colors: AsterExtendedColors
        @Composable
        get() = LocalAsterColors.current
}
