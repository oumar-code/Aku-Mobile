package com.akuplatform.shared.course.model

/**
 * Represents locally-tracked lesson completion for the current user.
 *
 * This supplements the server-authoritative [Lesson.isCompleted] flag with a fast
 * local record so the UI can reflect completion state without a round-trip.
 */
data class LessonProgress(
    val lessonId: String,
    val courseId: String,
    val isCompleted: Boolean,
    val completedAt: String? = null,
    /** Fractional playback progress (0.0–1.0). Used for resume-position display. */
    val playbackFraction: Float = 0f
)
