package com.akulearn.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akuplatform.shared.api.ApiError
import com.akuplatform.shared.course.CourseRepository
import com.akuplatform.shared.course.model.Certificate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CertificatesUiState(
    val isLoading: Boolean = false,
    val certificates: List<Certificate> = emptyList(),
    val error: String? = null
)

class CertificatesViewModel(private val courseRepository: CourseRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CertificatesUiState(isLoading = true))
    val uiState: StateFlow<CertificatesUiState> = _uiState.asStateFlow()

    init {
        loadCertificates()
    }

    fun loadCertificates() {
        viewModelScope.launch {
            _uiState.value = CertificatesUiState(isLoading = true)
            courseRepository.getCertificates()
                .onSuccess { certs ->
                    _uiState.value = CertificatesUiState(certificates = certs)
                }
                .onFailure { e ->
                    val message = when (e) {
                        is ApiError.Unauthorized -> "Session expired. Please log in again."
                        is ApiError.Network -> "Network error. Check your connection."
                        else -> e.message ?: "Failed to load certificates."
                    }
                    _uiState.value = CertificatesUiState(error = message)
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    class Factory(private val courseRepository: CourseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CertificatesViewModel(courseRepository) as T
    }
}
