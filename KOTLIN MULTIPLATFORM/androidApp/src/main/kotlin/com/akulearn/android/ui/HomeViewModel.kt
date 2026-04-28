package com.akulearn.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akuplatform.shared.auth.AuthRepository
import com.akuplatform.shared.course.CourseRepository
import com.akuplatform.shared.course.model.Course
import com.akuplatform.shared.course.model.Enrollment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val enrolledCourses: List<Pair<Enrollment, Course?>> = emptyList(),
    val isLoadingCourses: Boolean = false,
    val streakDays: Int = 0,
    val userName: String = ""
)

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingCourses = true)

            // Load profile for name and streak
            authRepository.getProfile().onSuccess { profile ->
                _uiState.value = _uiState.value.copy(
                    userName = profile.name,
                    streakDays = profile.streakDays
                )
            }

            // Load enrolled courses with course details
            val enrollments = courseRepository.getEnrolledCourses().getOrDefault(emptyList())
            val allCourses = courseRepository.getCourses().getOrDefault(emptyList())
            val courseMap = allCourses.associateBy { it.id }

            val enriched = enrollments.map { enrollment ->
                enrollment to courseMap[enrollment.courseId]
            }
            _uiState.value = _uiState.value.copy(
                enrolledCourses = enriched,
                isLoadingCourses = false
            )
        }
    }

    fun refresh() = loadHomeData()

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val courseRepository: CourseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(authRepository, courseRepository) as T
    }
}
