package com.aster.ui.screens.ipc

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.aster.service.mode.ModeState
import com.aster.ui.components.AsterButton
import com.aster.ui.components.AsterButtonVariant
import com.aster.ui.components.AsterCard
import com.aster.ui.components.AsterSectionHeader
import com.aster.ui.components.AsterStatCard
import com.aster.ui.components.AsterTopBar
import com.aster.ui.components.GlowOrb
import com.aster.ui.components.TokenDisplay
import com.aster.ui.components.ToolsSection
import com.aster.ui.theme.AsterTheme
import com.aster.util.PermissionUtils

private val IpcAccent = Color(0xFFF59E0B)

private val SetupSteps = listOf(
    "Start the IPC service using the button below",
    "Copy the authentication token shown above",
    "Open the Aster One app on this device",
    "Go to Settings \u2192 Connection \u2192 IPC and paste the token",
    "The apps will connect automatically via Android Binder",
)

@Composable
fun IpcDashboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToLogs: () -> Unit,
    viewModel: IpcDashboardViewModel = hiltViewModel()
) {
    val colors = AsterTheme.colors
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val status by viewModel.status.collectAsState()
    val token by viewModel.token.collectAsState()
    val tools = viewModel.tools

    val isRunning = status.state == ModeState.RUNNING
    val isTransitioning = status.state == ModeState.STARTING || status.state == ModeState.STOPPING

    val statusText = when (status.state) {
        ModeState.IDLE -> "Idle"
        ModeState.STARTING -> "Starting..."
        ModeState.RUNNING -> "Running"
        ModeState.ERROR -> "Error: ${status.message}"
        ModeState.STOPPING -> "Stopping..."
    }

    val orbColor = when (status.state) {
        ModeState.RUNNING -> IpcAccent
        ModeState.ERROR -> colors.error
        ModeState.STARTING, ModeState.STOPPING -> colors.warning
        else -> colors.textMuted
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            AsterTopBar(
                title = "IPC Mode",
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
                            viewModel.stopIpc()
                        } else {
                            if (!PermissionUtils.checkAllPermissions(context).allGranted) {
                                Toast.makeText(
                                    context,
                                    "Grant all permissions before starting",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onNavigateToPermissions()
                            } else {
                                viewModel.startIpc()
                            }
                        }
                    },
                    text = if (isRunning) "Stop IPC" else "Start IPC",
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
            // -- Status Section --
            AsterCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GlowOrb(
                        color = orbColor,
                        size = 48.dp,
                        isAnimating = isRunning || isTransitioning
                    )

                    Column {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.text
                        )
                        if (status.state == ModeState.RUNNING) {
                            Text(
                                text = "${status.connectedClients} client(s) connected",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSubtle
                            )
                        }
                    }
                }
            }

            // -- Token Section --
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AsterSectionHeader(label = "Authentication Token")

                TokenDisplay(
                    token = token,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(token))
                    },
                    onRegenerate = { viewModel.regenerateToken() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // -- Connected Clients --
            AsterStatCard(
                icon = Icons.Default.People,
                value = status.connectedClients.toString(),
                label = "Connected Clients",
                accentColor = IpcAccent,
                modifier = Modifier.fillMaxWidth()
            )

            // -- Setup Guide --
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AsterSectionHeader(label = "Setup Guide")

                AsterCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SetupSteps.forEachIndexed { index, step ->
                            SetupStepRow(
                                stepNumber = index + 1,
                                text = step,
                                accentColor = IpcAccent,
                            )
                        }
                    }
                }
            }

            // -- Tools Section --
            if (tools.isNotEmpty()) {
                ToolsSection(
                    tools = tools,
                    accentColor = IpcAccent
                )
            }

            // -- View Logs CTA --
            AsterButton(
                onClick = onNavigateToLogs,
                text = "View Tool Call Logs",
                variant = AsterButtonVariant.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )

            // -- Info Callout --
            AsterCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = IpcAccent,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 2.dp)
                    )

                    Text(
                        text = "IPC uses Android\u2019s native Binder mechanism. No network is " +
                                "involved \u2014 communication stays entirely on-device. This is the " +
                                "fastest and most secure way to connect.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSubtle,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupStepRow(
    stepNumber: Int,
    text: String,
    accentColor: Color,
) {
    val colors = AsterTheme.colors

    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stepNumber.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold,
            )
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
