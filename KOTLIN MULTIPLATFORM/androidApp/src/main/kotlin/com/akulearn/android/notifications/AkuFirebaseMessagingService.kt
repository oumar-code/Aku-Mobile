package com.akulearn.android.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.akulearn.android.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * FCM message handler for the Aku app.
 *
 * **Message payload conventions**
 *
 * All FCM data messages should include:
 * | Key        | Value                                   |
 * |------------|-----------------------------------------|
 * | `title`    | Notification title                      |
 * | `body`     | Notification body text                  |
 * | `lessonId` | (optional) Navigates to lesson player   |
 * | `courseId` | (optional) Navigates to course detail   |
 *
 * If neither `lessonId` nor `courseId` is present the notification opens the
 * app at its default landing screen.
 *
 * **Token registration**
 *
 * [onNewToken] is called whenever the FCM registration token is created or
 * refreshed.  Send the token to your Supabase backend (e.g. via an Edge
 * Function) so the server can target this device.
 */
class AkuFirebaseMessagingService : FirebaseMessagingService() {

    // -------------------------------------------------------------------------
    // Incoming message
    // -------------------------------------------------------------------------

    override fun onMessageReceived(message: RemoteMessage) {
        // Prefer explicit data payload values; fall back to the FCM notification object
        // so that the service works with both "data-only" and "notification + data" messages.
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: "Aku"
        val body  = data["body"]  ?: message.notification?.body  ?: ""

        val lessonId = data["lessonId"]
        val courseId = data["courseId"]

        when {
            lessonId != null -> DeepLinkHandler.postNotification(
                this, title, body, DeepLinkHandler.lessonUri(lessonId)
            )
            courseId != null -> DeepLinkHandler.postNotification(
                this, title, body, DeepLinkHandler.courseUri(courseId)
            )
            else -> postGenericNotification(title, body)
        }
    }

    // -------------------------------------------------------------------------
    // Token refresh
    // -------------------------------------------------------------------------

    /**
     * Called when the FCM registration token is first generated or rotated.
     *
     * TODO: POST the [token] to the Supabase backend so the server can send
     *       targeted push notifications to this device.  Example:
     *
     * ```kotlin
     * // Inside a coroutine scope (e.g. via CoroutineScope(Dispatchers.IO)):
     * supabaseClient.functions.invoke(
     *     function = "register-device-token",
     *     body = buildJsonObject { put("fcm_token", token) }
     * )
     * ```
     */
    override fun onNewToken(token: String) {
        // Token is available here; forward it to your backend when ready.
    }

    // -------------------------------------------------------------------------
    // Generic (no deep-link) notification
    // -------------------------------------------------------------------------

    private fun postGenericNotification(title: String, body: String) {
        ensureChannelCreated()

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this)
            .notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun ensureChannelCreated() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Akulearn course and lesson updates"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID   = "akulearn_main"
        private const val CHANNEL_NAME = "Akulearn"
    }
}
