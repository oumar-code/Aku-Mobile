package com.akulearn.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akuplatform.shared.api.ApiError
import com.akuplatform.shared.course.CourseRepository
import com.akuplatform.shared.course.model.Course
import com.akuplatform.shared.course.model.Enrollment
import com.akuplatform.shared.course.model.Lesson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CourseDetailUiState(
    val isLoading: Boolean = false,
    val course: Course? = null,
    val lessons: List<Lesson> = emptyList(),
    val enrollment: Enrollment? = null,
    val isEnrolling: Boolean = false,
    val error: String? = null,
    val enrollmentError: String? = null
)

class CourseDetailViewModel(
    private val courseRepository: CourseRepository,
    private val courseId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseDetailUiState(isLoading = true))
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

    init {
        loadCourseDetail()
    }

    private fun loadCourseDetail() {
        viewModelScope.launch {
            _uiState.value = CourseDetailUiState(isLoading = true)

            val courseResult = courseRepository.getCourseById(courseId)
            if (courseResult.isFailure) {
                _uiState.value = CourseDetailUiState(error = errorMessage(courseResult.exceptionOrNull()))
                return@launch
            }
            val course = courseResult.getOrThrow()

            val lessons = courseRepository.getLessons(courseId).getOrDefault(emptyList())

            // Check if the user is already enrolled
            val enrollment = courseRepository.getEnrolledCourses()
                .getOrDefault(emptyList())
                .firstOrNull { it.courseId == courseId }

            _uiState.value = CourseDetailUiState(
                course = course,
                lessons = lessons,
                enrollment = enrollment
            )
        }
    }

    fun enroll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isEnrolling = true, enrollmentError = null)
            courseRepository.enrollInCourse(courseId)
                .onSuccess { enrollment ->
                    _uiState.value = _uiState.value.copy(
                        isEnrolling = false,
                        enrollment = enrollment
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isEnrolling = false,
                        enrollmentError = errorMessage(e)
                    )
                }
        }
    }

    fun clearEnrollmentError() {
        _uiState.value = _uiState.value.copy(enrollmentError = null)
    }

    private fun errorMessage(e: Throwable?): String = when (e) {
        is ApiError.Unauthorized -> "Session expired. Please log in again."
        is ApiError.Network -> "Network error. Check your connection."
        is ApiError.ServerError -> "Server error. Please try again later."
        else -> e?.message ?: "An unexpected error occurred."
    }

    class Factory(
        private val courseRepository: CourseRepository,
        private val courseId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CourseDetailViewModel(courseRepository, courseId) as T
    }
}
