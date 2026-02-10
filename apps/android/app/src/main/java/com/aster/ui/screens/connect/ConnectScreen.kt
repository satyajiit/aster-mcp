package com.aster.ui.screens.connect

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aster.BuildConfig
import com.aster.data.local.SettingsDataStore
import com.aster.data.model.ConnectionState
import com.aster.data.websocket.AsterWebSocketClient
import com.aster.service.AsterService
import com.aster.ui.components.*
import com.aster.ui.theme.AsterTheme
import com.aster.ui.theme.AsterTypography
import com.aster.ui.theme.TerminalTextStyles
import com.aster.util.PermissionCheckResult
import com.aster.util.PermissionUtils
import com.aster.util.TailscaleUtils
import compose.icons.FeatherIcons
import compose.icons.feathericons.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun ConnectScreen(
    webSocketClient: AsterWebSocketClient,
    settingsDataStore: SettingsDataStore,
    connectionState: ConnectionState,
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AsterTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var serverUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Permission state
    var permissionResult by remember { mutableStateOf(PermissionUtils.checkAllPermissions(context)) }

    // Tailscale status
    var tailscaleStatus by remember { mutableStateOf(TailscaleUtils.getStatus(context)) }

    // Refresh permissions and Tailscale status when returning from settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionResult = PermissionUtils.checkAllPermissions(context)
                tailscaleStatus = TailscaleUtils.getStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Load saved server URL
    LaunchedEffect(Unit) {
        serverUrl = settingsDataStore.serverUrl.first() ?: ""
        isLoading = false
    }

    // Collect errors
    val errors by webSocketClient.errors.collectAsState(initial = null)
    var lastError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(errors) {
        errors?.let { lastError = it }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.terminalBg,
        topBar = {
            // Sticky ASCII header with logo - edge-to-edge to avoid camera cutout
            TerminalPageHeader(
                description = "Android Device Controller",
                showBackButton = false,
                edgeToEdge = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) { innerPadding ->
        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

                // Tailscale Status Panel
                TailscaleStatusPanel(
                    tailscaleStatus = tailscaleStatus,
                    onLaunchTailscale = {
                        if (tailscaleStatus.isInstalled) {
                            TailscaleUtils.launchTailscale(context)
                        } else {
                            TailscaleUtils.openPlayStore(context)
                        }
                    },
                    onRefresh = {
                        tailscaleStatus = TailscaleUtils.getStatus(context)
                    }
                )

                // Compact Permission Warning (shown if permissions missing)
                AnimatedVisibility(
                    visible = !permissionResult.allGranted,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, colors.amber.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .background(colors.amber.copy(alpha = 0.05f))
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.amber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = FeatherIcons.AlertTriangle,
                                    contentDescription = null,
                                    tint = colors.amber,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${permissionResult.missingPermissions.size} Permissions Required",
                                    style = AsterTypography.bodyMedium,
                                    color = colors.amber
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                TerminalButton(
                                    text = "Grant Access",
                                    onClick = onNavigateToPermissions,
                                    variant = ButtonVariant.WARNING,
                                    size = ButtonSize.SMALL
                                )
                            }
                        }
                    }
                }

            // HERO: Enhanced Server Connection Section
            HeroConnectionPanel(
                serverUrl = serverUrl,
                onServerUrlChange = {
                    serverUrl = it
                    lastError = null
                },
                connectionState = connectionState,
                lastError = lastError,
                permissionResult = permissionResult,
                onConnect = {
                    if (serverUrl.isNotBlank() && permissionResult.allGranted) {
                        lastError = null
                        scope.launch {
                            settingsDataStore.saveServerUrl(serverUrl)
                        }
                        AsterService.startService(context, serverUrl)
                    }
                },
                onCancel = {
                    AsterService.stopService(context)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Permissions shortcut (only show if all permissions granted)
            if (permissionResult.allGranted) {
                TerminalButton(
                    text = "Configure Permissions",
                    onClick = onNavigateToPermissions,
                    variant = ButtonVariant.GHOST,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Footer at end of scrollable content
            Spacer(modifier = Modifier.height(48.dp))
            AsterFooter(modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * Hero connection panel with enhanced visual design
 */
@Composable
private fun HeroConnectionPanel(
    serverUrl: String,
    onServerUrlChange: (String) -> Unit,
    connectionState: ConnectionState,
    lastError: String?,
    permissionResult: PermissionCheckResult,
    onConnect: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = AsterTheme.colors

    // Animated glow effect
    val infiniteTransition = rememberInfiniteTransition(label = "hero_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.primary.copy(alpha = 0.4f),
                        colors.terminalBorder
                    )
                ),
                shape = RoundedCornerShape(4.dp)
            )
            .background(colors.terminalSurface.copy(alpha = 0.95f))
            .drawBehind {
                // Animated top glow
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colors.primary.copy(alpha = glowAlpha),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height * 0.3f
                    )
                )
            }
    ) {
        // Header with connection status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            colors.primary.copy(alpha = 0.08f),
                            colors.primary.copy(alpha = 0.03f)
                        )
                    )
                )
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.primary.copy(alpha = 0.15f))
                        .drawBehind {
                            // Inner glow
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        colors.primary.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FeatherIcons.Wifi,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "SERVER CONNECTION",
                        style = TerminalTextStyles.StatLabel,
                        color = colors.primaryDim
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "WebSocket Transport",
                        style = AsterTypography.bodySmall,
                        color = colors.terminalDim
                    )
                }
            }

            // Enhanced status badge with icon
            EnhancedStatusBadge(
                connectionState = connectionState
            )
        }

        // Divider with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            colors.terminalBorder,
                            Color.Transparent
                        )
                    )
                )
        )

        // Main input section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Server address label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.blue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FeatherIcons.Server,
                        contentDescription = null,
                        tint = colors.blue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = "Server Address",
                        style = AsterTypography.bodyMedium,
                        color = colors.terminalText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tailscale IP or hostname of your MCP server",
                        style = AsterTypography.bodySmall,
                        color = colors.terminalDim
                    )
                }
            }

            // Enhanced text field with larger size
            // No prefix - defaults to wss:// for security, ws:// allowed for local
            TerminalTextField(
                value = serverUrl,
                onValueChange = onServerUrlChange,
                placeholder = "100.x.x.x:5987 or hostname:5987",
                keyboardType = KeyboardType.Uri,
                modifier = Modifier.fillMaxWidth()
            )

            // Protocol hint
            Text(
                text = "Defaults to secure connection (wss://). Use ws:// prefix for local networks.",
                style = AsterTypography.labelSmall,
                color = colors.terminalMuted,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Error display
            AnimatedVisibility(
                visible = lastError != null || connectionState == ConnectionState.ERROR,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, colors.rose.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .background(colors.rose.copy(alpha = 0.05f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = FeatherIcons.XCircle,
                        contentDescription = null,
                        tint = colors.rose,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = lastError ?: "Connection error",
                        style = AsterTypography.bodySmall,
                        color = colors.rose,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action buttons with enhanced styling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TerminalButton(
                    text = when {
                        !permissionResult.allGranted -> "Permissions Required"
                        connectionState == ConnectionState.CONNECTING -> "Connecting..."
                        connectionState == ConnectionState.PENDING_APPROVAL -> "Awaiting..."
                        else -> "Establish Connection"
                    },
                    onClick = onConnect,
                    enabled = serverUrl.isNotBlank() &&
                              permissionResult.allGranted &&
                              connectionState != ConnectionState.CONNECTING &&
                              connectionState != ConnectionState.PENDING_APPROVAL,
                    variant = ButtonVariant.PRIMARY,
                    modifier = Modifier.weight(1f)
                )

                if (connectionState == ConnectionState.CONNECTING ||
                    connectionState == ConnectionState.PENDING_APPROVAL) {
                    TerminalButton(
                        text = "Cancel",
                        onClick = onCancel,
                        variant = ButtonVariant.DANGER,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Pending approval state
            AnimatedVisibility(
                visible = connectionState == ConnectionState.PENDING_APPROVAL,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, colors.amber.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .background(colors.amber.copy(alpha = 0.05f))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = colors.amber,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Awaiting approval on Aster Dashboard...",
                        style = AsterTypography.bodyMedium,
                        color = colors.amber,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Check your desktop client to approve this connection",
                        style = AsterTypography.bodySmall,
                        color = colors.terminalDim,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun ConnectionState.toDisplayText(): String {
    return when (this) {
        ConnectionState.DISCONNECTED -> "DISCONNECTED"
        ConnectionState.CONNECTING -> "CONNECTING"
        ConnectionState.CONNECTED -> "CONNECTED"
        ConnectionState.PENDING_APPROVAL -> "PENDING APPROVAL"
        ConnectionState.APPROVED -> "APPROVED"
        ConnectionState.REJECTED -> "REJECTED"
        ConnectionState.ERROR -> "ERROR"
    }
}

private fun ConnectionState.toBadgeType(): BadgeType {
    return when (this) {
        ConnectionState.DISCONNECTED -> BadgeType.MUTED
        ConnectionState.CONNECTING -> BadgeType.AMBER
        ConnectionState.CONNECTED -> BadgeType.CYAN
        ConnectionState.PENDING_APPROVAL -> BadgeType.AMBER
        ConnectionState.APPROVED -> BadgeType.EMERALD
        ConnectionState.REJECTED -> BadgeType.ROSE
        ConnectionState.ERROR -> BadgeType.ROSE
    }
}

/**
 * Beautiful terminal-styled footer with version and credits
 */
@Composable
private fun AsterFooter(modifier: Modifier = Modifier) {
    val colors = AsterTheme.colors

    Column(
        modifier = modifier
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Decorative divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                colors.terminalBorder
                            )
                        )
                    )
            )
            Text(
                text = "◆",
                style = AsterTypography.labelSmall,
                color = colors.primary.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                colors.terminalBorder,
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Version
        Text(
            text = "ASTER v${BuildConfig.VERSION_NAME}",
            style = TerminalTextStyles.StatLabel,
            color = colors.primary
        )

        // Made with love message
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Built with",
                style = AsterTypography.bodySmall,
                color = colors.terminalDim
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "⚡",
                style = AsterTypography.bodySmall,
                color = colors.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "for Android automation",
                style = AsterTypography.bodySmall,
                color = colors.terminalDim
            )
        }

        // Copyright
        Text(
            text = "© 2026 • Open Source",
            style = AsterTypography.labelSmall,
            color = colors.terminalMuted
        )
    }
}

/**
 * Enhanced status badge with icon and animation
 */
@Composable
private fun EnhancedStatusBadge(connectionState: ConnectionState) {
    val colors = AsterTheme.colors

    val (statusColor, statusIcon) = when (connectionState) {
        ConnectionState.DISCONNECTED -> colors.rose to FeatherIcons.WifiOff
        ConnectionState.CONNECTING -> colors.amber to FeatherIcons.RefreshCw
        ConnectionState.CONNECTED -> colors.primary to FeatherIcons.Wifi
        ConnectionState.PENDING_APPROVAL -> colors.amber to FeatherIcons.Clock
        ConnectionState.APPROVED -> colors.emerald to FeatherIcons.CheckCircle
        ConnectionState.REJECTED -> colors.rose to FeatherIcons.XCircle
        ConnectionState.ERROR -> colors.rose to FeatherIcons.AlertCircle
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(
                width = 1.5.dp,
                color = statusColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(6.dp)
            )
            .background(statusColor.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = statusIcon,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = connectionState.toDisplayText(),
            style = TerminalTextStyles.Badge,
            color = statusColor
        )
    }
}

/**
 * Tailscale status panel showing VPN connection status
 */
@Composable
private fun TailscaleStatusPanel(
    tailscaleStatus: TailscaleUtils.TailscaleStatus,
    onLaunchTailscale: () -> Unit,
    onRefresh: () -> Unit
) {
    val colors = AsterTheme.colors

    // Determine status colors and icon
    val (statusColor, statusIcon, statusText) = when {
        !tailscaleStatus.isInstalled -> Triple(colors.rose, FeatherIcons.Download, "NOT INSTALLED")
        !tailscaleStatus.isVpnActive -> Triple(colors.amber, FeatherIcons.WifiOff, "VPN INACTIVE")
        else -> Triple(colors.emerald, FeatherIcons.Shield, "CONNECTED")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .background(statusColor.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "TAILSCALE",
                            style = TerminalTextStyles.StatLabel,
                            color = statusColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = statusText,
                            style = AsterTypography.bodySmall,
                            color = colors.terminalDim
                        )
                    }
                }

                // Refresh button
                TerminalIconButton(
                    icon = FeatherIcons.RefreshCw,
                    text = "Refresh",
                    onClick = onRefresh,
                    variant = ButtonVariant.GHOST
                )
            }

            // Show Tailscale IP if connected
            if (tailscaleStatus.isReady && tailscaleStatus.tailscaleIp != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.terminalSurface.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = FeatherIcons.Globe,
                        contentDescription = null,
                        tint = colors.emerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Tailscale IP:",
                        style = AsterTypography.bodySmall,
                        color = colors.terminalDim
                    )
                    Text(
                        text = tailscaleStatus.tailscaleIp,
                        style = AsterTypography.bodyMedium,
                        color = colors.emerald
                    )
                }
            }

            // Action button if not ready
            if (!tailscaleStatus.isReady) {
                Spacer(modifier = Modifier.height(12.dp))
                TerminalButton(
                    text = if (tailscaleStatus.isInstalled) "Open Tailscale" else "Install Tailscale",
                    onClick = onLaunchTailscale,
                    variant = if (tailscaleStatus.isInstalled) ButtonVariant.WARNING else ButtonVariant.PRIMARY,
                    size = ButtonSize.SMALL,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!tailscaleStatus.isInstalled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tailscale is required for secure remote connections",
                        style = AsterTypography.bodySmall,
                        color = colors.terminalDim,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enable Tailscale VPN to connect securely",
                        style = AsterTypography.bodySmall,
                        color = colors.terminalDim,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
