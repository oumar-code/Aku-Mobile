package com.akuplatform.shared.course.cache

import com.akuplatform.shared.course.model.Course
import com.akuplatform.shared.database.AkuDatabase
import com.akuplatform.shared.database.DatabaseDriverFactory
import kotlinx.datetime.Clock

/**
 * [CourseCache] implementation backed by the local SQLDelight [AkuDatabase].
 *
 * The same TTL-expiry semantics as [InMemoryCourseCache] are preserved — the cache
 * is considered stale after [ttlMs] milliseconds and the next [getCourses] call will
 * return `null`, prompting a fresh network fetch.
 *
 * @param driverFactory Platform-specific [DatabaseDriverFactory].
 * @param ttlMs         How long (in ms) a cached result remains valid. Default 5 minutes.
 */
class SqlDelightCourseCache(
    driverFactory: DatabaseDriverFactory,
    private val ttlMs: Long = 5 * 60 * 1000L
) : CourseCache {

    private val database = AkuDatabase(driverFactory.createDriver())
    private var cachedAtMs: Long = 0L

    override suspend fun getCourses(): List<Course>? {
        val nowMs = Clock.System.now().toEpochMilliseconds()
        if (nowMs - cachedAtMs >= ttlMs) return null
        val rows = database.courseQueries.selectAll().executeAsList()
        if (rows.isEmpty()) return null
        return rows.map { row ->
            Course(
                id = row.id,
                title = row.title,
                description = row.description,
                imageUrl = row.imageUrl,
                instructor = row.instructor,
                lessonCount = row.lessonCount.toInt(),
                durationMinutes = row.durationMinutes.toInt(),
                category = row.category,
                tags = row.tags.split(",").filter { it.isNotBlank() }
            )
        }
    }

    override suspend fun putCourses(courses: List<Course>) {
        database.transaction {
            database.courseQueries.deleteAll()
            courses.forEach { course ->
                database.courseQueries.insertCourse(
                    id = course.id,
                    title = course.title,
                    description = course.description,
                    imageUrl = course.imageUrl,
                    instructor = course.instructor,
                    lessonCount = course.lessonCount.toLong(),
                    durationMinutes = course.durationMinutes.toLong(),
                    category = course.category,
                    tags = course.tags.joinToString(",")
                )
            }
        }
        cachedAtMs = Clock.System.now().toEpochMilliseconds()
    }

    override suspend fun invalidate() {
        cachedAtMs = 0L
    }
}
