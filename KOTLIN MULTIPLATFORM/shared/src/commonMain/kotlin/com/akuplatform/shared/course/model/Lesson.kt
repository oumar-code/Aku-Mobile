package com.akuplatform.shared.course.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Content type for a lesson.
 */
enum class LessonContentType { TEXT, VIDEO, QUIZ, UNKNOWN }

/**
 * A single lesson within a [Course].
 */
@Serializable
data class Lesson(
    val id: String,
    @SerialName("course_id") val courseId: String,
    val title: String,
    val description: String = "",
    @SerialName("duration_minutes") val durationMinutes: Int = 0,
    @SerialName("order_index") val orderIndex: Int = 0,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    /** Raw markdown / HTML body for text lessons. */
    val body: String = "",
    /** URL to a video or supplementary resource. */
    @SerialName("content_url") val contentUrl: String = "",
    /** Lesson content type returned by the API ("text", "video", "quiz"). */
    @SerialName("content_type") val contentTypeRaw: String = "text"
) {
    val contentType: LessonContentType
        get() = when (contentTypeRaw.lowercase()) {
            "video" -> LessonContentType.VIDEO
            "quiz" -> LessonContentType.QUIZ
            "text" -> LessonContentType.TEXT
            else -> LessonContentType.UNKNOWN
        }
}
