package com.akulearn.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akuplatform.shared.api.ApiError
import com.akuplatform.shared.course.CourseRepository
import com.akuplatform.shared.course.model.Course
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CoursesUiState(
    val isLoading: Boolean = false,
    val courses: List<Course> = emptyList(),
    val error: String? = null
)

class CoursesViewModel(private val courseRepository: CourseRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CoursesUiState(isLoading = true))
    val uiState: StateFlow<CoursesUiState> = _uiState.asStateFlow()

    init {
        loadCourses()
    }

    fun loadCourses() {
        viewModelScope.launch {
            _uiState.value = CoursesUiState(isLoading = true)
            courseRepository.getCourses()
                .onSuccess { courses ->
                    _uiState.value = CoursesUiState(courses = courses)
                }
                .onFailure { e ->
                    val message = when (e) {
                        is ApiError.Unauthorized -> "Session expired. Please log in again."
                        is ApiError.Network -> "Network error. Check your connection."
                        is ApiError.ServerError -> "Server error. Please try again later."
                        else -> e.message ?: "Failed to load courses."
                    }
                    _uiState.value = CoursesUiState(error = message)
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    class Factory(private val courseRepository: CourseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CoursesViewModel(courseRepository) as T
    }
}
