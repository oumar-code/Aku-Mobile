package com.akuplatform.shared.course

import com.akuplatform.shared.api.ApiError
import com.akuplatform.shared.auth.SessionManager
import com.akuplatform.shared.course.cache.CourseCache
import com.akuplatform.shared.course.cache.InMemoryCourseCache
import com.akuplatform.shared.course.model.Certificate
import com.akuplatform.shared.course.model.Course
import com.akuplatform.shared.course.model.Enrollment
import com.akuplatform.shared.course.model.Lesson
import com.akuplatform.shared.course.progress.LessonProgressStorage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * High-level repository for course content.
 *
 * Wraps a [CourseDataSource] (backed by Supabase Postgrest + Storage in production)
 * and layers an in-memory [CourseCache] on top of the courses list to avoid
 * redundant network calls within the cache TTL.
 *
 * Auth tokens are managed transparently by the Supabase GoTrue session; callers
 * never need to handle tokens directly.
 *
 * @param progressStorage Optional local progress store. When provided, lesson
 *                        completion is persisted locally in addition to being
 *                        reported to Supabase so the UI reflects completion
 *                        state without an additional network round-trip.
 */
class CourseRepository internal constructor(
    private val sessionManager: SessionManager,
    private val dataSource: CourseDataSource,
    private val cache: CourseCache = InMemoryCourseCache(),
    private val progressStorage: LessonProgressStorage? = null
) {

    /**
     * Primary constructor used by the Koin DI graph.
     * The [supabaseClient] must have the Postgrest and Storage plugins installed.
     */
    constructor(
        supabaseClient: SupabaseClient,
        sessionManager: SessionManager,
        cache: CourseCache = InMemoryCourseCache(),
        progressStorage: LessonProgressStorage? = null
    ) : this(
        sessionManager = sessionManager,
        dataSource = SupabaseCourseDataSource(supabaseClient),
        cache = cache,
        progressStorage = progressStorage
    )

    /**
     * Secondary constructor for iOS / standalone usage where a DI container is not
     * available.  Creates its own [SupabaseClient] configured with Auth, Postgrest,
     * and Storage.
     */
    constructor(
        supabaseUrl: String,
        supabaseAnonKey: String,
        sessionManager: SessionManager,
        cache: CourseCache = InMemoryCourseCache(),
        progressStorage: LessonProgressStorage? = null
    ) : this(
        supabaseClient = createSupabaseClient(supabaseUrl, supabaseAnonKey) {
            install(Auth)
            install(Postgrest)
            install(Storage)
        },
        sessionManager = sessionManager,
        cache = cache,
        progressStorage = progressStorage
    )


    private suspend fun userId(): String {
        val token = sessionManager.getToken()?.accessToken ?: return "default"
        return token.hashCode().toString()
    }

    // ── Catalogue ─────────────────────────────────────────────────────────────

    /**
     * Returns the full courses catalogue.
     * Results are served from the in-memory cache for up to five minutes; after
     * that the cache expires and the next call fetches fresh data from Supabase.
     */
    suspend fun getCourses(): Result<List<Course>> {
        val cached = cache.getCourses()
        if (cached != null) return Result.success(cached)
        return safeCall { dataSource.getCourses() }.onSuccess { cache.putCourses(it) }
    }

    /** Returns a single course by its [id]. Always fetches from Supabase. */
    suspend fun getCourseById(id: String): Result<Course> =
        safeCall { dataSource.getCourseById(id) }

    /** Returns all lessons for a given [courseId]. Always fetches from Supabase. */
    suspend fun getLessons(courseId: String): Result<List<Lesson>> =
        safeCall { dataSource.getLessons(courseId) }

    /**
     * Returns a time-limited signed URL for the lesson's media content.
     *
     * Video content is signed for 4 hours; PDF / other content for 1 hour.
     * Call this just before starting playback — do not cache the signed URL
     * as it will expire.
     */
    suspend fun getSignedContentUrl(lesson: Lesson): Result<String> =
        safeCall { dataSource.getSignedContentUrl(lesson) }

    // ── Enrollments ───────────────────────────────────────────────────────────

    /** Returns the current user's enrollments. Always fetches from Supabase. */
    suspend fun getEnrolledCourses(): Result<List<Enrollment>> =
        safeCall { dataSource.getEnrolledCourses() }

    /** Enrols the current user in [courseId]. Invalidates the courses cache on success. */
    suspend fun enrollInCourse(courseId: String): Result<Enrollment> =
        safeCall { dataSource.enrollInCourse(courseId) }
            .also { if (it.isSuccess) cache.invalidate() }

    // ── Lesson completion ─────────────────────────────────────────────────────

    /**
     * Marks a lesson as complete.
     *
     * Reports the completion to Supabase and, on success, also persists the
     * record locally via [progressStorage] so the UI can reflect completion
     * state without an additional network round-trip.
     */
    suspend fun markLessonComplete(lessonId: String): Result<Unit> =
        safeCall { dataSource.markLessonComplete(lessonId) }
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

    // ── Search & filter ───────────────────────────────────────────────────────

    /**
     * Searches courses by [query].
     * Uses client-side filtering when the catalogue is cached;
     * falls back to a Supabase search otherwise.
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
        return safeCall { dataSource.getCourses() }.map { all ->
            val q = query.trim().lowercase()
            if (q.isBlank()) all
            else all.filter {
                it.title.lowercase().contains(q) || it.instructor.lowercase().contains(q)
            }
        }
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

    // ── Certificates ──────────────────────────────────────────────────────────

    /** Returns the current user's certificates. */
    suspend fun getCertificates(): Result<List<Certificate>> =
        safeCall { dataSource.getCertificates() }

    // ── Cache management ──────────────────────────────────────────────────────

    /** Manually invalidates the courses cache, forcing the next [getCourses] to re-fetch. */
    suspend fun invalidateCache() = cache.invalidate()

    // ── Internal helpers ──────────────────────────────────────────────────────

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: ApiError) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(ApiError.Network(e.message ?: "Unknown error"))
    }
}

