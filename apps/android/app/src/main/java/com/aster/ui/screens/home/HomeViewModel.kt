package com.aster.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aster.data.local.SettingsDataStore
import com.aster.service.AsterService
import com.aster.service.mode.ModeType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val lastUsedMode: StateFlow<String?> = settingsDataStore.lastMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Whether the "star the repo" invitation has been acted on or dismissed.
     *
     * Starts `true` so the card is absent until DataStore says otherwise —
     * seeded `false`, it would flash in for a frame on every launch after the
     * user had already dismissed it.
     */
    val starPromptDismissed: StateFlow<Boolean> = settingsDataStore.starPromptDismissed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isServiceRunning: Boolean
        get() = AsterService.isRunning

    /** All currently active mode types. */
    val activeModeTypes: Set<ModeType>
        get() = AsterService.activeModeTypes

    /** Legacy — first active mode or null. */
    val activeModeType: ModeType?
        get() = AsterService.activeModeType

    fun isModeActive(modeType: ModeType): Boolean =
        AsterService.activeModeTypes.contains(modeType)

    fun setLastMode(mode: String) {
        viewModelScope.launch {
            settingsDataStore.setLastMode(mode)
        }
    }

    fun dismissStarPrompt() {
        viewModelScope.launch {
            settingsDataStore.setStarPromptDismissed(true)
        }
    }
}
