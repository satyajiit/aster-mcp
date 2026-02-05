package com.aster.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Terminal-inspired color palette matching the Aster Nuxt dashboard.
 * Blue-slate backgrounds with cyan accent - sophisticated CRT aesthetic.
 */
object AsterColors {
    // Background layers (blue-slate palette)
    val TerminalBg = Color(0xFF0F172A)              // Deep blue-slate
    val TerminalSurface = Color(0xFF1E293B)         // Card/panel backgrounds
    val TerminalSurfaceElevated = Color(0xFF334155) // Elevated surfaces
    val TerminalHover = Color(0xFF475569)           // Hover states

    // Borders (slate with transparency feel)
    val TerminalBorder = Color(0xFF2D3B4F)          // Subtle borders ~12% slate
    val TerminalBorderBright = Color(0xFF3D4D65)    // Prominent borders ~20% slate

    // Text hierarchy
    val TerminalText = Color(0xFFF1F5F9)            // Primary text (off-white)
    val TerminalTextBright = Color(0xFFFFFFFF)      // Pure white emphasis
    val TerminalMuted = Color(0xFF94A3B8)           // Secondary text (gray)
    val TerminalDim = Color(0xFF64748B)             // Tertiary text (darker gray)

    // Primary accent - Cyan (replacing phosphor green)
    val Primary = Color(0xFF22D3EE)                 // Main cyan accent
    val PrimaryDim = Color(0xFF06B6D4)              // Darker cyan
    val PrimaryBright = Color(0xFF67E8F9)           // Lighter cyan
    val PrimaryGlow = Color(0x2622D3EE)             // 15% opacity glow

    // Semantic colors - Success (Emerald)
    val Emerald = Color(0xFF10B981)
    val EmeraldDim = Color(0xFF059669)
    val EmeraldBright = Color(0xFF34D399)
    val EmeraldGlow = Color(0x2610B981)

    // Semantic colors - Warning (Amber)
    val Amber = Color(0xFFF59E0B)
    val AmberDim = Color(0xFFD97706)
    val AmberBright = Color(0xFFFBBF24)
    val AmberGlow = Color(0x26F59E0B)

    // Semantic colors - Error (Rose)
    val Rose = Color(0xFFF43F5E)
    val RoseDim = Color(0xFFE11D48)
    val RoseBright = Color(0xFFFB7185)
    val RoseGlow = Color(0x26F43F5E)

    // Semantic colors - Info (Violet)
    val Violet = Color(0xFF8B5CF6)
    val VioletDim = Color(0xFF7C3AED)
    val VioletBright = Color(0xFFA78BFA)
    val VioletGlow = Color(0x268B5CF6)

    // Semantic colors - Blue
    val Blue = Color(0xFF3B82F6)
    val BlueDim = Color(0xFF2563EB)
    val BlueBright = Color(0xFF60A5FA)
    val BlueGlow = Color(0x263B82F6)

    // Semantic colors - Indigo
    val Indigo = Color(0xFF6366F1)
    val IndigoDim = Color(0xFF4F46E5)
    val IndigoBright = Color(0xFF818CF8)
    val IndigoGlow = Color(0x266366F1)

    // Legacy status colors (for compatibility)
    val TerminalRed = Rose
    val TerminalYellow = Amber
    val TerminalBlue = Blue
    val TerminalCyan = Primary
    val TerminalMagenta = Violet

    // Status shorthand
    val Success = Color(0xFF22C55E)
    val Warning = Color(0xFFEAB308)
    val Error = Color(0xFFEF4444)
    val Info = Color(0xFF3B82F6)

    // macOS-style window dots
    val DotClose = Color(0xFFFF5F57)
    val DotMinimize = Color(0xFFFEBC2E)
    val DotMaximize = Color(0xFF28C840)

    // Scanline effect color
    val Scanline = Color(0x0494A3B8)                // ~1.5% opacity slate
}
