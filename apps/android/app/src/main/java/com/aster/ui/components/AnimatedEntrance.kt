package com.aster.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Fade-in-up animation wrapper.
 *
 * Wraps content in [AnimatedVisibility] that fades in and slides up from
 * a 24dp offset with configurable delay and duration.
 */
@Composable
fun AnimatedEntrance(
    visible: Boolean = true,
    delayMillis: Int = 0,
    durationMillis: Int = 400,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val offsetPx = with(density) { 24.dp.roundToPx() }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
            ),
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
            ),
            initialOffsetY = { offsetPx },
        ),
    ) {
        content()
    }
}
