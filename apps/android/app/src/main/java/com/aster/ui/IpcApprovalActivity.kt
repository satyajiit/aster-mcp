package com.aster.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aster.service.AsterService
import com.aster.service.mode.IpcMode
import com.aster.service.mode.ModeState
import com.aster.service.mode.ModeType
import com.aster.ui.components.AsterButton
import com.aster.ui.components.AsterButtonVariant
import com.aster.ui.components.StatusBadge
import com.aster.ui.theme.AsterTheme
import com.aster.util.PermissionCheckResult
import com.aster.util.PermissionType
import com.aster.util.PermissionUtils
import compose.icons.FeatherIcons
import compose.icons.feathericons.Battery
import compose.icons.feathericons.Bell
import compose.icons.feathericons.Camera
import compose.icons.feathericons.Crosshair
import compose.icons.feathericons.Eye
import compose.icons.feathericons.Folder
import compose.icons.feathericons.Layers
import compose.icons.feathericons.MapPin
import compose.icons.feathericons.Phone
import compose.icons.feathericons.Users
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@AndroidEntryPoint
class IpcApprovalActivity : ComponentActivity() {

    companion object {
        private const val TAG = "IpcApprovalActivity"
    }

    @Inject
    lateinit var ipcMode: IpcMode

    private var showSheet by mutableStateOf(true)
    private var showPermissionSheet by mutableStateOf(false)
    private var permissionResult by mutableStateOf<PermissionCheckResult?>(null)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val callingPkg = callingActivity?.packageName ?: callingPackage
        val (appName, appIcon) = resolveCallerInfo(callingPkg)
        val iconBitmap = appIcon?.toBitmap()

        // Initial permission check
        permissionResult = PermissionUtils.checkAllPermissions(this)

        setContent {
            AsterTheme {
                val approvalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val scope = rememberCoroutineScope()

                // Refresh permissions when returning from system settings
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            permissionResult = PermissionUtils.checkAllPermissions(this@IpcApprovalActivity)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                val runtimePermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { _ ->
                    permissionResult = PermissionUtils.checkAllPermissions(this@IpcApprovalActivity)
                }

                // Approval sheet
                if (showSheet && !showPermissionSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { deny() },
                        sheetState = approvalSheetState,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (iconBitmap != null) {
                                    Image(
                                        painter = BitmapPainter(iconBitmap.asImageBitmap()),
                                        contentDescription = appName,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                }
                                Text(
                                    text = appName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = "wants to connect via IPC",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "This will allow the app to use Aster Tools on this device, including device control, file access, and other capabilities.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(28.dp))

                            Button(
                                onClick = {
                                    val result = PermissionUtils.checkAllPermissions(this@IpcApprovalActivity)
                                    permissionResult = result
                                    if (!result.allGranted) {
                                        showPermissionSheet = true
                                    } else {
                                        scope.launch { approve() }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = "Approve",
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { deny() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Deny",
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Permissions panel — inline overlay (not Dialog) so activity insets work
                if (showPermissionSheet) {
                    val currentResult = permissionResult
                    val colors = AsterTheme.colors

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Surface(
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 20.dp)
                                    .padding(top = 20.dp),
                            ) {
                                // Fixed header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Permissions Required",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = colors.text
                                    )
                                    if (currentResult != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(colors.warning.copy(alpha = 0.10f))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${currentResult.grantedCount}/${currentResult.totalCount}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.warning,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = "Aster Tools needs these permissions to provide full device control capabilities.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSubtle
                                )

                                Spacer(Modifier.height(16.dp))

                                // Scrollable permissions list
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f, fill = false)
                                        .verticalScroll(rememberScrollState()),
                                ) {
                                    // Runtime permissions
                                    Text(
                                        text = "RUNTIME",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textMuted,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    PermissionRow(
                                        icon = FeatherIcons.Bell,
                                        name = "Notifications",
                                        description = "Post and manage notifications",
                                        isGranted = currentResult?.permissions?.get(PermissionType.NOTIFICATIONS) == true,
                                        accentColor = colors.warning,
                                        onGrant = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                runtimePermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                                            }
                                        }
                                    )
                                    PermissionRow(
                                        icon = FeatherIcons.MapPin,
                                        name = "Location",
                                        description = "Access GPS and network location",
                                        isGranted = currentResult?.permissions?.get(PermissionType.LOCATION) == true,
                                        accentColor = colors.info,
                                        onGrant = {
                                            runtimePermissionLauncher.launch(arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            ))
                                        }
                                    )
                                    PermissionRow(
                                        icon = FeatherIcons.Phone,
                                        name = "Phone & SMS",
                                        description = "Read/send SMS and make calls",
                                        isGranted = currentResult?.permissions?.get(PermissionType.PHONE_SMS) == true,
                                        accentColor = colors.success,
                                        onGrant = {
                                            runtimePermissionLauncher.launch(arrayOf(
                                                Manifest.permission.READ_SMS,
                                                Manifest.permission.SEND_SMS,
                                                Manifest.permission.RECEIVE_SMS,
                                                Manifest.permission.CALL_PHONE,
                                                Manifest.permission.READ_PHONE_STATE
                                            ))
                                        }
                                    )
                                    PermissionRow(
                                        icon = FeatherIcons.Users,
                                        name = "Contacts",
                                        description = "Search and read device contacts",
                                        isGranted = currentResult?.permissions?.get(PermissionType.CONTACTS) == true,
                                        accentColor = colors.info,
                                        onGrant = {
                                            runtimePermissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
                                        }
                                    )
                                    PermissionRow(
                                        icon = FeatherIcons.Camera,
                                        name = "Camera",
                                        description = "Capture photos and video",
                                        isGranted = currentResult?.permissions?.get(PermissionType.CAMERA) == true,
                                        accentColor = colors.accent,
                                        onGrant = {
                                            runtimePermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                                        }
                                    )

                                    Spacer(Modifier.height(16.dp))

                                    // Special access permissions
                                    Text(
                                        text = "SPECIAL ACCESS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textMuted,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    PermissionRow(
                                        icon = FeatherIcons.Folder,
                                        name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "All Files Access" else "Storage",
                                        description = "Full file system read/write access",
                                        isGranted = currentResult?.permissions?.get(PermissionType.STORAGE) == true,
                                        accentColor = colors.error,
                                        onGrant = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                                startActivity(Intent(
                                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                                    Uri.parse("package:$packageName")
                                                ))
                                            } else {
                                                runtimePermissionLauncher.launch(arrayOf(
                                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                                ))
                                            }
                                        }
                                    )
                                    PermissionRow(
                                        icon = FeatherIcons.Crosshair,
                                        name = "Accessibility Service",
                                        description = "Screen control, gestures, and UI automation",
                                        isGranted = currentResult?.permissions?.get(PermissionType.ACCESSIBILITY) == true,
                                        accentColor = colors.info,
                                        onGrant = {
                                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                        }
                                    )
                                    PermissionRow(
                                        icon = FeatherIcons.Eye,
                                        name = "Notification Listener",
                                        description = "Read and intercept incoming notifications",
                                        isGranted = currentResult?.permissions?.get(PermissionType.NOTIFICATION_LISTENER) == true,
                                        accentColor = colors.primary,
                                        onGrant = {
                                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                        }
                                    )
                                    PermissionRow(
                                        icon = FeatherIcons.Layers,
                                        name = "Display Over Apps",
                                        description = "Show system overlay windows",
                                        isGranted = currentResult?.permissions?.get(PermissionType.OVERLAY) == true,
                                        accentColor = colors.accent,
                                        onGrant = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                startActivity(Intent(
                                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                    Uri.parse("package:$packageName")
                                                ))
                                            }
                                        }
                                    )
                                    PermissionRow(
                                        icon = FeatherIcons.Battery,
                                        name = "Battery Optimization",
                                        description = "Prevent service interruption",
                                        isGranted = currentResult?.permissions?.get(PermissionType.BATTERY) == true,
                                        accentColor = colors.success,
                                        onGrant = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                startActivity(Intent(
                                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                                    Uri.parse("package:$packageName")
                                                ))
                                            }
                                        }
                                    )
                                }

                                Spacer(Modifier.height(20.dp))

                                // Fixed footer
                                Button(
                                    onClick = {
                                        val result = PermissionUtils.checkAllPermissions(this@IpcApprovalActivity)
                                        permissionResult = result
                                        if (result.allGranted) {
                                            showPermissionSheet = false
                                            scope.launch { approve() }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    enabled = currentResult?.allGranted == true
                                ) {
                                    Text(
                                        text = if (currentResult?.allGranted == true) "Continue" else "Grant All Permissions First",
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = { showPermissionSheet = false },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Back",
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun approve() {
        try {
            val token: String

            if (AsterService.activeModeTypes.contains(ModeType.IPC)) {
                val existing = ipcMode.token
                token = if (existing.isEmpty()) {
                    ipcMode.generateToken()
                } else {
                    existing
                }
                Log.i(TAG, "IPC already running, reusing token")
            } else {
                token = ipcMode.generateToken()
                AsterService.startService(this, ModeType.IPC, token)

                val started = withTimeoutOrNull(5000L) {
                    ipcMode.statusFlow.filter { it.state == ModeState.RUNNING }.first()
                }
                if (started == null) {
                    Log.w(TAG, "IPC mode did not reach RUNNING state within timeout")
                }
            }

            setResult(Activity.RESULT_OK, Intent().putExtra("token", token))
        } catch (e: Exception) {
            Log.e(TAG, "Approval failed", e)
            setResult(Activity.RESULT_CANCELED)
        }

        showSheet = false
        finish()
    }

    private fun deny() {
        setResult(Activity.RESULT_CANCELED)
        showSheet = false
        finish()
    }

    private fun resolveCallerInfo(packageName: String?): Pair<String, Drawable?> {
        if (packageName == null) return "Unknown App" to null
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val name = pm.getApplicationLabel(appInfo).toString()
            val icon = pm.getApplicationIcon(appInfo)
            name to icon
        } catch (e: PackageManager.NameNotFoundException) {
            packageName to null
        }
    }

    private fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable && bitmap != null) return bitmap
        val bmp = Bitmap.createBitmap(
            intrinsicWidth.coerceAtLeast(1),
            intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bmp)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bmp
    }
}

@Composable
private fun PermissionRow(
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
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
        }

        if (isGranted) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(color = colors.success)
                Text(
                    text = "OK",
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
