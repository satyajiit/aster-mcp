package com.aster.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import com.aster.service.AsterService
import com.aster.service.mode.IpcMode
import com.aster.service.mode.ModeState
import com.aster.service.mode.ModeType
import com.aster.ui.theme.AsterTheme
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

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callingPkg = callingActivity?.packageName ?: callingPackage
        val (appName, appIcon) = resolveCallerInfo(callingPkg)
        val iconBitmap = appIcon?.toBitmap()

        setContent {
            AsterTheme {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val scope = rememberCoroutineScope()

                if (showSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { deny() },
                        sheetState = sheetState,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // App icon + name
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

                            // Approve button
                            Button(
                                onClick = {
                                    scope.launch { approve() }
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

                            // Deny button
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
            }
        }
    }

    private suspend fun approve() {
        try {
            val token: String

            if (AsterService.activeModeTypes.contains(ModeType.IPC)) {
                // IPC already running — reuse existing token
                val existing = ipcMode.token
                token = if (existing.isEmpty()) {
                    // Token lost despite IPC running — regenerate
                    ipcMode.generateToken()
                } else {
                    existing
                }
                Log.i(TAG, "IPC already running, reusing token")
            } else {
                // Generate token and start IPC mode via service
                // buildConfig() in AsterService will use this token and save it
                token = ipcMode.generateToken()
                AsterService.startService(this, ModeType.IPC, token)

                // Wait for IPC mode to be RUNNING (5s timeout)
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
