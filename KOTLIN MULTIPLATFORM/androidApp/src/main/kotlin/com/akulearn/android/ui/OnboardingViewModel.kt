package com.akulearn.android.ui

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_prefs")
private val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")

data class OnboardingUiState(
    val isChecking: Boolean = true,
    val hasSeenOnboarding: Boolean = false
)

class OnboardingViewModel(private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val seen = context.onboardingDataStore.data.first()[ONBOARDING_SEEN] ?: false
            _uiState.value = OnboardingUiState(isChecking = false, hasSeenOnboarding = seen)
        }
    }

    fun markOnboardingSeen() {
        viewModelScope.launch {
            context.onboardingDataStore.edit { prefs -> prefs[ONBOARDING_SEEN] = true }
            _uiState.value = _uiState.value.copy(hasSeenOnboarding = true)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OnboardingViewModel(context) as T
    }
}
