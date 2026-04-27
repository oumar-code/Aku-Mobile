package com.akuplatform.shared.course.cache

import com.akuplatform.shared.course.model.Course
import kotlinx.datetime.Clock

/**
 * Cache abstraction for the courses list.
 *
 * Implementations may use in-memory storage, a local database, or any other
 * persistence mechanism.
 */
interface CourseCache {
    /** Returns the cached courses if the cache is still valid, or `null` if it
     *  has expired or was never populated. */
    suspend fun getCourses(): List<Course>?

    /** Stores [courses] in the cache and records the current time as the population timestamp. */
    suspend fun putCourses(courses: List<Course>)

    /** Clears the cache so the next [getCourses] call always hits the network. */
    suspend fun invalidate()
}

/**
 * Simple in-memory [CourseCache] with a configurable time-to-live.
 *
 * @param ttlMs How long (in milliseconds) a cached result remains valid.
 *              Defaults to five minutes.
 */
class InMemoryCourseCache(private val ttlMs: Long = 5 * 60 * 1000L) : CourseCache {

    private var cached: List<Course>? = null
    private var cachedAtMs: Long = 0L

    override suspend fun getCourses(): List<Course>? {
        val nowMs = Clock.System.now().toEpochMilliseconds()
        return if (cached != null && (nowMs - cachedAtMs) < ttlMs) cached else null
    }

    override suspend fun putCourses(courses: List<Course>) {
        cached = courses
        cachedAtMs = Clock.System.now().toEpochMilliseconds()
    }

    override suspend fun invalidate() {
        cached = null
        cachedAtMs = 0L
    }
}
