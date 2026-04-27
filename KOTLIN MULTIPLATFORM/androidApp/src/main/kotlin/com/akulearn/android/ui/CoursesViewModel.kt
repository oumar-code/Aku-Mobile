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

enum class DurationFilter { ALL, SHORT, MEDIUM, LONG }

data class CoursesUiState(
    val isLoading: Boolean = false,
    val courses: List<Course> = emptyList(),
    val filteredCourses: List<Course> = emptyList(),
    val query: String = "",
    val durationFilter: DurationFilter = DurationFilter.ALL,
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
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            courseRepository.getCourses()
                .onSuccess { courses ->
                    val filtered = applyFilters(courses, _uiState.value.query, _uiState.value.durationFilter)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        courses = courses,
                        filteredCourses = filtered
                    )
                }
                .onFailure { e ->
                    val message = when (e) {
                        is ApiError.Unauthorized -> "Session expired. Please log in again."
                        is ApiError.Network -> "Network error. Check your connection."
                        is ApiError.ServerError -> "Server error. Please try again later."
                        else -> e.message ?: "Failed to load courses."
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, error = message)
                }
        }
    }

    fun onSearch(query: String) {
        val filtered = applyFilters(_uiState.value.courses, query, _uiState.value.durationFilter)
        _uiState.value = _uiState.value.copy(query = query, filteredCourses = filtered)
    }

    fun onDurationFilter(filter: DurationFilter) {
        val filtered = applyFilters(_uiState.value.courses, _uiState.value.query, filter)
        _uiState.value = _uiState.value.copy(durationFilter = filter, filteredCourses = filtered)
    }

    private fun applyFilters(courses: List<Course>, query: String, duration: DurationFilter): List<Course> {
        val q = query.trim().lowercase()
        return courses
            .filter { course ->
                q.isBlank() ||
                    course.title.lowercase().contains(q) ||
                    course.instructor.lowercase().contains(q) ||
                    course.category.lowercase().contains(q)
            }
            .filter { course ->
                when (duration) {
                    DurationFilter.ALL -> true
                    DurationFilter.SHORT -> course.durationMinutes in 1..30
                    DurationFilter.MEDIUM -> course.durationMinutes in 31..60
                    DurationFilter.LONG -> course.durationMinutes > 60
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
