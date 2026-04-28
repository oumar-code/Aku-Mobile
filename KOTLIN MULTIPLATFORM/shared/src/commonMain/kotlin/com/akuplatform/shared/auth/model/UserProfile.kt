package com.akuplatform.shared.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the authenticated user's profile as returned by GET /users/me.
 */
@Serializable
data class UserProfile(
    val id: String,
    val name: String = "",
    val email: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("joined_at") val joinedAt: String = "",
    /** Current activity streak in days. */
    @SerialName("streak_days") val streakDays: Int = 0
)
