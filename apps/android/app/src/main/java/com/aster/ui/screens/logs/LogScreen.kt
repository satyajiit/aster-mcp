package com.aster.ui.screens.logs

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aster.data.local.db.ToolCallLog
import com.aster.ui.components.AsterCard
import com.aster.ui.components.AsterStatCard
import com.aster.ui.components.AsterTopBar
import com.aster.ui.theme.AsterTheme
import compose.icons.FeatherIcons
import compose.icons.feathericons.Activity
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.Hash
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val IpcColor = Color(0xFFF59E0B)
private val McpColor = Color(0xFF8B5CF6)
private val RemoteColor = Color(0xFF3B82F6)
private val SuccessColor = Color(0xFF22C55E)
private val ErrorColor = Color(0xFFEF4444)

@Composable
fun LogScreen(
    onNavigateBack: () -> Unit,
    viewModel: LogViewModel = hiltViewModel()
) {
    val colors = AsterTheme.colors
    val logs by viewModel.logs.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val successCount by viewModel.successCount.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.bg,
        topBar = {
            AsterTopBar(
                title = "Tool Call Logs",
                onBack = onNavigateBack,
                actions = {
                    if (logs.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearLogs() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear logs",
                                tint = colors.textSubtle
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stats row
            item {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsterStatCard(
                        icon = FeatherIcons.Hash,
                        value = totalCount.toString(),
                        label = "Total Calls",
                        accentColor = RemoteColor,
                        modifier = Modifier.weight(1f)
                    )

                    AsterStatCard(
                        icon = FeatherIcons.CheckCircle,
                        value = if (totalCount > 0) "${(successCount * 100 / totalCount)}%" else "–",
                        label = "Success Rate",
                        accentColor = SuccessColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (logs.isEmpty()) {
                item {
                    AsterCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = FeatherIcons.Activity,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No tool calls yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textMuted
                            )
                            Text(
                                text = "Tool calls will appear here as they happen",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }

            items(logs, key = { it.id }) { log ->
                LogEntry(log = log)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LogEntry(log: ToolCallLog) {
    val colors = AsterTheme.colors

    val modeColor = when (log.connectionType) {
        "IPC" -> IpcColor
        "LOCAL_MCP" -> McpColor
        "REMOTE_WS" -> RemoteColor
        else -> colors.textMuted
    }

    val modeLabel = when (log.connectionType) {
        "IPC" -> "IPC"
        "LOCAL_MCP" -> "MCP"
        "REMOTE_WS" -> "Remote"
        else -> log.connectionType
    }

    AsterCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Top row: action name + status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (log.success) SuccessColor else ErrorColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = log.action,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.text,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                // Mode badge
                Text(
                    text = modeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = modeColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(modeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Bottom row: timestamp + duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(log.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                if (log.durationMs > 0) {
                    Text(
                        text = "${log.durationMs}ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSubtle,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }

            // Error message if present
            if (!log.errorMessage.isNullOrBlank()) {
                Text(
                    text = log.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorColor,
                    fontSize = 11.sp,
                    maxLines = 2
                )
            }
        }
    }
}

private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
private val dateTimeFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    // Show time only if today, otherwise show date + time
    return if (diff < 24 * 60 * 60 * 1000) {
        timeFormat.format(Date(timestamp))
    } else {
        dateTimeFormat.format(Date(timestamp))
    }
}
