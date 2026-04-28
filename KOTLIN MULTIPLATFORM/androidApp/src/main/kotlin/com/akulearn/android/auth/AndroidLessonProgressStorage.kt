package com.akulearn.android.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.akuplatform.shared.course.progress.LessonProgressStorage

/**
 * Android implementation of [LessonProgressStorage].
 *
 * Lesson completion records are stored as a comma-separated set of lesson IDs inside
 * [EncryptedSharedPreferences] (AES-256-GCM values, AES256-SIV keys) so the data is
 * protected by the Android Keystore — the same mechanism used for auth tokens.
 *
 * The SharedPreferences file is named `aku_progress_{userId}` to keep different users'
 * data isolated on shared devices.
 */
class AndroidLessonProgressStorage(private val context: Context) : LessonProgressStorage {

    private fun prefs(userId: String) = EncryptedSharedPreferences.create(
        context,
        // Hash the userId so that path-traversal characters and very long IDs are not an issue.
        "aku_progress_${userId.hashCode().toUInt()}",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override suspend fun markComplete(userId: String, lessonId: String) {
        val p = prefs(userId)
        val existing = p.getString(KEY_COMPLETED, "") ?: ""
        val ids = existing.split(SEPARATOR).filter { it.isNotBlank() }.toMutableSet()
        if (ids.add(lessonId)) {
            p.edit().putString(KEY_COMPLETED, ids.joinToString(SEPARATOR)).apply()
        }
    }

    override suspend fun getCompletedLessonIds(userId: String): Set<String> {
        val raw = prefs(userId).getString(KEY_COMPLETED, "") ?: ""
        return raw.split(SEPARATOR).filter { it.isNotBlank() }.toSet()
    }

    override suspend fun clearAll(userId: String) {
        prefs(userId).edit().remove(KEY_COMPLETED).apply()
    }

    companion object {
        private const val KEY_COMPLETED = "completed_lesson_ids"
        private const val SEPARATOR = ","
    }
}
