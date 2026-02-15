package com.aster.ui.screens.remote

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aster.data.model.ConnectionState
import com.aster.service.mode.ModeState
import com.aster.ui.components.AsterButton
import com.aster.ui.components.AsterButtonVariant
import com.aster.ui.components.AsterCard
import com.aster.ui.components.AsterSectionHeader
import com.aster.ui.components.AsterStatCard
import com.aster.ui.components.AsterTopBar
import com.aster.ui.components.GlowOrb
import com.aster.ui.components.InfoRow
import com.aster.ui.components.ToolsSection
import com.aster.ui.theme.AsterTheme
import compose.icons.FeatherIcons
import compose.icons.feathericons.Clock
import compose.icons.feathericons.Cpu
import compose.icons.feathericons.Globe
import compose.icons.feathericons.Server
import compose.icons.feathericons.Smartphone
import compose.icons.feathericons.Users
import compose.icons.feathericons.Wifi
import compose.icons.feathericons.Zap
import kotlinx.coroutines.delay
import java.net.NetworkInterface

private val BlueAccent = Color(0xFF3B82F6)
private val GreenAccent = Color(0xFF22C55E)
private val AmberAccent = Color(0xFFF59E0B)
private val VioletAccent = Color(0xFF8B5CF6)

@Composable
fun RemoteDashboardScreen(
    onNavigateBack: () -> Unit,
    onDisconnected: () -> Unit,
    onNavigateToLogs: () -> Unit,
    viewModel: RemoteViewModel = hiltViewModel()
) {
    val colors = AsterTheme.colors

    val connectionState by viewModel.connectionState.collectAsState()
    val status by viewModel.status.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val tools = viewModel.tools

    // Track uptime
    var uptimeSeconds by remember { mutableLongStateOf(0L) }

    // Uptime counter
    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.APPROVED) {
            uptimeSeconds = 0
            while (true) {
                delay(1000)
                uptimeSeconds++
            }
        }
    }

    // Auto-navigate back when disconnected
    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.DISCONNECTED) {
            onDisconnected()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            AsterTopBar(
                title = "Remote Server",
                onBack = onNavigateBack
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bg)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                AsterButton(
                    onClick = { viewModel.disconnect() },
                    text = "Disconnect",
                    variant = AsterButtonVariant.DANGER,
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
            // -- Connection Status --
            AsterCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isRunning = status.state == ModeState.RUNNING
                    GlowOrb(
                        color = if (isRunning) BlueAccent else colors.warning,
                        size = 48.dp,
                        isAnimating = isRunning
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (connectionState) {
                                ConnectionState.APPROVED -> "Connected"
                                ConnectionState.CONNECTING -> "Connecting..."
                                ConnectionState.PENDING_APPROVAL -> "Pending Approval"
                                ConnectionState.ERROR -> "Connection Error"
                                else -> "Disconnected"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.text,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = when (status.state) {
                                ModeState.RUNNING -> "Service is active and processing commands"
                                ModeState.STARTING -> "Service is starting up..."
                                ModeState.ERROR -> status.message.ifEmpty { "An error occurred" }
                                ModeState.STOPPING -> "Service is shutting down..."
                                ModeState.IDLE -> "Service is idle"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSubtle
                        )

                        if (serverUrl.isNotBlank()) {
                            Text(
                                text = serverUrl,
                                style = MaterialTheme.typography.labelSmall,
                                color = BlueAccent
                            )
                        }
                    }
                }
            }

            // -- Statistics --
            AsterSectionHeader(label = "Statistics")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsterStatCard(
                    icon = FeatherIcons.Clock,
                    value = formatUptime(uptimeSeconds),
                    label = "Uptime",
                    accentColor = BlueAccent,
                    modifier = Modifier.weight(1f)
                )

                AsterStatCard(
                    icon = FeatherIcons.Users,
                    value = "${status.connectedClients}",
                    label = "Clients",
                    accentColor = GreenAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsterStatCard(
                    icon = FeatherIcons.Zap,
                    value = status.state.name.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    label = "Status",
                    accentColor = when (status.state) {
                        ModeState.RUNNING -> GreenAccent
                        ModeState.ERROR -> colors.error
                        else -> AmberAccent
                    },
                    modifier = Modifier.weight(1f)
                )

                AsterStatCard(
                    icon = FeatherIcons.Server,
                    value = "WebSocket",
                    label = "Protocol",
                    accentColor = VioletAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            // -- Device Info --
            AsterSectionHeader(label = "Device Info")

            AsterCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoRow(
                        icon = FeatherIcons.Smartphone,
                        label = "Device",
                        value = "${Build.MANUFACTURER} ${Build.MODEL}"
                    )

                    InfoRow(
                        icon = FeatherIcons.Cpu,
                        label = "Android",
                        value = "API ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})"
                    )

                    InfoRow(
                        icon = FeatherIcons.Globe,
                        label = "Local IP",
                        value = getLocalIpAddress()
                    )

                    InfoRow(
                        icon = FeatherIcons.Wifi,
                        label = "Connection",
                        value = when (connectionState) {
                            ConnectionState.APPROVED -> "Stable"
                            ConnectionState.CONNECTING -> "Establishing..."
                            ConnectionState.PENDING_APPROVAL -> "Awaiting"
                            else -> "None"
                        }
                    )
                }
            }

            // -- Tools Section --
            if (tools.isNotEmpty()) {
                ToolsSection(
                    tools = tools,
                    accentColor = BlueAccent
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

private fun formatUptime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, secs)
}

private fun getLocalIpAddress(): String {
    return try {
        val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
        for (iface in interfaces) {
            if (!iface.isUp || iface.isLoopback) continue
            val addresses = iface.inetAddresses.toList()
            for (addr in addresses) {
                if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                    return addr.hostAddress ?: "Unknown"
                }
            }
        }
        "Unknown"
    } catch (_: Exception) {
        "Unknown"
    }
}
