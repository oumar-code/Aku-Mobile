package com.akuplatform.shared.course

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [FakeLessonProgressStorage] behaviour, which mirrors the contract that
 * every [com.akuplatform.shared.course.progress.LessonProgressStorage] implementation must satisfy.
 *
 * These tests also serve as a specification for [AndroidLessonProgressStorage] so that
 * when a concrete Android implementation is tested (e.g. via Robolectric or instrumentation)
 * the same assertions apply.
 */
class LessonProgressTest {

    private val storage = FakeLessonProgressStorage()
    private val userId = "user-123"

    @Test
    fun `newly created storage returns empty set`() = runTest {
        assertTrue(storage.getCompletedLessonIds(userId).isEmpty())
    }

    @Test
    fun `markComplete adds lesson to completed set`() = runTest {
        storage.markComplete(userId, "lesson-1")
        assertTrue(storage.getCompletedLessonIds(userId).contains("lesson-1"))
    }

    @Test
    fun `markComplete is idempotent`() = runTest {
        storage.markComplete(userId, "lesson-1")
        storage.markComplete(userId, "lesson-1")
        assertEquals(1, storage.getCompletedLessonIds(userId).size)
    }

    @Test
    fun `multiple lessons can be marked complete independently`() = runTest {
        storage.markComplete(userId, "lesson-1")
        storage.markComplete(userId, "lesson-2")
        storage.markComplete(userId, "lesson-3")
        val ids = storage.getCompletedLessonIds(userId)
        assertEquals(3, ids.size)
        assertTrue(ids.containsAll(listOf("lesson-1", "lesson-2", "lesson-3")))
    }

    @Test
    fun `isCompleted returns true after markComplete`() = runTest {
        storage.markComplete(userId, "lesson-42")
        assertTrue(storage.isCompleted(userId, "lesson-42"))
    }

    @Test
    fun `isCompleted returns false for unknown lesson`() = runTest {
        assertFalse(storage.isCompleted(userId, "lesson-not-there"))
    }

    @Test
    fun `clearAll removes all records for the user`() = runTest {
        storage.markComplete(userId, "lesson-1")
        storage.markComplete(userId, "lesson-2")
        storage.clearAll(userId)
        assertTrue(storage.getCompletedLessonIds(userId).isEmpty())
    }

    @Test
    fun `clearAll does not affect other users`() = runTest {
        val otherUser = "user-456"
        storage.markComplete(userId, "lesson-1")
        storage.markComplete(otherUser, "lesson-2")

        storage.clearAll(userId)

        assertTrue(storage.getCompletedLessonIds(userId).isEmpty())
        assertTrue(storage.getCompletedLessonIds(otherUser).contains("lesson-2"))
    }

    @Test
    fun `different users have independent progress sets`() = runTest {
        val userA = "user-A"
        val userB = "user-B"
        storage.markComplete(userA, "lesson-1")
        storage.markComplete(userB, "lesson-99")

        assertTrue(storage.isCompleted(userA, "lesson-1"))
        assertFalse(storage.isCompleted(userA, "lesson-99"))
        assertFalse(storage.isCompleted(userB, "lesson-1"))
        assertTrue(storage.isCompleted(userB, "lesson-99"))
    }
}
