package com.akulearn.android.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.akuplatform.shared.notifications.NotificationService

/**
 * Android implementation of [NotificationService].
 *
 * On Android 13+ (API 33) the [POST_NOTIFICATIONS][Manifest.permission.POST_NOTIFICATIONS]
 * permission must be explicitly requested at runtime.  The actual system dialog is shown
 * by the [RequestNotificationPermission] composable; this class reports the resulting
 * grant state so that shared-layer code can react to it.
 *
 * On older Android versions notifications are implicitly granted, so both methods
 * return `true` without any OS interaction.
 */
class AndroidNotificationService(private val context: Context) : NotificationService {

    override suspend fun isPermissionGranted(): Boolean = checkGranted()

    /** On Android the system dialog is driven by the Compose composable; this method
     *  simply returns the current grant state after the composable has done its work. */
    override suspend fun requestPermission(): Boolean = checkGranted()

    private fun checkGranted(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
}
