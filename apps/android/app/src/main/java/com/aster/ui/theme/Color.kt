package com.aster.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Aster color system — matches aster-one's Tamagui design language.
 * Dual dark/light palettes with teal/emerald accent.
 */
object AsterDarkColors {
    val bg = Color(0xFF06060C)
    val surface1 = Color(0xFF10101E)
    val surface2 = Color(0xFF161628)
    val surface3 = Color(0xFF1C1C34)
    val text = Color(0xFFECF0F6)
    val textSubtle = Color(0xFF7B8BA2)
    val textMuted = Color(0xFF4A5670)
    val primary = Color(0xFF2DD4BF)
    val accent = Color(0xFF34D399)
    val border = Color(0xFF1A1A30)
    val borderBright = Color(0xFF252548)
}

object AsterLightColors {
    val bg = Color(0xFFF8FAFB)
    val surface1 = Color(0xFFFFFFFF)
    val surface2 = Color(0xFFF4F7F9)
    val surface3 = Color(0xFFEBF0F4)
    val text = Color(0xFF0C1222)
    val textSubtle = Color(0xFF4A5568)
    val textMuted = Color(0xFF90A1B5)
    val primary = Color(0xFF0D9488)
    val accent = Color(0xFF10B981)
    val border = Color(0xFFE2E9F0)
    val borderBright = Color(0xFFD1DAE5)
}

object SemanticColors {
    // Success (emerald)
    val success = Color(0xFF10B981)
    val successDim = Color(0xFF059669)
    val successBright = Color(0xFF34D399)

    // Warning (amber)
    val warning = Color(0xFFF59E0B)
    val warningDim = Color(0xFFD97706)
    val warningBright = Color(0xFFFBBF24)

    // Error (rose)
    val error = Color(0xFFF43F5E)
    val errorDim = Color(0xFFE11D48)
    val errorBright = Color(0xFFFB7185)

    // Info (violet)
    val info = Color(0xFF8B5CF6)
    val infoDim = Color(0xFF7C3AED)
    val infoBright = Color(0xFFA78BFA)
}
