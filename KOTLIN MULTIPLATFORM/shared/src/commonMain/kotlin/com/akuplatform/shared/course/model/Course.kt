package com.akuplatform.shared.course.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents an Akulearn course as returned by the Wave 3 API.
 */
@Serializable
data class Course(
    val id: String,
    val title: String,
    val description: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val instructor: String = "",
    @SerialName("lesson_count") val lessonCount: Int = 0,
    @SerialName("duration_minutes") val durationMinutes: Int = 0
)
