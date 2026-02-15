package com.aster.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aster.R
import com.aster.ui.components.AsterCard
import com.aster.ui.components.AsterSectionHeader
import com.aster.ui.components.AsterTopBar
import com.aster.ui.theme.AsterTheme
import compose.icons.FeatherIcons
import compose.icons.feathericons.BookOpen
import compose.icons.feathericons.ExternalLink
import compose.icons.feathericons.Github
import compose.icons.feathericons.Monitor
import compose.icons.feathericons.Moon
import compose.icons.feathericons.Server
import compose.icons.feathericons.Smartphone
import compose.icons.feathericons.Sun
import compose.icons.feathericons.Wifi

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val colors = AsterTheme.colors

    val themeMode by viewModel.themeMode.collectAsState()
    val autoStartOnBoot by viewModel.autoStartOnBoot.collectAsState()
    val autoStartMode by viewModel.autoStartMode.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            AsterTopBar(
                title = "Settings",
                onBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // =========================================================
            // APPEARANCE SECTION
            // =========================================================
            AsterSectionHeader(label = "Appearance")

            AsterCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.text
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeChip(
                            icon = FeatherIcons.Moon,
                            label = "Dark",
                            isSelected = themeMode == "dark",
                            onClick = { viewModel.setThemeMode("dark") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeChip(
                            icon = FeatherIcons.Sun,
                            label = "Light",
                            isSelected = themeMode == "light",
                            onClick = { viewModel.setThemeMode("light") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeChip(
                            icon = FeatherIcons.Monitor,
                            label = "System",
                            isSelected = themeMode == "system",
                            onClick = { viewModel.setThemeMode("system") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // =========================================================
            // AUTO-START SECTION
            // =========================================================
            AsterSectionHeader(label = "Auto-Start")

            AsterCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Enable/disable switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Start on Boot",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.text
                            )
                            Text(
                                text = "Automatically start Aster service when the device boots",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSubtle
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Switch(
                            checked = autoStartOnBoot,
                            onCheckedChange = { viewModel.setAutoStartOnBoot(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.bg,
                                checkedTrackColor = colors.primary,
                                uncheckedThumbColor = colors.textMuted,
                                uncheckedTrackColor = colors.surface2,
                                uncheckedBorderColor = colors.border
                            )
                        )
                    }

                    // Mode selector (only shown when auto-start is enabled)
                    if (autoStartOnBoot) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.border)
                        )

                        Text(
                            text = "Auto-Start Mode",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textSubtle
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ModeOption(
                                icon = FeatherIcons.Smartphone,
                                label = "IPC Mode",
                                description = "Local inter-process communication",
                                isSelected = autoStartMode == "IPC",
                                onClick = { viewModel.setAutoStartMode("IPC") }
                            )
                            ModeOption(
                                icon = FeatherIcons.Server,
                                label = "Local MCP",
                                description = "Local MCP server on device",
                                isSelected = autoStartMode == "LOCAL_MCP",
                                onClick = { viewModel.setAutoStartMode("LOCAL_MCP") }
                            )
                            ModeOption(
                                icon = FeatherIcons.Wifi,
                                label = "Remote WS",
                                description = "Connect to remote WebSocket server",
                                isSelected = autoStartMode == "REMOTE_WS",
                                onClick = { viewModel.setAutoStartMode("REMOTE_WS") }
                            )
                        }
                    }
                }
            }

            // =========================================================
            // ABOUT SECTION
            // =========================================================
            AsterSectionHeader(label = "About")

            AsterCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.bolt),
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Aster",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.text,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Android Device Controller",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSubtle
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.border)
                    )

                    AboutInfoRow(label = "Version", value = "1.1.5")
                    AboutInfoRow(label = "Build", value = "release")
                    AboutInfoRow(
                        label = "Platform",
                        value = "Android ${android.os.Build.VERSION.RELEASE}"
                    )
                }
            }

            val uriHandler = LocalUriHandler.current

            // GitHub repo link
            AsterCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        uriHandler.openUri("https://github.com/satyajiit/aster-mcp")
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = FeatherIcons.Github,
                            contentDescription = null,
                            tint = colors.textSubtle,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "GitHub Repository",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.text
                        )
                    }

                    Icon(
                        imageVector = FeatherIcons.ExternalLink,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Licenses link
            AsterCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        uriHandler.openUri("https://github.com/satyajiit/aster-mcp/blob/main/LICENSE")
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = FeatherIcons.BookOpen,
                            contentDescription = null,
                            tint = colors.textSubtle,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Open-Source Licenses",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.text
                        )
                    }

                    Icon(
                        imageVector = FeatherIcons.ExternalLink,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// =============================================================================
// THEME CHIP
// =============================================================================

@Composable
private fun ThemeChip(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AsterTheme.colors

    val borderColor = if (isSelected) colors.primary else colors.border
    val bgColor = if (isSelected) colors.primary.copy(alpha = 0.10f) else colors.surface2
    val contentColor = if (isSelected) colors.primary else colors.textSubtle

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// =============================================================================
// MODE OPTION
// =============================================================================

@Composable
private fun ModeOption(
    icon: ImageVector,
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = AsterTheme.colors

    val borderColor = if (isSelected) colors.primary else colors.border
    val bgColor = if (isSelected) colors.primary.copy(alpha = 0.06f) else colors.surface2

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) colors.primary else colors.textSubtle,
            modifier = Modifier.size(20.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) colors.text else colors.textSubtle,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.primary)
            )
        }
    }
}

// =============================================================================
// ABOUT INFO ROW
// =============================================================================

@Composable
private fun AboutInfoRow(
    label: String,
    value: String
) {
    val colors = AsterTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSubtle
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text
        )
    }
}
