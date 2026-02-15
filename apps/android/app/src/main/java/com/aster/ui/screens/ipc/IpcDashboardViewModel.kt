package com.aster.ui.screens.ipc

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aster.data.local.SettingsDataStore
import com.aster.service.AsterService
import com.aster.service.mode.IpcMode
import com.aster.service.mode.ModeStatus
import com.aster.service.mode.ModeType
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
class IpcDashboardViewModel @Inject constructor(
    private val ipcMode: IpcMode,
    private val settingsDataStore: SettingsDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val status: StateFlow<ModeStatus> = ipcMode.statusFlow

    private val _token = MutableStateFlow("")
    val token: StateFlow<String> = _token.asStateFlow()

    val tools: List<ToolInfo> = ipcMode.getAvailableTools()

    init {
        viewModelScope.launch {
            val savedToken = settingsDataStore.ipcToken.first()
            if (savedToken != null) {
                _token.value = savedToken
            } else {
                val newToken = ipcMode.generateToken()
                _token.value = newToken
                settingsDataStore.saveIpcToken(newToken)
            }
        }
    }

    fun startIpc() {
        viewModelScope.launch {
            settingsDataStore.setLastMode("IPC")
            AsterService.startService(context, ModeType.IPC, "")
        }
    }

    fun stopIpc() {
        AsterService.stopMode(context, ModeType.IPC)
    }

    fun regenerateToken() {
        val newToken = ipcMode.generateToken()
        _token.value = newToken
        viewModelScope.launch {
            settingsDataStore.saveIpcToken(newToken)
        }
    }
}
