package com.aster.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aster.ui.theme.AsterTheme
import com.aster.ui.theme.MonospaceFamily

/**
 * Monospace code/config display with optional label and copy button.
 */
@Composable
fun CodeBlock(
    code: String,
    label: String? = null,
    onCopy: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = AsterTheme.colors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .background(colors.surface2)
            .padding(12.dp),
    ) {
        Column {
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSubtle,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = MonospaceFamily,
                ),
                color = colors.text,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            )
        }

        if (onCopy != null) {
            IconButton(
                onClick = onCopy,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = colors.textSubtle,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
