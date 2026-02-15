package com.aster.ui.screens.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aster.ui.components.AnimatedEntrance
import com.aster.ui.components.AsterButton
import com.aster.ui.components.AsterButtonVariant
import com.aster.ui.components.AsterCard
import com.aster.ui.theme.AsterTheme
import com.aster.util.PermissionCheckResult
import com.aster.util.PermissionUtils
import compose.icons.FeatherIcons
import compose.icons.feathericons.Shield

@Composable
fun PermissionAlertScreen(
    onNavigateToPermissions: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AsterTheme.colors
    val context = LocalContext.current

    var permissionResult by remember { mutableStateOf<PermissionCheckResult?>(null) }

    LaunchedEffect(Unit) {
        permissionResult = PermissionUtils.checkAllPermissions(context)
    }

    val result = permissionResult ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Warning icon
            AnimatedEntrance(delayMillis = 0) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.warning.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FeatherIcons.Shield,
                        contentDescription = null,
                        tint = colors.warning,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Title
            AnimatedEntrance(delayMillis = 100) {
                Text(
                    text = "Permissions Required",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.text,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            AnimatedEntrance(delayMillis = 200) {
                Text(
                    text = "Some permissions have been revoked or were not granted. Aster needs these to function properly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSubtle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Missing permissions card
            AnimatedEntrance(delayMillis = 300) {
                AsterCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Count header
                        Text(
                            text = "${result.missingPermissions.size} of ${result.totalCount} permissions missing",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.warning,
                            fontWeight = FontWeight.SemiBold
                        )

                        // List of missing permission names
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            result.missingPermissions.forEach { permType ->
                                val name = PermissionUtils.getPermissionName(permType)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            1.dp,
                                            colors.warning.copy(alpha = 0.2f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .background(colors.warning.copy(alpha = 0.06f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.text,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Fixed footer
        AnimatedEntrance(delayMillis = 400) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsterButton(
                    onClick = onNavigateToPermissions,
                    text = "Review Permissions",
                    modifier = Modifier.fillMaxWidth()
                )

                AsterButton(
                    onClick = onSkip,
                    text = "Skip",
                    variant = AsterButtonVariant.SECONDARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
