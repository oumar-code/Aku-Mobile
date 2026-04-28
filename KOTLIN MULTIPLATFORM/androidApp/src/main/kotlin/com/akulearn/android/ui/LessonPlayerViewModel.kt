package com.akulearn.android.ui

import android.content.Context
import android.content.SharedPreferences
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
    val error: String? = null,
    /** Fraction (0.0–1.0) of the lesson already watched, used for progress display. */
    val playbackFraction: Float = 0f,
    /** Saved playback position in milliseconds, applied when the player first opens. */
    val resumePositionMs: Long = 0L
)

class LessonPlayerViewModel(
    private val courseRepository: CourseRepository,
    private val lesson: Lesson,
    private val playbackPrefs: SharedPreferences? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LessonPlayerUiState(
            lesson = lesson,
            isCompleted = lesson.isCompleted,
            resumePositionMs = playbackPrefs?.getLong(positionKey(lesson.id), 0L) ?: 0L,
            playbackFraction = playbackPrefs?.getFloat(fractionKey(lesson.id), 0f) ?: 0f
        )
    )
    val uiState: StateFlow<LessonPlayerUiState> = _uiState.asStateFlow()

    /** Called by the player UI whenever the position changes. Persists position so it survives recreation. */
    fun onPositionChanged(positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L) return
        val fraction = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        _uiState.value = _uiState.value.copy(playbackFraction = fraction)
        playbackPrefs?.edit()
            ?.putLong(positionKey(lesson.id), positionMs)
            ?.putFloat(fractionKey(lesson.id), fraction)
            ?.apply()

        // Auto-mark complete when reaching ≥ COMPLETION_THRESHOLD of the video
        if (fraction >= COMPLETION_THRESHOLD && !_uiState.value.isCompleted && !_uiState.value.isMarkingComplete) {
            markComplete()
        }
    }

    fun markComplete() {
        if (_uiState.value.isCompleted || _uiState.value.isMarkingComplete) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMarkingComplete = true)
            courseRepository.markLessonComplete(lesson.id)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isMarkingComplete = false,
                        isCompleted = true,
                        playbackFraction = 1f
                    )
                    // Clear saved position so it starts from the beginning next time
                    playbackPrefs?.edit()
                        ?.remove(positionKey(lesson.id))
                        ?.putFloat(fractionKey(lesson.id), 1f)
                        ?.apply()
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
        private val lesson: Lesson,
        private val context: Context? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return LessonPlayerViewModel(courseRepository, lesson, prefs) as T
        }
    }

    companion object {
        const val PREFS_NAME = "aku_playback"
        /** Fraction of video duration at which the lesson is automatically marked complete. */
        const val COMPLETION_THRESHOLD = 0.90f
        private fun positionKey(lessonId: String) = "pos_$lessonId"
        private fun fractionKey(lessonId: String) = "frac_$lessonId"
    }
}
