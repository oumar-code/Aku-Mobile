package com.akuplatform.shared.course

import com.akuplatform.shared.api.Wave3ApiClient
import com.akuplatform.shared.auth.SessionManager
import com.akuplatform.shared.course.cache.CourseCache
import com.akuplatform.shared.course.cache.InMemoryCourseCache
import com.akuplatform.shared.course.model.Certificate
import com.akuplatform.shared.course.model.Course
import com.akuplatform.shared.course.model.Enrollment
import com.akuplatform.shared.course.model.Lesson
import com.akuplatform.shared.course.progress.LessonProgressStorage

/**
 * High-level repository for course content.
 *
 * Wraps [Wave3ApiClient] course endpoints and layers an in-memory [CourseCache]
 * on top of the courses list to avoid redundant network calls within the cache TTL.
 *
 * Auth tokens are retrieved lazily from [SessionManager] so the caller never
 * needs to manage tokens directly.
 *
 * @param progressStorage Optional local progress store. When provided, lesson completion
 *                        is persisted locally in addition to being reported to the API.
 */
class CourseRepository(
    private val apiClient: Wave3ApiClient,
    private val sessionManager: SessionManager,
    private val cache: CourseCache = InMemoryCourseCache(),
    private val progressStorage: LessonProgressStorage? = null
) {

    private suspend fun token(): String? = sessionManager.getToken()?.accessToken

    /** Returns the current user's ID from the active session token (best-effort).
     *  Falls back to "default" for single-user installations; callers requiring a stable
     *  per-account key should pass the resolved userId from a profile API response. */
    private suspend fun userId(): String {
        val token = sessionManager.getToken()?.accessToken ?: return "default"
        // Derive a stable, anonymous bucket key from the token without leaking token contents.
        return token.hashCode().toString()
    }

    /**
     * Returns the full courses catalogue.
     * Results are served from the in-memory cache for up to five minutes; after
     * that the cache expires and the next call fetches fresh data from the API.
     */
    suspend fun getCourses(): Result<List<Course>> {
        val cached = cache.getCourses()
        if (cached != null) return Result.success(cached)
        return apiClient.getCourses(token = token()).onSuccess { cache.putCourses(it) }
    }

    /** Returns a single course by its [id]. Always fetches from the network. */
    suspend fun getCourseById(id: String): Result<Course> =
        apiClient.getCourseById(id = id, token = token())

    /** Returns all lessons for a given [courseId]. Always fetches from the network. */
    suspend fun getLessons(courseId: String): Result<List<Lesson>> =
        apiClient.getLessons(courseId = courseId, token = token())

    /** Returns the current user's enrollments. Always fetches from the network. */
    suspend fun getEnrolledCourses(): Result<List<Enrollment>> =
        apiClient.getEnrolledCourses(token = token())

    /** Enrols the current user in [courseId]. Invalidates the courses cache on success. */
    suspend fun enrollInCourse(courseId: String): Result<Enrollment> =
        apiClient.enrollInCourse(courseId = courseId, token = token())
            .also { if (it.isSuccess) cache.invalidate() }

    /**
     * Marks a lesson as complete.
     *
     * Reports the completion to the API and, on success, also persists the record
     * locally via [progressStorage] so the UI can reflect completion state without
     * an additional network round-trip.
     */
    suspend fun markLessonComplete(lessonId: String): Result<Unit> =
        apiClient.markLessonComplete(lessonId = lessonId, token = token())
            .also { result ->
                if (result.isSuccess) {
                    progressStorage?.markComplete(userId(), lessonId)
                }
            }

    /**
     * Returns the set of lesson IDs locally marked complete for the current user.
     *
     * Falls back to an empty set when no [progressStorage] is configured.
     */
    suspend fun getCompletedLessons(): Set<String> =
        progressStorage?.getCompletedLessonIds(userId()) ?: emptySet()

    /**
     * Searches courses by [query].
     * Uses client-side filtering when the catalogue is cached;
     * falls back to the server search endpoint otherwise.
     */
    suspend fun searchCourses(query: String): Result<List<Course>> {
        val cached = cache.getCourses()
        if (cached != null) {
            val q = query.trim().lowercase()
            return Result.success(
                if (q.isBlank()) cached
                else cached.filter {
                    it.title.lowercase().contains(q) || it.instructor.lowercase().contains(q)
                }
            )
        }
        return apiClient.searchCourses(query = query, token = token())
    }

    /**
     * Filters courses by [category].
     * Uses client-side filtering when the catalogue is cached;
     * falls back to fetching all courses and filtering otherwise.
     */
    suspend fun filterCourses(category: String): Result<List<Course>> {
        val source = cache.getCourses() ?: return getCourses().map { courses ->
            if (category.isBlank()) courses
            else courses.filter { it.category.lowercase() == category.trim().lowercase() }
        }
        return Result.success(
            if (category.isBlank()) source
            else source.filter { it.category.lowercase() == category.trim().lowercase() }
        )
    }

    /** Returns the current user's certificates. */
    suspend fun getCertificates(): Result<List<Certificate>> =
        apiClient.getCertificates(token = token())

    /** Manually invalidates the courses cache, forcing the next [getCourses] to re-fetch. */
    suspend fun invalidateCache() = cache.invalidate()
}
