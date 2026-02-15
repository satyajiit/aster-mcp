package com.aster.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aster.R
import com.aster.service.mode.ModeType
import com.aster.ui.components.AnimatedEntrance
import com.aster.ui.components.AsterButton
import com.aster.ui.components.AsterButtonVariant
import com.aster.ui.components.BadgeItem
import com.aster.ui.components.GlowOrb
import com.aster.ui.components.ModeCard
import com.aster.ui.theme.AsterTheme
import compose.icons.FeatherIcons
import compose.icons.feathericons.Activity
import compose.icons.feathericons.Youtube

// Per-mode accent colors
private val IpcColor = Color(0xFFF59E0B)      // Amber
private val McpColor = Color(0xFF8B5CF6)       // Violet
private val RemoteColor = Color(0xFF3B82F6)    // Blue

// Badge colors
private val NpmBadgeColor = Color(0xFFCB3837)       // NPM red
private val OpenClawBadgeColor = Color(0xFF8B5CF6)   // Violet

@Composable
fun HomeScreen(
    onNavigateToIpc: () -> Unit,
    onNavigateToMcp: () -> Unit,
    onNavigateToRemote: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToIpcDashboard: () -> Unit,
    onNavigateToMcpDashboard: () -> Unit,
    onNavigateToRemoteDashboard: () -> Unit,
    onNavigateToLogs: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val colors = AsterTheme.colors
    val lastUsedMode by viewModel.lastUsedMode.collectAsState()
    val isServiceRunning = viewModel.isServiceRunning
    val activeModes = viewModel.activeModeTypes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
    ) {
        // =====================================================================
        // TOP BAR (sticky — outside scroll)
        // =====================================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.bolt),
                contentDescription = "Aster",
                tint = colors.primary,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Aster",
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.text,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Android Device Controller",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted
                )
            }

            IconButton(onClick = onNavigateToLogs) {
                Icon(
                    imageVector = FeatherIcons.Activity,
                    contentDescription = "Logs",
                    tint = colors.textSubtle
                )
            }

            IconButton(onClick = onNavigateToPermissions) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Permissions",
                    tint = colors.textSubtle
                )
            }

            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = colors.textSubtle
                )
            }
        }

        // =====================================================================
        // SCROLLABLE CONTENT
        // =====================================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // =================================================================
            // SERVICE STATUS (shows all active modes)
            // =================================================================

            if (isServiceRunning && activeModes.isNotEmpty()) {
                AnimatedEntrance(delayMillis = 0, durationMillis = 300) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeModes.forEach { modeType ->
                            val activeColor = when (modeType) {
                                ModeType.IPC -> IpcColor
                                ModeType.LOCAL_MCP -> McpColor
                                ModeType.REMOTE_WS -> RemoteColor
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlowOrb(
                                    color = activeColor,
                                    size = 28.dp,
                                    isAnimating = true
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = modeType.toDisplayName(),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = activeColor,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )

                                AsterButton(
                                    onClick = {
                                        when (modeType) {
                                            ModeType.IPC -> onNavigateToIpcDashboard()
                                            ModeType.LOCAL_MCP -> onNavigateToMcpDashboard()
                                            ModeType.REMOTE_WS -> onNavigateToRemoteDashboard()
                                        }
                                    },
                                    text = "Dashboard",
                                    variant = AsterButtonVariant.SECONDARY
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =================================================================
            // SECTION: Choose how to connect
            // =================================================================

            Text(
                text = "Choose how to connect",
                style = MaterialTheme.typography.titleMedium,
                color = colors.text,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Select a connection mode to control this device",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // =================================================================
            // REMOTE SERVER CARD (Primary — via NPM module)
            // =================================================================

            AnimatedEntrance(delayMillis = 100) {
                ModeCard(
                    title = "Remote Server",
                    tagline = "Via aster-mcp NPM Module",
                    description = "Install the aster-mcp server via NPM, connect this device via WebSocket, and control it from Claude, Cursor, or any MCP client.",
                    icon = Icons.Default.Cloud,
                    accentColor = RemoteColor,
                    features = listOf("npm install", "40+ tools", "Webhooks", "Tailscale ready"),
                    badges = listOf(
                        BadgeItem("NPM", NpmBadgeColor),
                        BadgeItem("OpenClaw", OpenClawBadgeColor)
                    ),
                    complexity = "Recommended",
                    isSelected = lastUsedMode == ModeType.REMOTE_WS.name,
                    isActive = activeModes.contains(ModeType.REMOTE_WS),
                    onClick = onNavigateToRemote,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // =================================================================
            // IPC MODE CARD
            // =================================================================

            AnimatedEntrance(delayMillis = 250) {
                ModeCard(
                    title = "IPC Mode",
                    tagline = "Direct Device Bridge",
                    description = "Connect aster-one to this device directly via native Android IPC. No network needed — fastest and most reliable.",
                    icon = Icons.Default.PhoneAndroid,
                    accentColor = IpcColor,
                    features = listOf("Zero latency", "No network", "Token auth", "40+ tools"),
                    complexity = "Beginner",
                    isSelected = lastUsedMode == ModeType.IPC.name,
                    isActive = activeModes.contains(ModeType.IPC),
                    onClick = onNavigateToIpc,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // =================================================================
            // LOCAL MCP CARD
            // =================================================================

            AnimatedEntrance(delayMillis = 400) {
                ModeCard(
                    title = "Local MCP Server",
                    tagline = "On-Device MCP Server",
                    description = "Run an MCP server directly on this device. Connect from Claude Desktop, Cursor, or any MCP-compatible client over your network.",
                    icon = Icons.Default.Dns,
                    accentColor = McpColor,
                    features = listOf(
                        "Claude Desktop",
                        "HTTP transport",
                        "LAN access",
                        "Tailscale"
                    ),
                    complexity = "Intermediate",
                    isSelected = lastUsedMode == ModeType.LOCAL_MCP.name,
                    isActive = activeModes.contains(ModeType.LOCAL_MCP),
                    onClick = onNavigateToMcp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // =================================================================
            // YOUTUBE SUBSCRIBE FOOTER
            // =================================================================

            val uriHandler = LocalUriHandler.current
            val ytRed = Color(0xFFFF0000)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = FeatherIcons.Youtube,
                    contentDescription = null,
                    tint = ytRed,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Subscribe to ",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted
                )

                Text(
                    text = "@GamesPatch",
                    style = MaterialTheme.typography.labelMedium,
                    color = ytRed,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable {
                            uriHandler.openUri("https://youtube.com/@GamesPatch")
                        }
                )

                Text(
                    text = " on YouTube",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted
                )
            }
        }
    }
}

private fun ModeType.toDisplayName(): String = when (this) {
    ModeType.IPC -> "IPC Mode"
    ModeType.LOCAL_MCP -> "Local MCP"
    ModeType.REMOTE_WS -> "Remote Server"
}
