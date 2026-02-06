package com.aster.ui.screens.permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aster.ui.components.*
import com.aster.ui.theme.AsterTheme
import com.aster.ui.theme.AsterTypography
import com.aster.ui.theme.TerminalTextStyles
import com.aster.util.PermissionType
import com.aster.util.PermissionUtils
import compose.icons.FeatherIcons
import compose.icons.feathericons.*

@Composable
fun PermissionsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AsterTheme.colors
    val context = LocalContext.current

    // Permission states using shared utility
    var permissionResult by remember { mutableStateOf(PermissionUtils.checkAllPermissions(context)) }

    // Refresh permissions when returning from settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionResult = PermissionUtils.checkAllPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Runtime permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        permissionResult = PermissionUtils.checkAllPermissions(context)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.terminalBg,
        topBar = {
            // Sticky header - full width, edge-to-edge
            TerminalDetailHeader(
                title = "Permissions",
                subtitle = "System Access Configuration",
                showBackButton = true,
                onBackClick = onNavigateBack,
                statusBadge = {
                    StatusBadge(
                        text = if (permissionResult.allGranted) "ALL GRANTED" else "${permissionResult.grantedCount}/${permissionResult.totalCount}",
                        type = if (permissionResult.allGranted) BadgeType.EMERALD else BadgeType.AMBER
                    )
                },
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
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp)
        ) {

        // Progress stat cards row - using IntrinsicSize to ensure equal height
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Granted card
            PermissionStatCard(
                label = "GRANTED",
                value = "${permissionResult.grantedCount}",
                icon = FeatherIcons.Check,
                accentColor = colors.emerald,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            // Remaining card
            PermissionStatCard(
                label = "REMAINING",
                value = "${permissionResult.totalCount - permissionResult.grantedCount}",
                icon = if (permissionResult.allGranted) FeatherIcons.Check else FeatherIcons.AlertTriangle,
                accentColor = if (permissionResult.allGranted) colors.primary else colors.amber,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Core Permissions Section
        SectionHeader(
            title = "Core Permissions",
            modifier = Modifier.padding(bottom = 12.dp)
        )

        TerminalPanel {
            PermissionItem(
                name = "Location Access",
                description = "Required for GPS location tracking",
                icon = FeatherIcons.MapPin,
                iconColor = colors.blue,
                isGranted = permissionResult.permissions[PermissionType.LOCATION] == true,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
            )

            TerminalDivider()

            PermissionItem(
                name = "Notifications",
                description = "Required to post and read notifications",
                icon = FeatherIcons.Bell,
                iconColor = colors.amber,
                isGranted = permissionResult.permissions[PermissionType.NOTIFICATIONS] == true,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                    }
                }
            )

            TerminalDivider()

            PermissionItem(
                name = "Phone & SMS",
                description = "Required for call and SMS features",
                icon = FeatherIcons.Phone,
                iconColor = colors.emerald,
                isGranted = permissionResult.permissions[PermissionType.PHONE_SMS] == true,
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_SMS,
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.CALL_PHONE,
                            Manifest.permission.READ_PHONE_STATE
                        )
                    )
                }
            )

            TerminalDivider()

            PermissionItem(
                name = "Camera",
                description = "Required for photo and video capture",
                icon = FeatherIcons.Camera,
                iconColor = colors.violet,
                isGranted = permissionResult.permissions[PermissionType.CAMERA] == true,
                onClick = {
                    permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                }
            )

            TerminalDivider()

            PermissionItem(
                name = "Contacts",
                description = "Required to search and read contacts",
                icon = FeatherIcons.Users,
                iconColor = colors.blue,
                isGranted = permissionResult.permissions[PermissionType.CONTACTS] == true,
                onClick = {
                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Special Permissions Section
        SectionHeader(
            title = "Special Access",
            modifier = Modifier.padding(bottom = 12.dp)
        )

        TerminalPanel {
            PermissionItem(
                name = "Accessibility Service",
                description = "Required for screen control and gestures",
                icon = FeatherIcons.Crosshair,
                iconColor = colors.violet,
                isGranted = permissionResult.permissions[PermissionType.ACCESSIBILITY] == true,
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )

            TerminalDivider()

            PermissionItem(
                name = "Notification Listener",
                description = "Required to read incoming notifications",
                icon = FeatherIcons.Eye,
                iconColor = colors.primary,
                isGranted = permissionResult.permissions[PermissionType.NOTIFICATION_LISTENER] == true,
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            )

            TerminalDivider()

            PermissionItem(
                name = "Display Over Apps",
                description = "Required for system overlay windows",
                icon = FeatherIcons.Layers,
                iconColor = colors.indigo,
                isGranted = permissionResult.permissions[PermissionType.OVERLAY] == true,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                }
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                TerminalDivider()

                PermissionItem(
                    name = "All Files Access",
                    description = "Required for full file system access",
                    icon = FeatherIcons.Folder,
                    iconColor = colors.rose,
                    isGranted = permissionResult.permissions[PermissionType.STORAGE] == true,
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                )
            }

            TerminalDivider()

            PermissionItem(
                name = "Battery Optimization",
                description = "Disable to prevent service interruption",
                icon = FeatherIcons.Battery,
                iconColor = colors.emerald,
                isGranted = permissionResult.permissions[PermissionType.BATTERY] == true,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
                .border(1.dp, colors.primary.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                .background(colors.primary.copy(alpha = 0.05f))
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = FeatherIcons.Info,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Tap each permission to grant access. Some permissions require manual enabling in system settings.",
                style = AsterTypography.bodySmall,
                color = colors.terminalMuted
            )
        }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            Text(
                text = "// ASTER v1.0.0",
                style = AsterTypography.labelSmall,
                color = colors.terminalDim,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Custom stat card for permissions screen with consistent sizing
 */
@Composable
private fun PermissionStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = AsterTheme.colors

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .border(1.dp, colors.terminalBorder, RoundedCornerShape(2.dp))
            .background(colors.terminalSurface.copy(alpha = 0.95f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon container with fixed size
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // Value
                Text(
                    text = value,
                    style = TerminalTextStyles.StatValue,
                    color = accentColor
                )

                // Label with // prefix
                Text(
                    text = "// ${label.uppercase()}",
                    style = TerminalTextStyles.StatLabel,
                    color = accentColor.copy(alpha = 0.7f)
                )
            }
        }

        // Accent bar at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(accentColor)
        )
    }
}

@Composable
private fun PermissionItem(
    name: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    val colors = AsterTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with color background
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = AsterTypography.bodyMedium,
                color = colors.terminalText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = AsterTypography.bodySmall,
                color = colors.terminalDim
            )
        }

        // Status badge
        StatusBadge(
            text = if (isGranted) "GRANTED" else "REQUIRED",
            type = if (isGranted) BadgeType.EMERALD else BadgeType.AMBER
        )
    }
}
