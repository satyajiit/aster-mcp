package com.aster.ui.screens.mcp

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aster.service.mode.ModeState
import com.aster.ui.components.AsterButton
import com.aster.ui.components.AsterButtonVariant
import com.aster.ui.components.AsterCard
import com.aster.ui.components.AsterSectionHeader
import com.aster.ui.components.AsterTopBar
import com.aster.ui.components.CodeBlock
import com.aster.ui.components.GlowOrb
import com.aster.ui.components.ToolsSection
import com.aster.ui.theme.AsterTheme
import com.aster.util.PermissionUtils

private val Violet = Color(0xFF8B5CF6)

@Composable
fun McpDashboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToLogs: () -> Unit,
    viewModel: McpDashboardViewModel = hiltViewModel()
) {
    val colors = AsterTheme.colors
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val status by viewModel.status.collectAsState()
    val port by viewModel.port.collectAsState()
    val tools = viewModel.tools

    val isRunning = status.state == ModeState.RUNNING
    val isTransitioning = status.state == ModeState.STARTING || status.state == ModeState.STOPPING

    val statusText = when (status.state) {
        ModeState.IDLE -> "Idle"
        ModeState.STARTING -> "Starting..."
        ModeState.RUNNING -> "Running on port $port"
        ModeState.ERROR -> "Error: ${status.message}"
        ModeState.STOPPING -> "Stopping..."
    }

    val orbColor = when (status.state) {
        ModeState.RUNNING -> Violet
        ModeState.ERROR -> colors.error
        ModeState.STARTING, ModeState.STOPPING -> colors.warning
        else -> colors.textMuted
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            AsterTopBar(
                title = "Local MCP",
                onBack = onNavigateBack
            )
        },
        bottomBar = {
            // Sticky start/stop button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                AsterButton(
                    onClick = {
                        if (isRunning) {
                            viewModel.stopMcp()
                        } else {
                            if (!PermissionUtils.checkAllPermissions(context).allGranted) {
                                Toast.makeText(
                                    context,
                                    "Grant all permissions before starting",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onNavigateToPermissions()
                            } else {
                                viewModel.startMcp()
                            }
                        }
                    },
                    text = if (isRunning) "Stop MCP" else "Start MCP",
                    variant = if (isRunning) AsterButtonVariant.DANGER else AsterButtonVariant.PRIMARY,
                    enabled = !isTransitioning,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // -- Server Endpoints (FIRST) --
            ServerEndpointsSection(
                localhostUrl = viewModel.localhostUrl,
                lanUrl = viewModel.lanUrl,
                tailscaleUrl = viewModel.tailscaleUrl,
                tailscaleDnsUrl = viewModel.tailscaleDnsUrl,
                onCopy = { url -> clipboardManager.setText(AnnotatedString(url)) }
            )

            // -- Status Section --
            StatusSection(
                statusText = statusText,
                orbColor = orbColor,
                connectedClients = status.connectedClients,
                isAnimating = isRunning || isTransitioning
            )

            // -- MCP Client Config --
            McpConfigSection(port = port)

            // -- Setup Guide --
            SetupGuideSection(port = port)

            // -- Tailscale Tip --
            TailscaleTipCallout()

            // -- Tools Section --
            if (tools.isNotEmpty()) {
                ToolsSection(
                    tools = tools,
                    accentColor = Violet
                )
            }

            // -- View Logs CTA --
            AsterButton(
                onClick = onNavigateToLogs,
                text = "View Tool Call Logs",
                variant = AsterButtonVariant.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// =============================================================================
// SERVER ENDPOINTS SECTION
// =============================================================================

@Composable
private fun ServerEndpointsSection(
    localhostUrl: String,
    lanUrl: String?,
    tailscaleUrl: String?,
    tailscaleDnsUrl: String? = null,
    onCopy: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AsterSectionHeader(label = "Server Endpoints")

        CodeBlock(
            code = localhostUrl,
            label = "Same Device",
            onCopy = { onCopy(localhostUrl) },
            modifier = Modifier.fillMaxWidth()
        )

        if (lanUrl != null) {
            CodeBlock(
                code = lanUrl,
                label = "Local Network",
                onCopy = { onCopy(lanUrl) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (tailscaleDnsUrl != null) {
            CodeBlock(
                code = tailscaleDnsUrl,
                label = "Tailscale (DNS)",
                onCopy = { onCopy(tailscaleDnsUrl) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (tailscaleUrl != null) {
            CodeBlock(
                code = tailscaleUrl,
                label = if (tailscaleDnsUrl != null) "Tailscale (IP)" else "Tailscale",
                onCopy = { onCopy(tailscaleUrl) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// =============================================================================
// STATUS SECTION
// =============================================================================

@Composable
private fun StatusSection(
    statusText: String,
    orbColor: Color,
    connectedClients: Int,
    isAnimating: Boolean
) {
    val colors = AsterTheme.colors

    AsterCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlowOrb(
                color = orbColor,
                size = 48.dp,
                isAnimating = isAnimating
            )

            Column {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.text
                )
                if (connectedClients > 0) {
                    Text(
                        text = "$connectedClients client(s) connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSubtle
                    )
                }
            }
        }
    }
}

// =============================================================================
// MCP CLIENT CONFIG SECTION
// =============================================================================

@Composable
private fun McpConfigSection(port: Int) {
    val colors = AsterTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AsterSectionHeader(label = "MCP Client Config")

        AsterCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Add to your .mcp.json (Claude Desktop / Cursor):",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSubtle
                )

                CodeBlock(
                    code = "{\n  \"mcpServers\": {\n    \"aster-local\": {\n      \"type\": \"http\",\n      \"url\": \"http://<ip>:$port/mcp\"\n    }\n  }\n}",
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Replace <ip> with a LAN or Tailscale IP from the endpoints above. Uses Streamable HTTP transport.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }
        }
    }
}

// =============================================================================
// SETUP GUIDE SECTION
// =============================================================================

@Composable
private fun SetupGuideSection(port: Int) {
    val colors = AsterTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AsterSectionHeader(label = "Setup Guide")

        AsterCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Claude Desktop / Cursor",
                    style = MaterialTheme.typography.titleSmall,
                    color = Violet,
                    fontWeight = FontWeight.SemiBold
                )

                SetupStep(
                    number = 1,
                    text = "Start the MCP server using the button below"
                )
                SetupStep(
                    number = 2,
                    text = "Copy a server endpoint URL from above"
                )
                SetupStep(
                    number = 3,
                    text = "Open Claude Desktop settings (or Cursor MCP config)"
                )
                SetupStep(
                    number = 4,
                    text = "Add a new MCP server with type \"http\" and paste the URL"
                )
                SetupStep(
                    number = 5,
                    text = "Restart the client \u2014 your Android device tools will appear"
                )

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.border)
                )

                // Connection method hints
                ConnectionMethodHint(
                    label = "Same Device",
                    description = "Use the localhost URL: http://127.0.0.1:$port/mcp"
                )

                ConnectionMethodHint(
                    label = "Over LAN",
                    description = "Use the LAN IP URL (both devices must be on the same WiFi network)"
                )

                ConnectionMethodHint(
                    label = "Via Tailscale (Recommended for remote)",
                    description = "Install Tailscale on both devices. Use the Tailscale IP URL for secure access from anywhere \u2014 no port forwarding needed."
                )
            }
        }
    }
}

@Composable
private fun SetupStep(
    number: Int,
    text: String
) {
    val colors = AsterTheme.colors

    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Violet),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun ConnectionMethodHint(
    label: String,
    description: String
) {
    val colors = AsterTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Violet,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSubtle
        )
    }
}

// =============================================================================
// TAILSCALE TIP CALLOUT
// =============================================================================

@Composable
private fun TailscaleTipCallout() {
    val colors = AsterTheme.colors
    val tipBackground = if (colors.isDark) {
        Violet.copy(alpha = 0.08f)
    } else {
        Violet.copy(alpha = 0.06f)
    }
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, Violet.copy(alpha = 0.25f), shape)
            .background(tipBackground)
            .padding(14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = Violet,
                modifier = Modifier.size(20.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Tailscale Tip",
                    style = MaterialTheme.typography.titleSmall,
                    color = Violet,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "For the best remote experience, install Tailscale on this device and your computer. " +
                            "It creates a secure private network \u2014 no port forwarding or firewall config needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSubtle
                )
            }
        }
    }
}

