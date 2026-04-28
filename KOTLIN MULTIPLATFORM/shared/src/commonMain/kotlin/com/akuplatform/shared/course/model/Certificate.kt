package com.akuplatform.shared.course.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A certificate awarded when a learner completes all lessons in a [Course].
 */
@Serializable
data class Certificate(
    val id: String,
    @SerialName("course_id") val courseId: String,
    @SerialName("course_title") val courseTitle: String = "",
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String = "",
    @SerialName("issued_at") val issuedAt: String = ""
)
