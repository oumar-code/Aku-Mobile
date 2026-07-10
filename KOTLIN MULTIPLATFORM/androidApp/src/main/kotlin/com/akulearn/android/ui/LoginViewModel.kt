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

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState(error = "Email and password are required.")
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            authRepository.login(email, password)
                .onSuccess { _uiState.value = LoginUiState(isSuccess = true) }
                .onFailure { e ->
                    val message = when {
                        e is ApiError.Unauthorized -> "Invalid email or password."
                        e is ApiError.Network -> "Network error. Check your connection."
                        e is ApiError.ServerError && e.code == 500 &&
                            e.message?.contains("not configured", ignoreCase = true) == true ->
                            "Service unavailable. The app is not properly configured. Contact support."
                        e is ApiError.ServerError -> "Server error. Please try again later."
                        else -> e.message ?: "Login failed. Please try again."
                    }
                    _uiState.value = LoginUiState(error = message)
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LoginViewModel(authRepository) as T
    }
}
