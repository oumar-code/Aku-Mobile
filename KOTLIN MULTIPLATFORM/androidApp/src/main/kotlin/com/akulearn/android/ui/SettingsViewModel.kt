package com.akulearn.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akuplatform.shared.api.ApiError
import com.akuplatform.shared.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class SettingsViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun changePassword(currentPassword: String, newPassword: String) {
        if (currentPassword.isBlank() || newPassword.isBlank()) {
            _uiState.value = SettingsUiState(error = "Both fields are required.")
            return
        }
        if (newPassword.length < 6) {
            _uiState.value = SettingsUiState(error = "New password must be at least 6 characters.")
            return
        }
        viewModelScope.launch {
            _uiState.value = SettingsUiState(isLoading = true)
            authRepository.changePassword(currentPassword, newPassword)
                .onSuccess {
                    _uiState.value = SettingsUiState(isSuccess = true)
                }
                .onFailure { e ->
                    val message = when (e) {
                        is ApiError.Unauthorized -> "Current password is incorrect."
                        is ApiError.Network -> "Network error. Check your connection."
                        else -> e.message ?: "Failed to change password."
                    }
                    _uiState.value = SettingsUiState(error = message)
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(authRepository) as T
    }
}
