package com.aster.ui.components

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Pulsing glow circle for status indication.
 *
 * Renders a semi-transparent outer circle that pulses in scale when
 * [isAnimating] is true, with a solid inner circle at half the outer size.
 */
@Composable
fun GlowOrb(
    color: Color,
    size: Dp = 48.dp,
    isAnimating: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow_orb_pulse")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isAnimating) 1.3f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = EaseInOut,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_orb_scale",
    )

    val outerScale = if (isAnimating) scale else 1.0f
    val innerSize = size / 2

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        // Outer pulsing circle
        Box(
            modifier = Modifier
                .size(size)
                .scale(outerScale)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
        )

        // Inner solid circle
        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(CircleShape)
                .background(color),
        )
    }
}
