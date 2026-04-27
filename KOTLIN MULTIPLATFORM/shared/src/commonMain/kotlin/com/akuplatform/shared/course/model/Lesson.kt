package com.akuplatform.shared.course.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("is_completed") val isCompleted: Boolean = false
)
