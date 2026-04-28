package com.akuplatform.shared.course

import com.akuplatform.shared.course.progress.LessonProgressStorage

/**
 * In-memory [LessonProgressStorage] double for unit tests.
 *
 * Backed by a simple set per userId; no encryption or persistence.
 */
class FakeLessonProgressStorage : LessonProgressStorage {

    private val completed: MutableMap<String, MutableSet<String>> = mutableMapOf()

    override suspend fun markComplete(userId: String, lessonId: String) {
        completed.getOrPut(userId) { mutableSetOf() }.add(lessonId)
    }

    override suspend fun getCompletedLessonIds(userId: String): Set<String> =
        completed[userId]?.toSet() ?: emptySet()

    override suspend fun clearAll(userId: String) {
        completed.remove(userId)
    }
}
