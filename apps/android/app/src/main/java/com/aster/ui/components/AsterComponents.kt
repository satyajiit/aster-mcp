package com.aster.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aster.ui.theme.AsterTheme

// =============================================================================
// ASTER CARD
// =============================================================================

/**
 * Standard card container with surface1 background and subtle border.
 */
@Composable
fun AsterCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = AsterTheme.colors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .background(colors.surface1)
            .padding(16.dp)
    ) {
        content()
    }
}

// =============================================================================
// ASTER BUTTON
// =============================================================================

enum class AsterButtonVariant {
    PRIMARY, SECONDARY, DANGER
}

/**
 * Themed button with three variants: PRIMARY, SECONDARY, and DANGER.
 */
@Composable
fun AsterButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    variant: AsterButtonVariant = AsterButtonVariant.PRIMARY,
    enabled: Boolean = true
) {
    val colors = AsterTheme.colors

    val backgroundColor: Color
    val contentColor: Color
    val borderColor: Color?

    when (variant) {
        AsterButtonVariant.PRIMARY -> {
            backgroundColor = if (enabled) colors.primary else colors.primary.copy(alpha = 0.4f)
            contentColor = colors.bg
            borderColor = null
        }

        AsterButtonVariant.SECONDARY -> {
            backgroundColor = if (enabled) colors.surface2 else colors.surface2.copy(alpha = 0.4f)
            contentColor = if (enabled) colors.text else colors.textMuted
            borderColor = colors.border
        }

        AsterButtonVariant.DANGER -> {
            backgroundColor = if (enabled) colors.error else colors.error.copy(alpha = 0.4f)
            contentColor = Color.White
            borderColor = null
        }
    }

    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier
            )
            .background(backgroundColor)
            .then(
                if (variant == AsterButtonVariant.PRIMARY && enabled) {
                    Modifier.drawBehind {
                        // Subtle glow beneath primary button
                        drawCircle(
                            color = colors.primary.copy(alpha = 0.15f),
                            radius = size.maxDimension * 0.6f,
                            center = Offset(size.width / 2, size.height)
                        )
                    }
                } else Modifier
            )
            .clickable(enabled = enabled, onClick = onClick)
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// =============================================================================
// ASTER TEXT FIELD
// =============================================================================

/**
 * Themed outlined text field with Aster color integration.
 */
@Composable
fun AsterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val colors = AsterTheme.colors

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = if (label != null) {
            { Text(text = label) }
        } else null,
        placeholder = if (placeholder != null) {
            {
                Text(
                    text = placeholder,
                    color = colors.textMuted
                )
            }
        } else null,
        singleLine = singleLine,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.text,
            unfocusedTextColor = colors.text,
            cursorColor = colors.primary,
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.border,
            focusedLabelColor = colors.primary,
            unfocusedLabelColor = colors.textSubtle,
            focusedPlaceholderColor = colors.textMuted,
            unfocusedPlaceholderColor = colors.textMuted
        )
    )
}

// =============================================================================
// ASTER TOP BAR
// =============================================================================

/**
 * Top bar with optional back navigation and action slot.
 */
@Composable
fun AsterTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null
) {
    val colors = AsterTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.text
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.text,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (actions != null) {
                actions()
            }
        }

        // Bottom border line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.border)
        )
    }
}

// =============================================================================
// STATUS BADGE
// =============================================================================

/**
 * Simple colored circle indicator.
 */
@Composable
fun StatusBadge(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

// =============================================================================
// ASTER STAT CARD
// =============================================================================

/**
 * Card with a colored icon background, large value, and label.
 */
@Composable
fun AsterStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accentColor: Color = AsterTheme.colors.primary
) {
    val colors = AsterTheme.colors
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .border(1.dp, colors.border, shape)
            .background(colors.surface1)
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.text,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSubtle
                )
            }
        }
    }
}

// =============================================================================
// SECTION HEADER
// =============================================================================

/**
 * Section label with optional count pill badge.
 */
@Composable
fun AsterSectionHeader(
    label: String,
    modifier: Modifier = Modifier,
    count: Int? = null
) {
    val colors = AsterTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSubtle
        )

        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.bg,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.primary)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

// =============================================================================
// INFO ROW
// =============================================================================

/**
 * Horizontal row displaying an icon, label, and value.
 */
@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = AsterTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSubtle,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSubtle
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text
        )
    }
}

// =============================================================================
// TOOL LIST ITEM
// =============================================================================

/**
 * List item for displaying a tool with name and description, separated by a
 * bottom border.
 */
@Composable
fun ToolListItem(
    name: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val colors = AsterTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface1)
            .drawBehind {
                // Bottom border separator
                drawLine(
                    color = colors.border,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(16.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = colors.text
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSubtle
        )
    }
}
