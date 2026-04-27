package com.akuplatform.shared.course

import com.akuplatform.shared.api.Wave3ApiClient
import com.akuplatform.shared.auth.SessionManager
import com.akuplatform.shared.course.cache.CourseCache
import com.akuplatform.shared.course.cache.InMemoryCourseCache
import com.akuplatform.shared.course.model.Course
import com.akuplatform.shared.course.model.Enrollment
import com.akuplatform.shared.course.model.Lesson

/**
 * High-level repository for course content.
 *
 * Wraps [Wave3ApiClient] course endpoints and layers an in-memory [CourseCache]
 * on top of the courses list to avoid redundant network calls within the cache TTL.
 *
 * Auth tokens are retrieved lazily from [SessionManager] so the caller never
 * needs to manage tokens directly.
 */
class CourseRepository(
    private val apiClient: Wave3ApiClient,
    private val sessionManager: SessionManager,
    private val cache: CourseCache = InMemoryCourseCache()
) {

    private suspend fun token(): String? = sessionManager.getToken()?.accessToken

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

    /** Returns the current user's enrolments. Always fetches from the network. */
    suspend fun getEnrolledCourses(): Result<List<Enrollment>> =
        apiClient.getEnrolledCourses(token = token())

    /** Enrols the current user in [courseId]. Invalidates the courses cache on success. */
    suspend fun enrollInCourse(courseId: String): Result<Enrollment> =
        apiClient.enrollInCourse(courseId = courseId, token = token())
            .also { if (it.isSuccess) cache.invalidate() }

    /** Manually invalidates the courses cache, forcing the next [getCourses] to re-fetch. */
    suspend fun invalidateCache() = cache.invalidate()
}
