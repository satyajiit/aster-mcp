package com.aster.ui.components

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aster.ui.theme.AsterTheme

data class BadgeItem(
    val text: String,
    val color: Color
)

private val ActiveBadgeColor = Color(0xFF22C55E) // Green-500

/**
 * Rich mode selection card with per-mode accent color, feature tags,
 * setup complexity indicator, and active/selected states.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModeCard(
    title: String,
    tagline: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    features: List<String> = emptyList(),
    complexity: String? = null,
    badges: List<BadgeItem> = emptyList(),
    isSelected: Boolean = false,
    isActive: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AsterTheme.colors

    val borderColor = when {
        isActive -> accentColor
        else -> colors.border
    }

    // Glow animation for active state
    val infiniteTransition = rememberInfiniteTransition(label = "mode_card_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha",
    )

    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (isActive) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = accentColor.copy(alpha = glowAlpha),
                            cornerRadius = CornerRadius(16.dp.toPx()),
                        )
                    }
                } else Modifier
            )
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = borderColor,
                shape = shape,
            )
            .background(colors.surface1, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = accentColor),
                onClick = onClick,
            )
            .padding(16.dp),
    ) {
        // Top row: icon + title + badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon in colored circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.text,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Medium,
                )
                if (badges.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        badges.forEach { badge ->
                            Text(
                                text = badge.text,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badge.color)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            // Active / Last used badge
            if (isActive) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ActiveBadgeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ActiveBadgeColor)
                    )
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = ActiveBadgeColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else if (isSelected) {
                Text(
                    text = "Last used",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surface2)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Description
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSubtle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        // Feature tags
        if (features.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                features.forEach { feature ->
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSubtle,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(
                                1.dp,
                                colors.border,
                                RoundedCornerShape(6.dp)
                            )
                            .background(colors.surface2)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }

        // Bottom row: complexity
        if (complexity != null) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = complexity,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
