package com.akulearn.android.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Requests the [Manifest.permission.POST_NOTIFICATIONS] permission on Android 13+
 * (API level 33 and above) when this composable enters the composition.
 *
 * On older OS versions the call is a no-op because the permission does not exist
 * and notifications are implicitly granted.
 *
 * Usage: call [RequestNotificationPermission] once from a screen that is shown
 * early in the app lifecycle (e.g. the splash / home screen).
 */
@Composable
fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* Permission result handled silently; the app degrades gracefully without it. */ }

        LaunchedEffect(Unit) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
