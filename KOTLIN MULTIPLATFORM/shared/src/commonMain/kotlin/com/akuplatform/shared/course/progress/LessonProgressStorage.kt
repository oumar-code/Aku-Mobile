package com.akuplatform.shared.course.progress

/**
 * Platform-agnostic storage for local lesson completion records.
 *
 * Platform modules (Android / iOS) provide the concrete implementation:
 * - Android: [AndroidLessonProgressStorage] backed by [EncryptedSharedPreferences]
 * - iOS: KeychainLessonProgressStorage (future)
 *
 * The storage is intentionally scoped to the currently logged-in user and cleared
 * on logout.  For future multi-account support the [userId] parameter is threaded
 * through every call so that different users' progress is kept separate.
 */
interface LessonProgressStorage {

    /**
     * Persists a completion record for [lessonId] belonging to [userId].
     *
     * Idempotent — calling it multiple times for the same lesson is safe.
     */
    suspend fun markComplete(userId: String, lessonId: String)

    /**
     * Returns the set of lesson IDs that have been marked complete for [userId].
     */
    suspend fun getCompletedLessonIds(userId: String): Set<String>

    /**
     * Returns `true` when [lessonId] has been locally marked complete for [userId].
     */
    suspend fun isCompleted(userId: String, lessonId: String): Boolean =
        getCompletedLessonIds(userId).contains(lessonId)

    /**
     * Removes all locally-stored completion records for [userId].
     * Should be called on logout.
     */
    suspend fun clearAll(userId: String)
}
