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

data class ForgotPasswordUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class ForgotPasswordViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun requestReset(email: String) {
        if (email.isBlank()) {
            _uiState.value = ForgotPasswordUiState(error = "Email is required.")
            return
        }
        viewModelScope.launch {
            _uiState.value = ForgotPasswordUiState(isLoading = true)
            authRepository.requestPasswordReset(email)
                .onSuccess { _uiState.value = ForgotPasswordUiState(isSuccess = true) }
                .onFailure { e ->
                    val message = when (e) {
                        is ApiError.Network -> "Network error. Check your connection."
                        is ApiError.ServerError -> "Server error. Please try again later."
                        else -> e.message ?: "Request failed. Please try again."
                    }
                    _uiState.value = ForgotPasswordUiState(error = message)
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ForgotPasswordViewModel(authRepository) as T
    }
}
