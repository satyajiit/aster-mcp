package com.aster.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aster.data.local.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val themeMode: StateFlow<String> = settingsDataStore.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val autoStartMode: StateFlow<String?> = settingsDataStore.autoStartMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val autoStartOnBoot: StateFlow<Boolean> = settingsDataStore.autoStartOnBoot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }

    fun setAutoStartOnBoot(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAutoStartOnBoot(enabled)
        }
    }

    fun setAutoStartMode(mode: String?) {
        viewModelScope.launch {
            settingsDataStore.setAutoStartMode(mode)
        }
    }
}
