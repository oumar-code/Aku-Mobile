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

data class RegisterUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun register(name: String, email: String, password: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = RegisterUiState(error = "All fields are required.")
            return
        }
        viewModelScope.launch {
            _uiState.value = RegisterUiState(isLoading = true)
            authRepository.register(email, password, name)
                .onSuccess { _uiState.value = RegisterUiState(isSuccess = true) }
                .onFailure { e ->
                    val message = when (e) {
                        is ApiError.Unauthorized -> "Registration not permitted."
                        is ApiError.Network -> "Network error. Check your connection."
                        is ApiError.ServerError -> "Server error. Please try again later."
                        else -> e.message ?: "Registration failed. Please try again."
                    }
                    _uiState.value = RegisterUiState(error = message)
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RegisterViewModel(authRepository) as T
    }
}
