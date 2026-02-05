package com.aster.ui.screens.status

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aster.data.model.ConnectionState
import com.aster.data.websocket.AsterWebSocketClient
import com.aster.service.AsterService
import com.aster.ui.components.*
import com.aster.ui.theme.AsterTheme
import com.aster.ui.theme.AsterTypography
import com.aster.ui.theme.TerminalTextStyles
import com.aster.util.PermissionUtils
import compose.icons.FeatherIcons
import compose.icons.feathericons.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatusScreen(
    webSocketClient: AsterWebSocketClient,
    connectionState: ConnectionState,
    onDisconnect: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AsterTheme.colors
    val context = LocalContext.current

    // Service status states
    var isAccessibilityEnabled by remember { mutableStateOf(PermissionUtils.checkAccessibilityPermission(context)) }
    var isNotificationListenerEnabled by remember { mutableStateOf(PermissionUtils.checkNotificationListenerPermission(context)) }
    var isForegroundServiceRunning by remember { mutableStateOf(AsterService.isRunning) }

    // Refresh service states when returning from settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = PermissionUtils.checkAccessibilityPermission(context)
                isNotificationListenerEnabled = PermissionUtils.checkNotificationListenerPermission(context)
                isForegroundServiceRunning = AsterService.isRunning
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Uptime counter
    var uptimeSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.APPROVED) {
            uptimeSeconds = 0
            while (true) {
                delay(1000)
                uptimeSeconds++
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.terminalBg,
        topBar = {
            // Use TerminalPageHeader like ConnectScreen
            TerminalPageHeader(
                description = "Device Control Panel",
                edgeToEdge = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 24.dp)
        ) {

            // Stats Grid - responsive layout
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompactStatCard(
                        label = "UPTIME",
                        value = formatUptime(uptimeSeconds),
                        icon = FeatherIcons.Clock,
                        accentColor = colors.primary,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    CompactStatCard(
                        label = "STATUS",
                        value = if (connectionState == ConnectionState.APPROVED) "LIVE" else "IDLE",
                        icon = if (connectionState == ConnectionState.APPROVED) FeatherIcons.Activity else FeatherIcons.Circle,
                        accentColor = if (connectionState == ConnectionState.APPROVED) colors.emerald else colors.amber,
                        animated = connectionState == ConnectionState.APPROVED,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
                CompactStatCard(
                    label = "SERVICES",
                    value = "${listOf(isForegroundServiceRunning, isAccessibilityEnabled, isNotificationListenerEnabled).count { it }}/3 ACTIVE",
                    icon = FeatherIcons.Zap,
                    accentColor = colors.blue,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Device Info Section
            SectionHeader(
                title = "Device Info",
                modifier = Modifier.padding(bottom = 10.dp)
            )
            TerminalPanel {
                CompactInfoRow(
                    label = "MODEL",
                    value = "${Build.MANUFACTURER} ${Build.MODEL}",
                    icon = FeatherIcons.Smartphone,
                    iconColor = colors.blue
                )
                TerminalDivider()
                CompactInfoRow(
                    label = "ANDROID",
                    value = "API ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})",
                    icon = FeatherIcons.Cpu,
                    iconColor = colors.emerald
                )
                TerminalDivider()
                CompactInfoRow(
                    label = "DEVICE ID",
                    value = getDeviceId(context),
                    icon = FeatherIcons.Hash,
                    iconColor = colors.violet
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Connection Section
            SectionHeader(
                title = "Connection",
                modifier = Modifier.padding(bottom = 10.dp)
            )
            TerminalPanel {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FeatherIcons.Wifi,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "STATUS",
                            style = AsterTypography.bodySmall,
                            color = colors.terminalDim
                        )
                    }
                    StatusBadge(
                        text = connectionState.toDisplayText(),
                        type = connectionState.toBadgeType()
                    )
                }
                TerminalDivider()
                CompactInfoRow(
                    label = "PROTOCOL",
                    value = "WebSocket (wss://)",
                    icon = FeatherIcons.Globe,
                    iconColor = colors.blue
                )
                TerminalDivider()
                CompactInfoRow(
                    label = "HEARTBEAT",
                    value = "30s interval",
                    icon = FeatherIcons.Heart,
                    iconColor = colors.rose
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Services Section
            SectionHeader(
                title = "Services",
                modifier = Modifier.padding(bottom = 10.dp)
            )

            TerminalPanel {
                CompactServiceRow(
                    name = "Foreground Service",
                    icon = FeatherIcons.Server,
                    iconColor = colors.primary,
                    isActive = isForegroundServiceRunning
                )
                TerminalDivider()
                CompactServiceRow(
                    name = "Accessibility Service",
                    icon = FeatherIcons.Crosshair,
                    iconColor = colors.violet,
                    isActive = isAccessibilityEnabled
                )
                TerminalDivider()
                CompactServiceRow(
                    name = "Notification Listener",
                    icon = FeatherIcons.Eye,
                    iconColor = colors.indigo,
                    isActive = isNotificationListenerEnabled
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TerminalButton(
                    text = "Permissions",
                    onClick = onNavigateToPermissions,
                    variant = ButtonVariant.GHOST,
                    modifier = Modifier.weight(1f)
                )
                TerminalButton(
                    text = "Disconnect",
                    onClick = onDisconnect,
                    variant = ButtonVariant.DANGER,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Activity Log Section
            SectionHeader(
                title = "Activity Log",
                modifier = Modifier.padding(bottom = 10.dp)
            )

            TerminalPanel(
                modifier = Modifier.heightIn(min = 160.dp)
            ) {
                PromptLine(
                    command = "aster --status",
                    path = "~"
                )
                Spacer(modifier = Modifier.height(10.dp))

                LogEntry(
                    time = getCurrentTime(),
                    level = LogLevel.INFO,
                    message = "Connected to MCP server"
                )
                LogEntry(
                    time = getCurrentTime(),
                    level = LogLevel.INFO,
                    message = "Device approved for control"
                )
                LogEntry(
                    time = getCurrentTime(),
                    level = LogLevel.DEBUG,
                    message = "Heartbeat sent"
                )

                Spacer(modifier = Modifier.height(10.dp))

                PromptLine(
                    command = "",
                    path = "~",
                    showCursor = true
                )
            }
        }
    }
}

/**
 * Compact stat card with animation support
 */
@Composable
private fun CompactStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    animated: Boolean = false
) {
    val colors = AsterTheme.colors

    // Pulse animation for live status
    val infiniteTransition = rememberInfiniteTransition(label = "stat_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, colors.terminalBorder, RoundedCornerShape(4.dp))
            .background(colors.terminalSurface.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = if (animated) pulseAlpha else 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = value,
                        style = TerminalTextStyles.StatValue,
                        color = accentColor
                    )
                    Text(
                        text = "// ${label.uppercase()}",
                        style = TerminalTextStyles.StatLabel,
                        color = colors.terminalDim
                    )
                }
            }
        }

        // Top accent line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(accentColor.copy(alpha = 0.6f))
                .align(Alignment.TopCenter)
        )
    }
}

/**
 * Compact info row for denser layouts
 */
@Composable
private fun CompactInfoRow(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color
) {
    val colors = AsterTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(14.dp)
            )
        }

        Text(
            text = label,
            style = AsterTypography.bodySmall,
            color = colors.terminalDim,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            style = AsterTypography.bodySmall,
            color = colors.terminalText
        )
    }
}

/**
 * Compact service row
 */
@Composable
private fun CompactServiceRow(
    name: String,
    icon: ImageVector,
    iconColor: Color,
    isActive: Boolean
) {
    val colors = AsterTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(14.dp)
            )
        }

        Text(
            text = name,
            style = AsterTypography.bodySmall,
            color = colors.terminalText,
            modifier = Modifier.weight(1f)
        )

        StatusBadge(
            text = if (isActive) "ACTIVE" else "INACTIVE",
            type = if (isActive) BadgeType.EMERALD else BadgeType.MUTED
        )
    }
}

private fun formatUptime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, secs)
}

private fun getCurrentTime(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
}

private fun getDeviceId(context: android.content.Context): String {
    return android.provider.Settings.Secure.getString(
        context.contentResolver,
        android.provider.Settings.Secure.ANDROID_ID
    )?.take(12) ?: "unknown"
}

private fun ConnectionState.toDisplayText(): String {
    return when (this) {
        ConnectionState.DISCONNECTED -> "DISCONNECTED"
        ConnectionState.CONNECTING -> "CONNECTING"
        ConnectionState.CONNECTED -> "CONNECTED"
        ConnectionState.PENDING_APPROVAL -> "PENDING"
        ConnectionState.APPROVED -> "APPROVED"
        ConnectionState.REJECTED -> "REJECTED"
        ConnectionState.ERROR -> "ERROR"
    }
}

private fun ConnectionState.toBadgeType(): BadgeType {
    return when (this) {
        ConnectionState.DISCONNECTED -> BadgeType.MUTED
        ConnectionState.CONNECTING -> BadgeType.AMBER
        ConnectionState.CONNECTED -> BadgeType.AMBER
        ConnectionState.PENDING_APPROVAL -> BadgeType.AMBER
        ConnectionState.APPROVED -> BadgeType.EMERALD
        ConnectionState.REJECTED -> BadgeType.ROSE
        ConnectionState.ERROR -> BadgeType.ROSE
    }
}
