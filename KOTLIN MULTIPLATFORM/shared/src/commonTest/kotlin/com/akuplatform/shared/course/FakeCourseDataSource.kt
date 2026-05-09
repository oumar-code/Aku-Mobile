package com.akuplatform.shared.course

import com.akuplatform.shared.course.model.Certificate
import com.akuplatform.shared.course.model.Course
import com.akuplatform.shared.course.model.Enrollment
import com.akuplatform.shared.course.model.Lesson

/**
 * In-memory [CourseDataSource] double for unit tests.
 *
 * All data is provided at construction time.  Pass a non-null [shouldThrow] to
 * simulate network or server failures for any operation.
 */
internal class FakeCourseDataSource(
    private val courses: List<Course> = emptyList(),
    private val lessonsMap: Map<String, List<Lesson>> = emptyMap(),
    private val enrollments: List<Enrollment> = emptyList(),
    private val certificates: List<Certificate> = emptyList(),
    private val shouldThrow: Exception? = null
) : CourseDataSource {

    /** Tracks lesson IDs that have been marked complete via [markLessonComplete]. */
    val completedLessonIds: MutableSet<String> = mutableSetOf()

    override suspend fun getCourses(): List<Course> = result { courses }

    override suspend fun getCourseById(id: String): Course = result {
        courses.first { it.id == id }
    }

    override suspend fun getLessons(courseId: String): List<Lesson> = result {
        lessonsMap[courseId] ?: emptyList()
    }

    override suspend fun getSignedContentUrl(lesson: Lesson): String = result {
        "https://signed.example.com/${lesson.id}"
    }

    override suspend fun getEnrolledCourses(): List<Enrollment> = result { enrollments }

    override suspend fun enrollInCourse(courseId: String): Enrollment = result {
        Enrollment(
            id = "enroll-$courseId",
            courseId = courseId,
            userId = "u1",
            enrolledAt = "2024-01-01",
            progressPercent = 0,
            completedLessons = 0
        )
    }

    override suspend fun markLessonComplete(lessonId: String): Unit = result {
        completedLessonIds.add(lessonId)
    }

    override suspend fun getCertificates(): List<Certificate> = result { certificates }

    private fun <T> result(block: () -> T): T {
        if (shouldThrow != null) throw shouldThrow
        return block()
    }
}
