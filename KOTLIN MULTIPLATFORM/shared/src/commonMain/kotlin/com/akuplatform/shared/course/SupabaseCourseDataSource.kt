package com.akuplatform.shared.course

import com.akuplatform.shared.api.ApiError
import com.akuplatform.shared.course.model.Certificate
import com.akuplatform.shared.course.model.Course
import com.akuplatform.shared.course.model.Enrollment
import com.akuplatform.shared.course.model.Lesson
import com.akuplatform.shared.course.model.LessonContentType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Platform-agnostic abstraction over all course-related data access.
 *
 * Production code uses [SupabaseCourseDataSource]; tests supply a
 * [FakeCourseDataSource] to avoid real network calls while preserving all
 * repository-level logic.
 */
internal interface CourseDataSource {
    suspend fun getCourses(): List<Course>
    suspend fun getCourseById(id: String): Course
    suspend fun getLessons(courseId: String): List<Lesson>

    /**
     * Returns a time-limited signed URL for the lesson's media content.
     *
     * Video content is signed for [VIDEO_TTL_SECONDS] (4 hours).
     * PDF / other content is signed for [PDF_TTL_SECONDS] (1 hour).
     * If [lesson]'s `contentUrl` is already an absolute HTTP(S) URL the
     * value is returned unchanged (e.g. externally-hosted content).
     */
    suspend fun getSignedContentUrl(lesson: Lesson): String

    suspend fun getEnrolledCourses(): List<Enrollment>
    suspend fun enrollInCourse(courseId: String): Enrollment
    suspend fun markLessonComplete(lessonId: String)
    suspend fun getCertificates(): List<Certificate>

    companion object {
        /** 4-hour TTL for video content (in seconds). */
        const val VIDEO_TTL_SECONDS = 14_400L

        /** 1-hour TTL for PDF / other content (in seconds). */
        const val PDF_TTL_SECONDS = 3_600L

        /** Supabase Storage bucket holding course media files. */
        const val CONTENT_BUCKET = "course-content"
    }
}

// ── Postgrest request payload models ─────────────────────────────────────────

@Serializable
private data class EnrollRequest(@SerialName("course_id") val courseId: String)

@Serializable
private data class LessonCompletion(@SerialName("lesson_id") val lessonId: String)

// ── Supabase implementation ───────────────────────────────────────────────────

/**
 * [CourseDataSource] backed by Supabase Postgrest (relational data) and
 * Supabase Storage (time-limited signed content URLs).
 *
 * Row-Level-Security (RLS) policies on the Supabase project enforce per-user
 * data scoping automatically via the authenticated JWT, so no explicit
 * `user_id` filters are required in most queries.
 */
internal class SupabaseCourseDataSource(
    private val client: SupabaseClient
) : CourseDataSource {

    // ── Catalogue ─────────────────────────────────────────────────────────────

    override suspend fun getCourses(): List<Course> =
        safeCall { client.postgrest["courses"].select().decodeList<Course>() }

    override suspend fun getCourseById(id: String): Course =
        safeCall {
            client.postgrest["courses"].select {
                eq("id", id)
            }.decodeSingle<Course>()
        }

    override suspend fun getLessons(courseId: String): List<Lesson> =
        safeCall {
            client.postgrest["lessons"].select {
                eq("course_id", courseId)
            }.decodeList<Lesson>().sortedBy { it.orderIndex }
        }

    // ── Signed content URL ────────────────────────────────────────────────────

    override suspend fun getSignedContentUrl(lesson: Lesson): String {
        val path = lesson.contentUrl
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        if (path.isBlank()) return path

        val ttl = when (lesson.contentType) {
            LessonContentType.VIDEO -> CourseDataSource.VIDEO_TTL_SECONDS
            else -> CourseDataSource.PDF_TTL_SECONDS
        }
        return safeCall {
            client.storage[CourseDataSource.CONTENT_BUCKET].createSignedUrl(path, expiresIn = ttl)
        }
    }

    // ── Enrollments ───────────────────────────────────────────────────────────

    override suspend fun getEnrolledCourses(): List<Enrollment> =
        safeCall { client.postgrest["enrollments"].select().decodeList<Enrollment>() }

    override suspend fun enrollInCourse(courseId: String): Enrollment =
        safeCall {
            client.postgrest["enrollments"]
                .insert(EnrollRequest(courseId = courseId))
                .decodeSingle<Enrollment>()
        }

    // ── Lesson completion ─────────────────────────────────────────────────────

    override suspend fun markLessonComplete(lessonId: String): Unit =
        safeCall {
            client.postgrest["lesson_completions"]
                .upsert(LessonCompletion(lessonId = lessonId))
        }

    // ── Certificates ──────────────────────────────────────────────────────────

    override suspend fun getCertificates(): List<Certificate> =
        safeCall { client.postgrest["certificates"].select().decodeList<Certificate>() }

    // ── Error mapping ─────────────────────────────────────────────────────────

    private suspend fun <T> safeCall(block: suspend () -> T): T = try {
        block()
    } catch (e: RestException) {
        when (e.statusCode) {
            401 -> throw ApiError.Unauthorized(e.message ?: "Authentication required.")
            403 -> throw ApiError.Unauthorized(e.message ?: "Access forbidden.")
            else -> throw ApiError.ServerError(e.statusCode, e.message ?: "Supabase error.")
        }
    } catch (e: HttpRequestException) {
        throw ApiError.Network(e.message ?: "Network error.")
    } catch (e: ApiError) {
        throw e
    } catch (e: NoSuchElementException) {
        throw ApiError.ServerError(404, e.message ?: "Not found.")
    }
}

