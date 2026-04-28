package com.akulearn.android.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Utility for building and posting Android push notifications that carry deep-link
 * targets into the app.
 *
 * Deep-link URI scheme:  `akulearn://`
 *
 * Supported paths:
 * - `akulearn://lesson/{lessonId}`  → opens [LessonPlayerScreen] for the given lesson
 * - `akulearn://course/{courseId}`  → opens [CourseDetailScreen] for the given course
 *
 * Notification payloads arriving over FCM or any other channel can call
 * [DeepLinkHandler.postNotification] to raise a tappable system notification that
 * navigates the user directly to the targeted content.
 */
object DeepLinkHandler {

    const val SCHEME = "akulearn"
    const val PATH_LESSON = "lesson"
    const val PATH_COURSE = "course"

    /** URI for navigating to a specific lesson. */
    fun lessonUri(lessonId: String): Uri =
        Uri.Builder().scheme(SCHEME).authority(PATH_LESSON).appendPath(lessonId).build()

    /** URI for navigating to a specific course. */
    fun courseUri(courseId: String): Uri =
        Uri.Builder().scheme(SCHEME).authority(PATH_COURSE).appendPath(courseId).build()

    /**
     * Posts a system notification that deep-links into the app when tapped.
     *
     * @param context    Android context.
     * @param title      Notification title.
     * @param body       Notification body text.
     * @param deepLinkUri Target URI; construct with [lessonUri] or [courseUri].
     * @param notificationId Unique ID for the notification (allows future updates/cancellation).
     */
    fun postNotification(
        context: Context,
        title: String,
        body: String,
        deepLinkUri: Uri,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        ensureChannelCreated(context)

        val contentIntent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
            // Ensure a running instance of MainActivity handles the deep link rather than
            // launching a second activity stack.
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun ensureChannelCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Akulearn course and lesson updates"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private const val CHANNEL_ID = "akulearn_main"
    private const val CHANNEL_NAME = "Akulearn"
}
