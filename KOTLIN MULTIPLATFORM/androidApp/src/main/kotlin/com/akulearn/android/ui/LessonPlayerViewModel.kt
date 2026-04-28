package com.akulearn.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akuplatform.shared.api.ApiError
import com.akuplatform.shared.course.CourseRepository
import com.akuplatform.shared.course.model.Lesson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LessonPlayerUiState(
    val lesson: Lesson? = null,
    val isMarkingComplete: Boolean = false,
    val isCompleted: Boolean = false,
    val error: String? = null
)

class LessonPlayerViewModel(
    private val courseRepository: CourseRepository,
    private val lesson: Lesson
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LessonPlayerUiState(lesson = lesson, isCompleted = lesson.isCompleted)
    )
    val uiState: StateFlow<LessonPlayerUiState> = _uiState.asStateFlow()

    fun markComplete() {
        if (_uiState.value.isCompleted || _uiState.value.isMarkingComplete) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMarkingComplete = true)
            courseRepository.markLessonComplete(lesson.id)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isMarkingComplete = false,
                        isCompleted = true
                    )
                }
                .onFailure { e ->
                    val message = when (e) {
                        is ApiError.Unauthorized -> "Session expired. Please log in again."
                        is ApiError.Network -> "Network error. Check your connection."
                        else -> e.message ?: "Failed to mark lesson complete."
                    }
                    _uiState.value = _uiState.value.copy(
                        isMarkingComplete = false,
                        error = message
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    class Factory(
        private val courseRepository: CourseRepository,
        private val lesson: Lesson
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LessonPlayerViewModel(courseRepository, lesson) as T
    }
}
