package com.aster.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aster.data.local.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    companion object {
        const val STEP_ABOUT = 0
        const val STEP_PERMISSIONS = 1
        const val TOTAL_STEPS = 2
    }

    private val _currentStep = MutableStateFlow(STEP_ABOUT)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    fun nextStep() {
        if (_currentStep.value < TOTAL_STEPS - 1) {
            _currentStep.value++
        }
    }

    fun prevStep() {
        if (_currentStep.value > 0) {
            _currentStep.value--
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsDataStore.setOnboardingComplete(true)
        }
    }
}
