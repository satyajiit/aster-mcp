package com.aster.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aster.service.mode.ToolInfo
import com.aster.ui.theme.AsterTheme
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bell
import compose.icons.feathericons.Camera
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Folder
import compose.icons.feathericons.HardDrive
import compose.icons.feathericons.Layers
import compose.icons.feathericons.MessageCircle
import compose.icons.feathericons.Monitor
import compose.icons.feathericons.Package
import compose.icons.feathericons.Smartphone
import compose.icons.feathericons.Terminal
import compose.icons.feathericons.Volume2

/**
 * Shared tools section used by IPC Dashboard, MCP Dashboard, and Remote Dashboard.
 * Shows categorized tools with icons and descriptions.
 *
 * @param tools List of ToolInfo (pre-sorted by ToolCatalog.resolve)
 * @param accentColor Per-mode accent color for category headers
 */
@Composable
fun ToolsSection(
    tools: List<ToolInfo>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = AsterTheme.colors
    val grouped = tools.groupBy { it.category }

    // Track expanded state per category — first category starts expanded
    val expandedState = remember {
        mutableStateMapOf<String, Boolean>().apply {
            grouped.keys.forEachIndexed { index, key ->
                put(key, index == 0)
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsterSectionHeader(
            label = "Available Tools",
            count = tools.size
        )

        grouped.forEach { (category, categoryTools) ->
            val isExpanded = expandedState[category] ?: false
            val categoryIcon = getCategoryIcon(category)

            AsterCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    // Category header (clickable to expand/collapse)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                expandedState[category] = !isExpanded
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category icon in tinted circle
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.text,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${categoryTools.size} tools",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = colors.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Expanded tool list
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            categoryTools.forEach { tool ->
                                ToolRow(tool = tool, accentColor = accentColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// TOOL ROW
// =============================================================================

@Composable
private fun ToolRow(
    tool: ToolInfo,
    accentColor: Color
) {
    val colors = AsterTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Dot indicator
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(accentColor.copy(alpha = 0.5f))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tool.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
        }
    }
}

// =============================================================================
// CATEGORY ICONS
// =============================================================================

private fun getCategoryIcon(category: String): ImageVector = when (category) {
    "Screen Control" -> FeatherIcons.Monitor
    "Device" -> FeatherIcons.Smartphone
    "Files" -> FeatherIcons.Folder
    "Camera" -> FeatherIcons.Camera
    "Communication" -> FeatherIcons.MessageCircle
    "Notifications" -> FeatherIcons.Bell
    "Media" -> FeatherIcons.Volume2
    "Storage" -> FeatherIcons.HardDrive
    "Apps" -> FeatherIcons.Package
    "System" -> FeatherIcons.Terminal
    "Overlays" -> FeatherIcons.Layers
    "Alarms" -> FeatherIcons.Clock
    else -> FeatherIcons.Terminal
}
