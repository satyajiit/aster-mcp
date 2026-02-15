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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aster.ui.components.AsterButton
import com.aster.ui.components.AsterButtonVariant
import com.aster.ui.components.AsterCard
import com.aster.ui.components.AsterSectionHeader
import com.aster.ui.components.AsterTopBar
import com.aster.ui.components.StatusBadge
import com.aster.ui.theme.AsterTheme
import com.aster.util.PermissionType
import com.aster.util.PermissionUtils
import compose.icons.FeatherIcons
import compose.icons.feathericons.Battery
import compose.icons.feathericons.Bell
import compose.icons.feathericons.Camera
import compose.icons.feathericons.Crosshair
import compose.icons.feathericons.Eye
import compose.icons.feathericons.Folder
import compose.icons.feathericons.Info
import compose.icons.feathericons.Layers
import compose.icons.feathericons.MapPin
import compose.icons.feathericons.Phone
import compose.icons.feathericons.Users

@Composable
fun PermissionsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AsterTheme.colors
    val context = LocalContext.current

    // Permission states
    var permissionResult by remember { mutableStateOf(PermissionUtils.checkAllPermissions(context)) }

    // Refresh when returning from system settings
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

    // Check permissions on initial composition
    LaunchedEffect(Unit) {
        permissionResult = PermissionUtils.checkAllPermissions(context)
    }

    // Runtime permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        permissionResult = PermissionUtils.checkAllPermissions(context)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            AsterTopBar(
                title = "Permissions",
                onBack = onNavigateBack,
                actions = {
                    // Summary badge in top bar
                    val badgeColor =
                        if (permissionResult.allGranted) colors.success else colors.warning
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.10f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (permissionResult.allGranted) "All Granted" else "${permissionResult.grantedCount}/${permissionResult.totalCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Progress summary
            AsterCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${permissionResult.grantedCount} of ${permissionResult.totalCount}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (permissionResult.allGranted) colors.success else colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (permissionResult.allGranted) "All permissions granted" else "Permissions granted",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSubtle
                        )
                    }

                    // Visual progress indicator
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (permissionResult.allGranted) colors.success.copy(alpha = 0.12f)
                                else colors.warning.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (permissionResult.allGranted) "OK" else "${permissionResult.totalCount - permissionResult.grantedCount}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (permissionResult.allGranted) colors.success else colors.warning,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // =========================================================
            // RUNTIME PERMISSIONS
            // =========================================================
            AsterSectionHeader(
                label = "Runtime Permissions",
                count = listOf(
                    PermissionType.NOTIFICATIONS,
                    PermissionType.LOCATION,
                    PermissionType.PHONE_SMS,
                    PermissionType.CONTACTS,
                    PermissionType.CAMERA
                ).count { permissionResult.permissions[it] == true }
            )

            AsterCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    PermissionItem(
                        icon = FeatherIcons.Bell,
                        name = "Notifications",
                        description = "Post and manage notifications",
                        isGranted = permissionResult.permissions[PermissionType.NOTIFICATIONS] == true,
                        accentColor = colors.warning,
                        onGrant = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(
                                    arrayOf(Manifest.permission.POST_NOTIFICATIONS)
                                )
                            }
                        }
                    )

                    PermissionDivider()

                    PermissionItem(
                        icon = FeatherIcons.MapPin,
                        name = "Location",
                        description = "Access GPS and network location",
                        isGranted = permissionResult.permissions[PermissionType.LOCATION] == true,
                        accentColor = colors.info,
                        onGrant = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    )

                    PermissionDivider()

                    PermissionItem(
                        icon = FeatherIcons.Phone,
                        name = "Phone & SMS",
                        description = "Read/send SMS and make calls",
                        isGranted = permissionResult.permissions[PermissionType.PHONE_SMS] == true,
                        accentColor = colors.success,
                        onGrant = {
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

                    PermissionDivider()

                    PermissionItem(
                        icon = FeatherIcons.Users,
                        name = "Contacts",
                        description = "Search and read device contacts",
                        isGranted = permissionResult.permissions[PermissionType.CONTACTS] == true,
                        accentColor = colors.info,
                        onGrant = {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.READ_CONTACTS)
                            )
                        }
                    )

                    PermissionDivider()

                    PermissionItem(
                        icon = FeatherIcons.Camera,
                        name = "Camera",
                        description = "Capture photos and video",
                        isGranted = permissionResult.permissions[PermissionType.CAMERA] == true,
                        accentColor = colors.accent,
                        onGrant = {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.CAMERA)
                            )
                        }
                    )
                }
            }

            // =========================================================
            // SPECIAL PERMISSIONS
            // =========================================================
            AsterSectionHeader(
                label = "Special Access",
                count = listOf(
                    PermissionType.STORAGE,
                    PermissionType.ACCESSIBILITY,
                    PermissionType.NOTIFICATION_LISTENER,
                    PermissionType.OVERLAY,
                    PermissionType.BATTERY
                ).count { permissionResult.permissions[it] == true }
            )

            AsterCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    PermissionItem(
                        icon = FeatherIcons.Folder,
                        name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "All Files Access" else "Storage",
                        description = "Full file system read/write access",
                        isGranted = permissionResult.permissions[PermissionType.STORAGE] == true,
                        accentColor = colors.error,
                        onGrant = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.READ_EXTERNAL_STORAGE
                                    )
                                )
                            }
                        }
                    )

                    PermissionDivider()

                    PermissionItem(
                        icon = FeatherIcons.Crosshair,
                        name = "Accessibility Service",
                        description = "Screen control, gestures, and UI automation",
                        isGranted = permissionResult.permissions[PermissionType.ACCESSIBILITY] == true,
                        accentColor = colors.info,
                        onGrant = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    )

                    PermissionDivider()

                    PermissionItem(
                        icon = FeatherIcons.Eye,
                        name = "Notification Listener",
                        description = "Read and intercept incoming notifications",
                        isGranted = permissionResult.permissions[PermissionType.NOTIFICATION_LISTENER] == true,
                        accentColor = colors.primary,
                        onGrant = {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            )
                        }
                    )

                    PermissionDivider()

                    PermissionItem(
                        icon = FeatherIcons.Layers,
                        name = "Display Over Apps",
                        description = "Show system overlay windows",
                        isGranted = permissionResult.permissions[PermissionType.OVERLAY] == true,
                        accentColor = colors.accent,
                        onGrant = {
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

                    PermissionDivider()

                    PermissionItem(
                        icon = FeatherIcons.Battery,
                        name = "Battery Optimization",
                        description = "Exempt from battery optimization to prevent service interruption",
                        isGranted = permissionResult.permissions[PermissionType.BATTERY] == true,
                        accentColor = colors.success,
                        onGrant = {
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
            }

            // Info notice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, colors.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .background(colors.primary.copy(alpha = 0.05f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = FeatherIcons.Info,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Tap each permission to grant access. Some permissions require manual enabling in system settings and cannot be requested directly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSubtle,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// =============================================================================
// PERMISSION ITEM
// =============================================================================

@Composable
private fun PermissionItem(
    icon: ImageVector,
    name: String,
    description: String,
    isGranted: Boolean,
    accentColor: Color,
    onGrant: () -> Unit
) {
    val colors = AsterTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
        }

        // Status and action
        if (isGranted) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    color = colors.success
                )
                Text(
                    text = "Granted",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.success,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            AsterButton(
                onClick = onGrant,
                text = "Grant",
                variant = AsterButtonVariant.SECONDARY
            )
        }
    }
}

// =============================================================================
// PERMISSION DIVIDER
// =============================================================================

@Composable
private fun PermissionDivider() {
    val colors = AsterTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.border.copy(alpha = 0.5f))
    )
}
