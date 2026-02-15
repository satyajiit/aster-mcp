package com.aster.ui.screens.mcp

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aster.data.local.SettingsDataStore
import com.aster.service.AsterService
import com.aster.service.mode.McpMode
import com.aster.service.mode.ModeStatus
import com.aster.service.mode.ModeType
import com.aster.service.mode.ToolInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class McpDashboardViewModel @Inject constructor(
    private val mcpMode: McpMode,
    private val settingsDataStore: SettingsDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val status: StateFlow<ModeStatus> = mcpMode.statusFlow

    val port: StateFlow<Int> = settingsDataStore.mcpPort
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 8080)

    val tools: List<ToolInfo> = mcpMode.getAvailableTools()

    val localhostUrl: String
        get() = mcpMode.getLocalhostUrl()

    val lanUrl: String?
        get() = mcpMode.getLanUrl()

    val tailscaleUrl: String?
        get() = mcpMode.getTailscaleUrl()

    val tailscaleDnsUrl: String?
        get() = mcpMode.getTailscaleDnsUrl()

    fun startMcp() {
        viewModelScope.launch {
            settingsDataStore.setLastMode("LOCAL_MCP")
            AsterService.startService(context, ModeType.LOCAL_MCP, "")
        }
    }

    fun stopMcp() {
        AsterService.stopMode(context, ModeType.LOCAL_MCP)
    }

    fun setPort(port: Int) {
        viewModelScope.launch {
            settingsDataStore.setMcpPort(port)
        }
    }
}
