package com.akuplatform.shared.course.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a learner's enrolment in a [Course].
 */
@Serializable
data class Enrollment(
    val id: String,
    @SerialName("course_id") val courseId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("enrolled_at") val enrolledAt: String = "",
    @SerialName("progress_percent") val progressPercent: Int = 0
)
