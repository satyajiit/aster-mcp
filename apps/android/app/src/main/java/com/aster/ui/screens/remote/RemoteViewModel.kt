package com.aster.ui.screens.remote

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aster.data.local.SettingsDataStore
import com.aster.data.model.ConnectionState
import com.aster.data.websocket.AsterWebSocketClient
import com.aster.service.AsterService
import com.aster.service.mode.ModeStatus
import com.aster.service.mode.ModeType
import com.aster.service.mode.RemoteWsMode
import com.aster.service.mode.ToolInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemoteViewModel @Inject constructor(
    private val webSocketClient: AsterWebSocketClient,
    private val remoteWsMode: RemoteWsMode,
    private val settingsDataStore: SettingsDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = webSocketClient.connectionState

    val status: StateFlow<ModeStatus> = remoteWsMode.statusFlow

    val tools: List<ToolInfo> = remoteWsMode.getAvailableTools()

    init {
        viewModelScope.launch {
            val savedUrl = settingsDataStore.serverUrl.first()
            if (savedUrl != null) {
                _serverUrl.value = savedUrl
            }
        }

        // Collect errors from the WebSocket client
        viewModelScope.launch {
            webSocketClient.errors.collect { error ->
                _lastError.value = error
            }
        }

        // Clear error when connection state changes away from ERROR
        viewModelScope.launch {
            webSocketClient.connectionState.collect { state ->
                if (state != ConnectionState.ERROR) {
                    _lastError.value = null
                }
            }
        }
    }

    fun updateServerUrl(url: String) {
        _serverUrl.value = url
    }

    fun connect() {
        val url = _serverUrl.value.trim()
        if (url.isBlank()) return

        viewModelScope.launch {
            settingsDataStore.saveServerUrl(url)
            settingsDataStore.setLastMode("REMOTE_WS")
            AsterService.startService(context, ModeType.REMOTE_WS, url)
        }
    }

    fun disconnect() {
        AsterService.stopMode(context, ModeType.REMOTE_WS)
    }
}
