package com.aster.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aster.ui.theme.AsterTheme
import com.aster.ui.theme.MonospaceFamily

/**
 * Large monospace token display with copy and regenerate action buttons.
 */
@Composable
fun TokenDisplay(
    token: String,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AsterTheme.colors

    // AsterCard wrapper: surface1 background, border, rounded corners, padding
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .background(colors.surface1)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Token text
        Text(
            text = token,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = MonospaceFamily,
                letterSpacing = 2.sp,
            ),
            color = colors.primary,
        )

        // Action buttons row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Copy button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface2)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = colors.textSubtle,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = "Copy",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSubtle,
                )
            }

            // Regenerate button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface2)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconButton(
                    onClick = onRegenerate,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Regenerate",
                        tint = colors.textSubtle,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = "Regenerate",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSubtle,
                )
            }
        }
    }
}
